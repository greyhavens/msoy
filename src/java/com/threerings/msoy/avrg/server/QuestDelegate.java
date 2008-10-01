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

        // if any players remain unpaid out, pay them out now
        for (Player player : _players.values()) {
            payoutPlayer(player, false);
        }
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
        _eventLog.avrgEntered(player.getMemberId(), player.visitorInfo.id);
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

        // note that they completed this task
        player.taskCompleted(payout);

        // accumulate the flow for this quest (to be persisted later)
        if (payout > 0) {
            _worldClient.reportCoinAward(plobj.getMemberId(), payout);
        }

        // report task completion to the game
        plobj.postMessage(AVRGameObject.TASK_COMPLETED_MESSAGE, questId, payout);

        // tell the requester that we're groovy
        listener.requestProcessed();
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
            final String tracker = (player.playerObject.visitorInfo == null) ?
                "" : player.playerObject.visitorInfo.id;
            _eventLog.avrgLeft(memberId, _gameId, playerSecs,
                               _plmgr.getPlaceObject().occupantInfo.size(), tracker);

            // let the world know that this player is no longer playing our game
            _worldClient.updatePlayer(memberId, null);
        }

        // if we're a guest, we don't actually award and coins, so we also don't record these
        // minutes or coin awards to our coin payout recalc metrics
        if (MemberName.isGuest(memberId)) {
            return;
        }

        // if they accrued any coins, pay them out
        if (player.coinsAccrued > 0) {
            // do the actual coin awarding
            UserAction action = UserAction.playedGame(
                memberId, _content.game.name, _gameId, playerSecs);
            _worldClient.awardCoins(_gameId, action, player.coinsAccrued);

            // note games played and coins awarded for coin payout factor calculation purposes
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
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected GameGameRegistry _gameReg;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected AVRGameRepository _repo;
}
