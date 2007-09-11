//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.media.util.MathUtil;
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
import com.threerings.parlor.rating.util.Percentiler;

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
import com.threerings.msoy.item.server.persist.GameDetailRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

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
        int now = now();

        // convert the players into record indexed on player oid which will weed out duplicates and
        // avoid funny business
        HashIntMap<Player> players = new HashIntMap<Player>();
        for (int ii = 0; ii < playerOids.length; ii++) {
            int availFlow = getAwardableFlow(now, playerOids[ii]);
            players.put(playerOids[ii], new Player(playerOids[ii], scores[ii], availFlow));
        }

        // record the scores of all players in the game
        Percentiler tiler = getScoreDistribution();
        for (Player player : players.values()) {
            tiler.recordValue(player.score);
        }

        // convert scores to percentiles, scale available flow
        for (Player player : players.values()) {
            player.percentile = getPercentile(tiler, player.score);
            // scale each players' flow award by their percentile performance
            player.availFlow = (int)Math.ceil(player.availFlow * (player.percentile / 99f));
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // compute new ratings
        for (Rating rating : _ratings.values()) {
            Player player = players.get(rating.playerOid);
            if (player != null) {
                updateScoreBasedRating(player, rating);
            }
        }

        int[] nratings = new int[_playerIds.length];
        for (int ii = 0; ii < nratings.length; ii ++) {
            nratings[ii] = computeRating(ii);
        }

        // and write them back to their rating records
        for (int ii = 0; ii < nratings.length; ii++) {
            Rating rating = _ratings.get(_playerIds[ii]);
            if (rating != null && nratings[ii] > 0) {
                rating.rating = nratings[ii];
                rating.experience++;
                rating.modified = true;
            }
        }

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
        int now = now();

        // convert the players into records indexed on player oid to weed out duplicates and avoid
        // any funny business
        HashIntMap<Player> players = new HashIntMap<Player>();
        for (int ii = 0; ii < winnerOids.length; ii++) {
            Player player = new Player(winnerOids[ii], 1, getAwardableFlow(now, winnerOids[ii]));
            player.percentile = 74; // winners are 75th percentile
            players.put(winnerOids[ii], player);
        }
        for (int ii = 0; ii < loserOids.length; ii++) {
            Player player = new Player(loserOids[ii], 0, getAwardableFlow(now, loserOids[ii]));
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

    @Override
    public void didStartup (PlaceObject plobj)
    {
        super.didStartup(plobj);

        // wire up our WhirledGameService
        if (plobj instanceof WhirledGame) {
            _invmarsh = MsoyBaseServer.invmgr.registerDispatcher(new WhirledGameDispatcher(this));
            ((WhirledGame)plobj).setWhirledGameService((WhirledGameMarshaller)_invmarsh);
        }

        // load up some metadata
        final int gameId = _gmgr.getGameConfig().getGameId();
        MsoyBaseServer.invoker.postUnit(new RepositoryUnit("loadGameDetail") {
            public void invokePersist () throws Exception {
                _result = getGameRepository().loadGameDetail(gameId);
                if (_result == null) {
                    throw new Exception("Missing game detail record.");
                }
            }

            public void handleSuccess () {
                _detail = _result;
                float minuteRate = RuntimeConfig.server.hourlyGameFlowRate / 60f;
                _flowPerMinute = (int)Math.round(minuteRate * _detail.getAntiAbuseFactor());
            }

            public void handleFailure (Exception e) {
                log.log(Level.WARNING, "Failed to fetch game metadata [id=" + gameId + "]", e);
                // we're probably hosed, but use a conservative default anyway
                _flowPerMinute = (int)((RuntimeConfig.server.hourlyGameFlowRate * 0.5f) / 60f);
            }

            protected GameDetailRecord _result;
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

        // update our statistics for this game (plays, duration, etc.)
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

        // if we were played for zero minutes, don't bother updating anything
        if (totalMinutes <= 0) {
            return;
        }

        // sanity check (TODO: bound by percent of existing game duration)
        if (totalMinutes/_allPlayers.size() > 15) {
            log.info("Capping player minutes at 15 mins per game [games=" + _allPlayers.size() +
                     ", mins=" + totalMinutes + "].");
            totalMinutes = _allPlayers.size() * 15;
        }

        final int gameId = _gmgr.getGameConfig().getGameId();
        final int playerGames = _allPlayers.size(), playerMins = totalMinutes;
        final boolean recalc = (RuntimeConfig.server.abuseFactorReassessment == 0) ? false :
            _detail.shouldRecalcAbuse(playerMins, RuntimeConfig.server.abuseFactorReassessment);
        MsoyBaseServer.invoker.postUnit(new Invoker.Unit("updateGameDetail") {
            public boolean invoke () {
                try {
                    getGameRepository().noteGamePlayed(gameId, playerGames, playerMins, recalc);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to note end of game [in=" + where() + "]", pe);
                }
                return false;
            }
        });
    }

    @Override // from PlaceManagerDelegate
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
                    _allPlayers.add(record.memberId);
                }
            }
        }
    }

    @Override // from PlaceManagerDelegate
    public void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // when a player leaves the game, pay out their earned flow
        payoutPlayer(bodyOid);
    }

    @Override // from GameManagerDelegate
    public void gameDidStart ()
    {
        super.gameDidStart();

        // note that all occupants are accumulating "game time"
        startTracking();
    }

    @Override // from GameManagerDelegate
    public void gameDidEnd ()
    {
        super.gameDidEnd();

        // stop accumulating "game time" for players
        stopTracking();
    }

    @Override // from RatingManagerDelegate
    protected int minimumRatedDuration ()
    {
        return 10; // don't rate games that last less than 10 seconds
    }

    @Override // from RatingManagerDelegate
    protected RatingRepository getRatingRepository ()
    {
        return MsoyBaseServer.ratingRepo;
    }

    @Override // from RatingManagerDelegate
    protected void updateRatingInMemory (int gameId, Rating rating)
    {
        // we don't keep in-memory ratings for whirled
    }

    protected void updateScoreBasedRating (Player player, Rating rating)
    {
        // map our percentile to a rating value (0 - 33 map to 1000 and we scale linearly up from
        // there to 3000)
        int erat = Math.max((player.percentile * MAXIMUM_RATING) / 100, MINIMUM_RATING);
        int orat = MathUtil.bound(MINIMUM_RATING, rating.rating, MAXIMUM_RATING);

        // compute the K value. Low exp players get to move more quickly.
        int sessions = rating.experience;
        float K;
        if (sessions < 20) {
            if (sessions < 10) {
                K = 300f; // 0-9 sessions
            } else {
                K = 150f; // 10-19 sessions
            }
        } else {
            K = 75f; // 20+ sessions
        }

        // compute the delta rating as a percentage of the player's current rating (eg. they should
        // have been 12% better or worse)
        float pctdiff = ((float)(erat - orat) / orat);

        // update the player's rating
        int nrat = Math.round(orat + pctdiff * K);

        // make sure the rating remains within a valid range
        rating.rating = MathUtil.bound(MINIMUM_RATING, nrat, MAXIMUM_RATING);
        rating.experience++;
        rating.modified = true;

        log.info("Updated rating [who=" + rating.playerName + ", orat=" + orat + ", erat=" + erat +
                 ", diff=" + pctdiff + ", K=" + K + ", nrat=" + nrat + "].");
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

    @Override // from RatingManagerDelegate
    protected int getGameId ()
    {
        // single player ratings are stored as -gameId, multi-player as gameId
        int gameId = Math.abs(super.getGameId());
        return isMultiPlayer() ? gameId : -gameId;
    }

    @Override // from RatingManagerDelegate
    protected void loadRatings (Collection<Rating> ratings)
    {
        // we don't support ratings for non-published games
        if (_gmgr.getGameConfig().getGameId() < 0) {
            return;
        }
        super.loadRatings(ratings);
    }

    /**
     * Returns the average duration for this game in fractional minutes.
     */
    protected float getAverageGameDuration (int playerSeconds)
    {
        // if we failed to load our detail record, use the player's actual time capped at the max
        if (_detail == null) {
            return Math.min(MAX_FRESH_GAME_DURATION, playerSeconds / 60f);
        }

        // if we've got enough data to trust the average, simply return it
        float minutes = _detail.playerMinutes;
        int samples = _detail.playerGames;
        if (samples > FRESH_GAME_CUTOFF) {
            return minutes / samples;
        }

        // otherwise incorporate this player's time into the average and cap it
        minutes += (playerSeconds / 60f);
        samples++;
        return Math.min(minutes / samples, MAX_FRESH_GAME_DURATION);
    }

    protected boolean isMultiPlayer ()
    {
        switch (_gmgr.getGameConfig().getMatchType()) {
        case GameConfig.PARTY:
            // all party games are multiplayer; we can't know when the game starts whether more
            // than one player will show up so we must always load and save multiplayer ratings and
            // percentile information
            return true;

        case GameConfig.SEATED_CONTINUOUS:
            // same goes for seated continuous where players can show up after the game starts
            return true;

        default:
        case GameConfig.SEATED_GAME:
            return (_gmgr.getGameConfig().players.length > 1);
        }
    }

    protected Percentiler getScoreDistribution ()
    {
        Percentiler tiler = null;
        // if we're not running on a game server, we don't have score distributions
        if (MsoyGameServer.gameReg != null) {
            tiler = MsoyGameServer.gameReg.getScoreDistribution(getGameId(), isMultiPlayer());
        }
        // if for whatever reason we don't have a score distribution, return a blank one which will
        // result in the default percentile being used
        return (tiler == null) ? new Percentiler() : tiler;
    }

    protected int getPercentile (Percentiler tiler, int score)
    {
        return (tiler.getRecordedCount() < MIN_VALID_SCORES) ?
            DEFAULT_PERCENTILE : tiler.getPercentile(score);
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
            _allPlayers.add(record.memberId);
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

    protected int getAwardableFlow (int now, int playerOid)
    {
        FlowRecord record = _flowRecords.get(playerOid);
        if (record == null) {
            return 0;
        }
        float minutes = getAverageGameDuration(record.getPlayTime(now));
        return (int)Math.round(record.humanity * _flowPerMinute * minutes);
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

    protected static GameRepository getGameRepository ()
    {
        return (MsoyGameServer.gameReg != null) ?
            MsoyGameServer.gameReg.getGameRepository() : MsoyServer.itemMan.getGameRepository();
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

        public int getPlayTime (int now) {
            int secondsOfPlay = secondsPlayed;
            if (beganStamp != 0) {
                secondsOfPlay += (now - beganStamp);
            }
            return secondsOfPlay;
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

    /** Our detail record for this game. */
    protected GameDetailRecord _detail;

    /** The average duration (in seconds) of this game. */
    protected int _averageDuration;

    /** The number of samples used to compute {@link #_averageDuration}. */
    protected int _averageSamples;

    /** Used to track whether or not we should recalculate our abuse factor. */
    protected int _minsSinceLastAbuseRecalc;

    /** If true, the clock is ticking and participants are earning flow potential. */
    protected boolean _tracking;

    /** Counts the total number of seconds that have elapsed during 'tracked' time, for each
     * tracked member that is no longer present with a FlowRecord. */
    protected int _totalTrackedSeconds = 0;

    /** Used to track how many players participated in this game. */
    protected ArrayIntSet _allPlayers = new ArrayIntSet();

    /** Tracks accumulated playtime for all players in the game. */
    protected HashIntMap<FlowRecord> _flowRecords = new HashIntMap<FlowRecord>();

    /** Once a game has accumulated this many player games, its average time is trusted. */
    protected static final int FRESH_GAME_CUTOFF = 10;

    /** Games for which we have no history earn no flow beyond this many minutes. */
    protected static final int MAX_FRESH_GAME_DURATION = 10;

    /** We require at least this many data points before we'll consider a percentile distribution
     * to be sufficiently valid that we use it to compute performance. */
    protected static final int MIN_VALID_SCORES = 10;

    /** If we lack a valid or sufficiently large score distribution, we use this performance. */
    protected static final int DEFAULT_PERCENTILE = 50;

    /** From WhirledGameControl.as. */
    protected static final int CASCADING_PAYOUT = 0;

    /** From WhirledGameControl.as. */
    protected static final int WINNERS_TAKE_ALL = 1;

    /** From WhirledGameControl.as. */
    protected static final int TO_EACH_THEIR_OWN = 2;
}
