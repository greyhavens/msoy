//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.data.GameCodes;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.rating.server.RatingManagerDelegate;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.ezgame.server.EZGameManager;

import com.whirled.data.WhirledGame;
import com.whirled.data.WhirledGameMarshaller;
import com.whirled.server.WhirledGameDispatcher;
import com.whirled.server.WhirledGameProvider;

import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyBaseServer;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.game.server.MsoyGameServer;

import static com.threerings.msoy.Log.log;

/**
 * Handles Whirled game services like awarding flow.
 */
public class WhirledGameDelegate extends RatingManagerDelegate
    implements WhirledGameProvider
{
    public WhirledGameDelegate (GameManager gmgr)
    {
        super(gmgr);
    }

    // from interface WhirledGameProvider
    public void endGameWithScores (ClientObject caller, int[] playerOids, int[] scores,
                                   int payoutType, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayer(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }

        // convert the players into record indexed on player oid which will weed out duplicates and
        // avoid funny business
        HashIntMap<Player> players = new HashIntMap<Player>();
        for (int ii = 0; ii < playerOids.length; ii++) {
            int availFlow = getAwardableFlow(playerOids[ii]);
            players.put(playerOids[ii], new Player(playerOids[ii], scores[ii], availFlow));
        }

        // TODO: record scores, convert scores to percentiles
        for (Player player : players.values()) {
            player.percentile = 69; // TEMP
            // scale each players' flow award by their percentile performance
            player.availFlow = (int)Math.ceil(player.availFlow * (player.percentile / 99f));
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // TODO: update ratings

        // now actually end the game
        _gmgr.endGame();
    }

    // from interface WhirledGameProvider
    public void endGameWithWinners (ClientObject caller, int[] winnerOids, int[] loserOids,
                                    int payoutType, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayer(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }

        // convert the players into records indexed on player oid to weed out duplicates and avoid
        // any funny business
        HashIntMap<Player> players = new HashIntMap<Player>();
        for (int ii = 0; ii < winnerOids.length; ii++) {
            Player player = new Player(winnerOids[ii], 1, getAwardableFlow(winnerOids[ii]));
            player.percentile = 74; // winners are 75th percentile
            players.put(winnerOids[ii], player);
        }
        for (int ii = 0; ii < loserOids.length; ii++) {
            Player player = new Player(loserOids[ii], 0, getAwardableFlow(loserOids[ii]));
            player.percentile = 24; // losers are 25th percentile
            players.put(loserOids[ii], player);
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // tell the game manager about our winners which will be used to compute ratings, etc.
        if (_gmgr instanceof EZGameManager) {
            ArrayIntSet winners = new ArrayIntSet();
            for (Player player : players.values()) {
                if (player.score == 1) {
                    winners.add(player.playerOid);
                }
            }
            ((EZGameManager)_gmgr).setWinners(winners.toIntArray());

        } else {
            log.warning("Unable to configure EZGameManager with winners [where=" + where() +
                        ", isa=" + _gmgr.getClass().getName() + "].");
        }

        // now actually end the game
        _gmgr.endGame();
    }

    protected void awardFlow (HashIntMap<Player> players, int payoutType)
    {
        // figure out who ranked where
        TreeMap<Integer,ArrayList<Player>> rankings = new TreeMap<Integer,ArrayList<Player>>();
        for (Player player : players.values()) {
            ArrayList<Player> list = rankings.get(player.score);
            if (list == null) {
                list = new ArrayList<Player>();
            }
            list.add(player);
        }

        switch (payoutType) {
        case WINNERS_TAKE_ALL: // TODO
//            break;

        case CASCADING_PAYOUT: // TODO
//            break;

        case TO_EACH_THEIR_OWN:
            for (Player player : players.values()) {
                player.flowAward = player.availFlow;
            }
            break;
        }

        log.info("Awarding flow [game=" + where() + ", to=" + players + "].");

        // actually award flow and report it to the player
        int now = now();
        for (Player player : players.values()) {
            FlowRecord record = _flowRecords.get(player.playerOid);
            if (record == null) {
                continue;
            }

            // accumulate their awarded flow into their flow record; we'll pay it all out in one
            // database action when they leave the room or the game is shutdown
            record.awarded += player.flowAward;

            // update the player's member object on their world server
            if (player.flowAward > 0) {
                reportFlowAward(record.memberId, player.flowAward);
            }

            // report to the game that this player earned some flow
            DObject user = MsoyBaseServer.omgr.getObject(player.playerOid);
            if (user != null) {
                user.postMessage(WhirledGame.FLOW_AWARDED_MESSAGE,
                                 player.flowAward, player.percentile);
            }
        }
    }

    @Override
    public void didStartup (PlaceObject plobj)
    {
        super.didStartup(plobj);

        // wire up our WhirledGameService
        if (plobj instanceof WhirledGame) {
            _invmarsh = MsoyBaseServer.invmgr.registerDispatcher(new WhirledGameDispatcher(this));
            ((WhirledGame)plobj).setWhirledGameService((WhirledGameMarshaller)_invmarsh);
        }

        // then load up our anti-abuse factor
        final int gameId = getGameId();
        MsoyBaseServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor =
                        MsoyBaseServer.memberRepo.getFlowRepository().getAntiAbuseFactor(gameId);

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to fetch game's anti-abuse factor [where=" +
                            where() + "]", pe);
                    // if for some reason our anti-abuse mechanism is on the blink, assume the
                    // game is innocent until proven guilty
                    _antiAbuseFactor = 1.0f;
                }
                return true; // = call handleResult()
            }

            // here, we're back on the dobj thread
            public void handleResult () {
                int hourlyRate = RuntimeConfig.server.hourlyGameFlowRate;
                _flowPerMinute = (int)((hourlyRate * _antiAbuseFactor) / 60d);
            }

            protected double _antiAbuseFactor;
        });
    }

    @Override
    public void didShutdown ()
    {
        super.didShutdown();
        if (_invmarsh != null) {
            MsoyBaseServer.invmgr.clearDispatcher(_invmarsh);
        }

        stopTracking();

        // pay out to all the players who have not yet been paid
        int[] oids = _flowRecords.intKeySet().toIntArray();
        for (int oid : oids) {
            payoutPlayer(oid);
        }

        // put the kibosh on further flow tracking
        _flowPerMinute = -1;
    }

    @Override
    public void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // potentially create a flow record for this occupant
        if (!_flowRecords.containsKey(bodyOid)) {
            MsoyUserObject uobj = (MsoyUserObject) MsoyBaseServer.omgr.getObject(bodyOid);
            if (uobj == null) {
                log.warning("Failed to lookup member [oid=" + bodyOid + "]");

            } else {
                FlowRecord record = new FlowRecord(uobj.getMemberId(), uobj.getHumanity());
                _flowRecords.put(bodyOid, record);
                // if we're currently tracking, note that they're "starting" immediately
                if (_tracking) {
                    record.beganStamp = now();
                }
            }
        }
    }

    @Override
    public void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // when a player leaves the game, pay out their earned flow
        payoutPlayer(bodyOid);
    }

    @Override
    public void gameDidStart ()
    {
        super.gameDidStart();

        // note that all occupants are accumulating "game time"
        startTracking();
    }

    @Override
    public void gameDidEnd ()
    {
        super.gameDidEnd();

        // stop accumulating "game time" for players
        stopTracking();

        int totalSeconds = _totalTrackedSeconds;
        int now = _tracking ? now() : 0;
        for (FlowRecord record : _flowRecords.values()) {
            totalSeconds += record.secondsPlayed;
            if (_tracking && record.beganStamp != 0) {
                totalSeconds += (now - record.beganStamp);
            }
        }
        int totalMinutes = Math.round(totalSeconds / 60f);
        if (totalMinutes == 0 && totalSeconds > 0) {
            totalMinutes = 1; // round very short games up to 1 minute.
        }

        if (totalMinutes > 0) {
            final int playerMins = totalMinutes;
            final int gameId = getGameId();
            MsoyBaseServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        MsoyBaseServer.memberRepo.noteGameEnded(gameId, playerMins);
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING,
                            "Failed to note end of game [where=" + where() + "]", pe);
                    }
                    return false;
                }
            });
        }
    }

    @Override
    protected int minimumRatedDuration ()
    {
        return 10; // don't rate games that last less than 10 seconds
    }

    @Override
    protected RatingRepository getRatingRepository ()
    {
        return MsoyBaseServer.ratingRepo;
    }

    @Override
    protected void updateRatingInMemory (int gameId, Name playerName, Rating rating)
    {
        // we don't keep in-memory ratings for whirled
    }

    /**
     * Convenience method to get our game Id.
     */
    protected int getGameId ()
    {
        return ((GameConfig) _plmgr.getConfig()).getGameId();
    }

    protected void startTracking ()
    {
        if (_tracking) {
            return;
        }
        _tracking = true;

        // note the time at which we started for flow calculations
        int startStamp = now();
        for (FlowRecord record : _flowRecords.values()) {
            record.beganStamp = startStamp;
        }
    }

    protected void stopTracking ()
    {
        if (!_tracking) {
            return;
        }
        _tracking = false;

        // note all remaining player's seconds played
        int endStamp = now();
        for (FlowRecord record : _flowRecords.values()) {
            record.stopTracking(endStamp);
        }
    }

    /**
     * Return the total number of seconds that players were being tracked.
     */
    protected int getTotalTrackedSeconds ()
    {
        int total = _totalTrackedSeconds, now = _tracking ? now() : 0;
        for (FlowRecord record : _flowRecords.values()) {
            total += record.secondsPlayed;
            if (_tracking && record.beganStamp != 0) {
                total += (now - record.beganStamp);
            }
        }
        return total;
    }

    protected int getAwardableFlow (int playerOid)
    {
        FlowRecord record = _flowRecords.get(playerOid);
        return (record == null) ? 0 : record.getAwardableFlow(_flowPerMinute, now());
    }

    protected void payoutPlayer (int oid)
    {
        // remove their flow record and grant them the flow
        final FlowRecord record = _flowRecords.remove(oid);
        if (record == null) {
            log.warning("No flow record found [oid=" + oid + "]");
            return;
        }

        // if they're leaving in the middle of things, update their secondsPlayed, just so that
        // it's correct for calculations below
        if (_tracking) {
            record.stopTracking(now());
        }

        // since we're dropping this record, we need to record the seconds played
        _totalTrackedSeconds += record.secondsPlayed;

        // see if we even care
        if (record.awarded == 0 || record.memberId == MemberName.GUEST_ID) {
            return;
        }

        // sanity check that we're initialized
        if (_flowPerMinute == -1) {
            log.warning("Unknown flow rate, but there's a grant. Wha?");
            return;
        }

        // see how much they actually get (also uses their secondsPlayed)
        final String details = getGameId() + " " + record.secondsPlayed;

        // actually grant their flow award; we don't need to update their in-memory flow value
        // because we've been doing that all along
        MsoyBaseServer.invoker.postUnit(new Invoker.Unit("grantFlow") {
            public boolean invoke () {
                try {
                    MsoyBaseServer.memberRepo.getFlowRepository().grantFlow(
                        record.memberId, record.awarded, UserAction.PLAYED_GAME, details);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to grant flow [mid=" + record.memberId +
                            ", amount=" + record.awarded + ", details=" + details + "].", pe);
                }
                return false;
            }
        });
    }

    /**
     * Checks that the caller in question is a player if the game is not a party game.
     */
    protected void verifyIsPlayer (ClientObject caller)
        throws InvocationException
    {
        MsoyUserObject user = (MsoyUserObject)caller;
        if (_gobj.players.length > 0) {
            if (_gobj.getPlayerIndex(user.getMemberName()) == -1) {
                throw new InvocationException(GameCodes.E_ACCESS_DENIED);
            }
        }
    }

    protected void reportFlowAward (int memberId, int deltaFlow)
    {
        if (MsoyServer.isActive()) {
            MsoyServer.gameReg.reportFlowAward(null, memberId, deltaFlow);
        } else {
            MsoyGameServer.worldClient.reportFlowAward(memberId, deltaFlow);
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
     * A record of flow awarded.
     */
    protected static class FlowRecord
    {
        public double humanity;
        public int memberId;

        public int beganStamp;
        public int secondsPlayed;

        public int awarded;

        public FlowRecord (int memberId, double humanity) {
            this.humanity = humanity;
            this.memberId = memberId;
            this.awarded = 0;
        }

        public int getAwardableFlow (int flowPerMinute, int now) {
            int secondsOfPlay = secondsPlayed;
            if (beganStamp != 0) {
                secondsOfPlay += (now - beganStamp);
            }
            return (int) ((humanity * flowPerMinute * secondsOfPlay) / 60);
        }

        public void stopTracking (int endStamp) {
            if (beganStamp != 0) {
                secondsPlayed += endStamp - beganStamp;
                beganStamp = 0;
            }
        }
    }

    protected static class Player
    {
        public int playerOid;
        public int score;
        public int availFlow;

        public int percentile;
        public int flowAward;

        public Player (int playerOid, int score, int availFlow) {
            this.playerOid = playerOid;
            this.score = score;
            this.availFlow = availFlow;
        }

        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Keep our invocation service registration so that we can unload it at shutdown. */
    protected InvocationMarshaller _invmarsh;

    /** The base flow per player per minute rate that can be awarded by this game. */
    protected int _flowPerMinute = -1; // marker for 'unknown'.

    /** If true, the clock is ticking and participants are earning flow potential. */
    protected boolean _tracking;

    /** Counts the total number of seconds that have elapsed during 'tracked' time, for each
     * tracked member that is no longer present with a FlowRecord. */
    protected int _totalTrackedSeconds = 0;

    /** Tracks accumulated playtime for all players in the game. */
    protected HashIntMap<FlowRecord> _flowRecords = new HashIntMap<FlowRecord>();

    /** From WhirledGameControl.as. */
    protected static final int CASCADING_PAYOUT = 0;

    /** From WhirledGameControl.as. */
    protected static final int WINNERS_TAKE_ALL = 1;

    /** From WhirledGameControl.as. */
    protected static final int TO_EACH_THEIR_OWN = 2;
}
