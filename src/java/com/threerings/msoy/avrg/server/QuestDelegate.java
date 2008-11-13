//
// $Id$

package com.threerings.msoy.avrg.server;

import static com.threerings.msoy.Log.log;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManagerDelegate;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.server.persist.AVRGameRepository;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.GameContent;
import com.threerings.msoy.game.server.GameGameRegistry;
import com.threerings.msoy.game.server.WorldServerClient;

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

    /**
     * Flushes the pending coin earnings for the specified player (if they are not a player of this
     * game or have no pending coin earnings, this method does nothing).
     */
    public void flushCoinEarnings (final int playerId)
    {
        // flow record is indexed by oid not playerId so we have to search
        Player player = Iterables.find(_players.values(), new Predicate<Player>() {
            public boolean apply (Player player) {
                return player.playerObject.getMemberId() == playerId;
            }
        });
        if (player != null) {
            payoutPlayer(player, true);
        }
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

        // if any players remain unpaid out, pay them out now (this will also note any accumulated
        // playtime for these players that was not associated with a payout)
        flushAllPlayers();
        _players.clear();
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
        _eventLog.avrgEntered(player.getMemberId(), player.getVisitorId());
    }

    @Override // from PlaceManagerDelegate
    public void bodyLeft (final int bodyOid)
    {
        final Player player = _players.remove(bodyOid);
        if (player == null) {
            log.warning("Can't payout untracked player", "gameId", _gameId, "bodyOid", bodyOid);
            return;
        }
        payoutPlayer(player, false);
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
        int flowPerHour = _runtime.money.hourlyGameFlowRate;
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

        // we don't actually award coins if we're the development version of the game
        final boolean actuallyAward = !_content.game.isDevelopmentVersion();

        // note that they completed this task
        if (actuallyAward) {
            player.taskCompleted(payout);

            // accumulate the flow for this quest (to be persisted later)
            if (payout > 0) {
                _worldClient.reportCoinAward(plobj.getMemberId(), payout);
            }
        }

        // report task completion to the game
        plobj.postMessage(AVRGameObject.TASK_COMPLETED_MESSAGE, questId, payout, actuallyAward);

        // tell the requester that we're groovy
        listener.requestProcessed();

        // finally, if we have pending coin awards that exceed our current coin budget, flush 'em
        // and trigger a recaculation of our payout rate
        if (actuallyAward) {
            int pendingCoins = 0;
            for (Player p : _players.values()) {
                pendingCoins += p.coinsAccrued;
            }
            if (pendingCoins >= _content.detail.flowToNextRecalc) {
                flushAllPlayers();
            }
        }
    }

    /**
     * Convenience method to calculate the current timestmap in seconds.
     */
    protected static int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * Pays out all players any accrued coins and records all player minutes and coin awards to
     * this game's activity log, potentially recalculating its payout factor as a result.
     */
    protected void flushAllPlayers ()
    {
        int totalSecs = 0, totalTasks = 0, totalAward = 0;

        // pay out flow to all players, total up our coin awards and play time
        for (Player player : _players.values()) {
            final int playerSecs = player.timeAccrued + player.getPlayTime(now());
            totalSecs += playerSecs;
            totalTasks += player.tasksCompleted;
            if (player.coinsAccrued > 0) {
                final UserAction action = UserAction.playedGame(
                    player.playerObject.getMemberId(), _content.game.name, _gameId, playerSecs);
                _worldClient.awardCoins(_gameId, action, player.coinsAccrued);
                totalAward += player.coinsAccrued;
                player.reset();
            }
        }

        // if we actually awarded coins or accrued time, update our game metrics
        if (totalAward > 0 || totalSecs > 0) {
            final int totalMins = Math.round(totalSecs / 60f);
            _gameReg.updateGameMetrics(_content.detail, true, totalMins, totalTasks, totalAward);
        }
    }

    /**
     * Pays accumulated coins to the supplied player and potentially recalculates our payout factor
     * if this player's payout consumes the last of our coin budget.
     */
    protected void payoutPlayer (final Player player, boolean flushing)
    {
        final int memberId = player.playerObject.getMemberId();
        final int playerSecs = player.timeAccrued + player.getPlayTime(now());
        final int playerMins = Math.round(playerSecs / 60f);

        if (!flushing) {
            // TODO: Get this into EventLogDelegate, or write a AVRG-specific one?
            if (player.playerObject.visitorInfo == null) {
                log.warning("No VisitorInfo for AVRG player!", "gameId", _gameId,
                            "memberId", memberId);
            }
            _eventLog.avrgLeft(memberId, _gameId, playerSecs,
                               _plmgr.getPlaceObject().occupantInfo.size(),
                               player.playerObject.getVisitorId());

            // let the world know that this player is no longer playing our game
            _worldClient.updatePlayer(memberId, null);
        }

        // if they accrued any coins, pay them out
        if (player.coinsAccrued > 0) {
            // do the actual coin awarding
            final UserAction action = UserAction.playedGame(
                memberId, _content.game.name, _gameId, playerSecs);
            _worldClient.awardCoins(_gameId, action, player.coinsAccrued);
        }

        // note time played and coins awarded for coin payout factor calculation purposes
        if (playerMins > 0 || player.coinsAccrued > 0) {
            _gameReg.updateGameMetrics(
                _content.detail, true, playerMins, player.tasksCompleted, player.coinsAccrued);
        }

        // reset their accumulated coins and whatnot
        player.reset();
    }

    protected class Player
    {
        public PlayerObject playerObject;

        public int coinsAccrued;
        public int tasksCompleted;
        public int timeAccrued;

        public Player (final PlayerObject playerObject) {
            this.playerObject = playerObject;
            reset();
        }

        public int getPlayTime (int now) {
            return now - _beganStamp;
        }

        public void taskCompleted (int payout) {
            coinsAccrued += payout;
            tasksCompleted++;
            int now = now();
            timeAccrued += getPlayTime(now);
            _beganStamp = now;
        }

        public void reset () {
            coinsAccrued = 0;
            tasksCompleted = 0;
            timeAccrued = 0;
            _beganStamp = now();
        }

        protected int _beganStamp;
    }

    /** The gameId of this particular AVRG. */
    protected int _gameId;

    /** The metadata for the game for which we're lobbying. */
    protected GameContent _content;

    /** A map to track current AVRG player data, per PlayerObject Oid. */
    protected IntMap<Player> _players = new HashIntMap<Player>();

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected GameGameRegistry _gameReg;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected AVRGameRepository _repo;
}
