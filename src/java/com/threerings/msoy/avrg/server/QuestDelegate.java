//
// $Id$

package com.threerings.msoy.avrg.server;

import static com.threerings.msoy.Log.log;

import com.google.inject.Inject;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.WriteOnlyUnit;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManagerDelegate;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.server.persist.AVRGameRepository;
import com.threerings.msoy.avrg.server.persist.QuestLogSummaryRecord;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.GameContent;
import com.threerings.msoy.game.server.WorldServerClient;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.server.MsoyEventLogger;

/**
 * Handles the completeTask service call, including awarding coins.
 * TODO: This may or may not be included in a refactor involving AwardDelegate
 */
public class QuestDelegate extends PlaceManagerDelegate
{
    /**
     * Creates a Whirled game manager delegate with the supplied game content.
     */
    public QuestDelegate (final GameContent content)
    {
        // keep our game content around for later
        _content = content;
        _gameId = _content.game.gameId;
    }

    @Override
    public void didStartup (final PlaceObject plobj)
    {
        super.didStartup(plobj);
    }

    @Override
    public void didShutdown ()
    {
        super.didShutdown();

        // if any players remain unpaid out, pay them out now
        for (int bodyOid : _players.keySet().toArray(new Integer[_players.size()])) {
            payoutPlayer(bodyOid);
        }

        // record any minutes that were played that did not get noted via the completion of tasks
        final int unawardedMins = Math.max(1, Math.round(_totalUnawardedSeconds / 60f));
        _invoker.postUnit(new WriteOnlyUnit("shutdown") {
            @Override public void invokePersist () throws Exception {
                _repo.noteUnawardedTime(_gameId, unawardedMins);
            }
        });
    }

    @Override // from PlaceManagerDelegate
    public void bodyEntered (final int bodyOid)
    {
        final PlayerObject player = (PlayerObject) _omgr.getObject(bodyOid);
        if (_players.containsKey(bodyOid)) {
            log.warning("Attempting to re-add existing player [gameId=" + _gameId + ", playerId=" +
                        player.getMemberId() + "]");
            return;
        }
        _players.put(bodyOid, new Player(player));
        _worldClient.updatePlayer(player.getMemberId(), _content.game);
        _eventLog.avrgEntered(player.getMemberId(), player.visitorInfo.id);
    }

    @Override // from PlaceManagerDelegate
    public void bodyLeft (final int bodyOid)
    {
        payoutPlayer(bodyOid);
    }

    /**
     * Notes that the supplied player completed the specified task. Computes a coin payout for the
     * player, transiently reports it to them and accumulates it to their stat record for actual
     * payout when they leave the game.
     */
    public void completeTask (
        final PlayerObject plobj, final String questId, final float payoutLevel,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // sanity check
        if (payoutLevel < 0 || payoutLevel > 1) {
            log.warning("Invalid payout", "game", where(), "caller", plobj.who(),
                        "quest", questId, "payout", payoutLevel);
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        Player player = _players.get(plobj.getOid());
        if (player == null) {
            log.warning("Asked to complete task for untracked player?", "oid", plobj.getOid(),
                        "memberId", plobj.getMemberId());
            return;
        }

        // payout factor depends on accumulated play time -- if we've yet to accumulate enough data
        // for a calculation, guesstimate 5 mins
        int flowPerHour = RuntimeConfig.server.hourlyGameFlowRate;
        int payoutFactor = (_content.detail.payoutFactor == 0) ?
            ((5 * flowPerHour) / 60) : _content.detail.payoutFactor;

        // compute our quest payout; as a sanity check, cap it at one hour of payout
        final int rawPayout = Math.round(payoutFactor * payoutLevel);
        final int payout = Math.min(flowPerHour, rawPayout);
        if (payout != rawPayout) {
            log.warning("Capped AVRG payout at one hour [game=" + _gameId +
                        ", factor=" + payoutFactor + ", level=" + payoutLevel +
                        ", wanted=" + rawPayout + ", got=" + payout + "].");
        }

        // if this user is a guest, just report that it was completed and avoid any persistent
        // business (all the persisting will hopefully soon go away anyway)
        if (MemberName.isGuest(plobj.getMemberId())) {
            listener.requestProcessed();
            return;
        }

        // accumulate the flow for this quest (to be persisted later)
        final int playerSecs = player.taskCompleted(payout);
        final int playerMins = Math.round(playerSecs / 60f);
        if (payout > 0) {
            _worldClient.reportCoinAward(plobj.getMemberId(), payout);
        }

        // TODO: this is only used to recompute AVRG flow payout AFAIK, so it should go away and we
        // should use the standard system used by normal games for this purpose
        _invoker.postUnit(new PersistingUnit("completeTask", listener) {
            @Override public void invokePersistent () throws Exception {
                // mark the quest completed and create a log record
                _repo.noteQuestCompleted(
                    _gameId, plobj.getMemberId(), questId, playerMins, payoutLevel);
            }
            @Override public void handleSuccess () {
                // report task completion to the game
                plobj.postMessage(AVRGameObject.TASK_COMPLETED_MESSAGE, questId, payout);
                reportRequestProcessed();
            }
        });
    }

    /**
     * Convenience method to calculate the current timestmap in seconds.
     */
    protected static int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * Pays accumulated coins to the supplied player and potentially recalculates our payout factor
     * if this player's payout consumes the last of our coin budget.
     */
    protected void payoutPlayer (final int bodyOid)
    {
        final Player player = _players.remove(bodyOid);
        if (player == null) {
            log.warning("Can't payout untracked player", "gameId", _gameId, "bodyOid", bodyOid);
            return;
        }
        final int memberId = player.playerObject.getMemberId();
        final int unawardedTime = player.getPlayTime(now());
        final int totalPlayTime = player.timeAccrued + unawardedTime;

        // note this players unawarded time so that we can record it when we shutdown
        _totalUnawardedSeconds += unawardedTime;

        // TODO: Get this into EventLogDelegate, or write a AVRG-specific one?
        if (player.playerObject.visitorInfo == null) {
            log.warning("No VisitorInfo for AVRG player!", "gameId", _gameId, "memberId", memberId);
        }
        final String tracker = (player.playerObject.visitorInfo == null) ?
            "" : player.playerObject.visitorInfo.id;
        _eventLog.avrgLeft(memberId, _gameId, totalPlayTime,
                           _plmgr.getPlaceObject().occupantInfo.size(), tracker);

        // do the actual coin awarding
        UserAction action = UserAction.playedGame(
            memberId, _content.game.name, _gameId, totalPlayTime);
        _worldClient.awardCoins(_gameId, action, player.coinsAccrued);
        _worldClient.updatePlayer(memberId, null);

        // note that we've used up some of our flow budget (this same adjustment will be recorded
        // to the database in noteGamePlayed())
        _content.detail.flowToNextRecalc -= player.coinsAccrued;

        final int flowPerHour = RuntimeConfig.server.hourlyGameFlowRate;
        final int recalcMins = RuntimeConfig.server.payoutFactorReassessment;
        final int newFlowToNextRecalc;
        // if this payout consumed the remainder of our awardable flow, queue a factor recalc
        if (_content.detail.flowToNextRecalc <= 0) {
            newFlowToNextRecalc = flowPerHour * recalcMins + _content.detail.flowToNextRecalc;
            // update our in memory record immediately so that we don't encounter funny
            // business if another quest payout is done while we're writing this to the
            // database
            _content.detail.flowToNextRecalc = newFlowToNextRecalc;
        } else {
            newFlowToNextRecalc = 0;
        }

        // note that games were played, coins were paid, and possibly do a payout factor recalc
        _invoker.postUnit(new RepositoryUnit("bodyLeft") {
            @Override public void invokePersist () throws Exception {
                // note that we played some number of "games" and awarded the specified flow
                _mgameRepo.noteGamePlayed(_gameId, player.tasksCompleted, player.coinsAccrued);

//                 // mark the quest completed and create a log record
//                 if (!MemberName.isGuest(player.getMemberId())) {
//                     _repo.noteQuestCompleted(
//                         _gameId, player.getMemberId(), questId, playerMins, payoutLevel);
//                 }

                // if this award consumes the remainder of our awardable flow, recalc our bits
                if (newFlowToNextRecalc > 0) {
                    final QuestLogSummaryRecord record = _repo.summarizeQuestLogRecords(_gameId);
                    if (record.payoutFactorTotal > 0) {
                        final float targetFlow = flowPerHour * record.playerMinsTotal / 60f;
                        _newFactor = Math.round(targetFlow / record.payoutFactorTotal);

                        _mgameRepo.updatePayoutFactor(_gameId, _newFactor, newFlowToNextRecalc);
                        _repo.deleteQuestLogRecords(_gameId);

                        log.info("Recalculation complete [factor=(" + flowPerHour + "*" +
                                 record.playerMinsTotal + ")/60f/" +
                                 record.payoutFactorTotal + " => " + _newFactor + "]");
                    }
                }
            }

            @Override public void handleSuccess () {
                // if we updated the payout factor in the db, do it in dobj land too
                if (newFlowToNextRecalc > 0) {
                    _content.detail.payoutFactor = _newFactor;
                }
            }

            protected int _newFactor;
        });
    }

    protected class Player
    {
        public PlayerObject playerObject;

        public int coinsAccrued;
        public int tasksCompleted;
        public int timeAccrued;

        public Player (final PlayerObject playerObject) {
            this.playerObject = playerObject;
            _beganStamp = now();
        }

        public int getPlayTime (int now) {
            int secondsOfPlay = 0;
            if (_beganStamp != 0) {
                secondsOfPlay += (now - _beganStamp);
            }
            return secondsOfPlay;
        }

        public int taskCompleted (int payout) {
            coinsAccrued += payout;
            tasksCompleted++;
            int now = now(), taskTime = getPlayTime(now);
            timeAccrued += taskTime;
            _beganStamp = now;
            return taskTime;
        }

        protected int _beganStamp;
    }

    /** The gameId of this particular AVRG. */
    protected int _gameId;

    /** The metadata for the game for which we're lobbying. */
    protected GameContent _content;

    /** A map to track current AVRG player data, per PlayerObject Oid. */
    protected IntMap<Player> _players = new HashIntMap<Player>();

    /** Records the number of seconds played by players that did not end up getting noted when they
     * completed a task. */
    protected int _totalUnawardedSeconds = 0;

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected AVRGameRepository _repo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected MsoyEventLogger _eventLog;
}
