//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Comparators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.media.util.MathUtil;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.rating.server.RatingDelegate;
import com.threerings.parlor.rating.util.Percentiler;

import com.whirled.game.data.WhirledGameObject;
import com.whirled.game.server.WhirledGameManager;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.game.data.PlayerObject;

import static com.threerings.msoy.Log.log;

/**
 * Handles Whirled game services like paying out coins.
 */
public class AwardDelegate extends RatingDelegate
{
    /**
     * Creates a Whirled game manager delegate with the supplied game content.
     */
    public AwardDelegate (GameContent content)
    {
        // keep our game content around for later
        _content = content;
    }

    // from interface WhirledGameProvider
    public void endGameWithScores (ClientObject caller, int[] playerOids, int[] scores,
                                   int payoutType, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayerOrAgent(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }

        // TEMP? Log scores.
        List<Integer> playerOidList = CollectionUtil.addAll(
            new ArrayList<Integer>(), IntListUtil.box(playerOids));
        log.info("endGameWithScores", "name", _content.game.name, "id", _content.game.gameId,
                 "payoutType", payoutType, "playerOids", playerOids,
                 "playerIds", playerOidsToMemberIds(playerOidList, false), "scores", scores);

        int now = now();

        // convert the players into record indexed on player oid which will weed out duplicates and
        // avoid funny business
        int highestScore = Integer.MIN_VALUE;
        IntMap<Player> players = IntMaps.newHashIntMap();
        for (int ii = 0; ii < playerOids.length; ii++) {
            int availFlow = getAwardableFlow(now, playerOids[ii]);
            players.put(playerOids[ii], new Player(playerOids[ii], scores[ii], availFlow));
            int thisScore = scores[ii];
            highestScore = Math.max(highestScore, thisScore);
        }

        // update stats: we assume (for stat purposes) in games with scores that everyone who has
        // the highscore is a winner and everyone else is a loser; this is somewhat dubious
        List<Integer> winnerOids = Lists.newArrayList();
        if (highestScore > 0) {
            for (int ii = 0; ii < playerOids.length; ii++) {
                if (scores[ii] == highestScore) {
                    winnerOids.add(playerOids[ii]);
                }
            }
        }
        updatePlayerStats(players.keySet(), winnerOids);

        // if we have no non-zero scores then end the game without awarding flow or updating
        // ratings or percentilers
        if (highestScore <= 0) {
            _gmgr.endGame();
            return;
        }

        // record the scores of all players in the game
        Percentiler tiler = getScoreDistribution();
        for (Player player : players.values()) {
            tiler.recordValue(player.score);
        }

        // convert scores to percentiles
        for (Player player : players.values()) {
            player.percentile = getPercentile(tiler, player.score);
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // compute new ratings if appropriate
        if (shouldRateGame()) {
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
        }

        // now actually end the game
        _gmgr.endGame();
    }

    // from interface WhirledGameProvider
    public void endGameWithWinners (ClientObject caller, int[] winnerOids, int[] loserOids,
                                    int payoutType, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayerOrAgent(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }
        int now = now();

        // convert the players into records indexed on player oid to weed out duplicates and avoid
        // any funny business
        IntMap<Player> players = IntMaps.newHashIntMap();
        for (int ii = 0; ii < winnerOids.length; ii++) {
            Player pl = new Player(winnerOids[ii], 1, getAwardableFlow(now, winnerOids[ii]));
            // everyone gets ranked as a 50% performance in multiplayer and we award portions of
            // the losers' winnings to the winners
            pl.percentile = 49;
            players.put(winnerOids[ii], pl);
        }
        for (int ii = 0; ii < loserOids.length; ii++) {
            Player pl = new Player(loserOids[ii], 0, getAwardableFlow(now, loserOids[ii]));
            pl.percentile = 49;
            players.put(loserOids[ii], pl);
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // pull the winners out of the IntMap
        ArrayIntSet winners = new ArrayIntSet();
        for (Player player : players.values()) {
            if (player.score == 1) {
                winners.add(player.playerOid);
            }
        }

        // Update stats. We do this here instead of in gameDidEnd, because the list of winners
        // isn't available there if endGameWithScores() is called.
        updatePlayerStats(players.keySet(), winners);

        // tell the game manager about our winners which will be used to compute ratings, etc.
        if (_gmgr instanceof WhirledGameManager) {
            ((WhirledGameManager)_gmgr).setWinners(winners.toIntArray());

        } else {
            log.warning("Unable to configure WhirledGameManager with winners", "where", where(),
                        "isa", _gmgr.getClass().getName());
        }

        // now actually end the game
        _gmgr.endGame();
    }

    @Override
    public void didStartup (PlaceObject plobj)
    {
        super.didStartup(plobj);

        // compute our flow per minute
        float minuteRate = RuntimeConfig.server.hourlyGameFlowRate / 60f;
        _flowPerMinute = Math.round(minuteRate * _content.detail.getPayoutFactor());
    }

    @Override
    public void didShutdown ()
    {
        super.didShutdown();

        stopTracking();
        resetTracking();

        // pay out to all the players who have not yet been paid
        int[] oids = _flowRecords.intKeySet().toIntArray();
        for (int oid : oids) {
            payoutPlayer(oid);
        }

        // put the kibosh on further flow tracking
        _flowPerMinute = -1;

        // if we were played for zero seconds, don't bother updating anything
        if (_totalTrackedSeconds == 0) {
            return;
        }

        // update our statistics for this game (plays, duration, etc.)
        int totalMinutes = Math.round(_totalTrackedSeconds / 60f);

        // to avoid a single anomalous game freaking out out our distribution, cap game duration at
        // 120% of the current average which will allow many long games to bring up the average
        int perPlayerDuration = _totalTrackedSeconds/_totalTrackedGames;
        int avgDuration = Math.round(60 * getAverageGameDuration(perPlayerDuration));
        int capDuration = 5 * avgDuration / 4;
        if (perPlayerDuration > capDuration) {
            log.info("Capping player minutes at 120% of average", "game", where(),
                     "pgames", _totalTrackedGames, "average", avgDuration,
                     "current", perPlayerDuration, "capped", capDuration);
            totalMinutes = capDuration * _totalTrackedGames;
        }

        // update our in-memory record to reflect this gameplay
        _content.detail.flowToNextRecalc -= _totalAwardedFlow;
        _content.detail.gamesPlayed += _totalTrackedGames;

        // determine whether or not it's time to recalculate this game's payout factor
        final int hourlyRate = RuntimeConfig.server.hourlyGameFlowRate;
        final int newFlowToNextRecalc;
        if (_content.detail.flowToNextRecalc <= 0) {
            newFlowToNextRecalc = RuntimeConfig.server.payoutFactorReassessment * hourlyRate +
                _content.detail.flowToNextRecalc;
            _content.detail.flowToNextRecalc = newFlowToNextRecalc;
        } else {
            newFlowToNextRecalc = 0;
        }

        // record this gameplay for future game metrics tracking and blah blah
        final int gameId = _content.detail.gameId, playerMins = Math.max(totalMinutes, 1);
        _invoker.postUnit(new RepositoryUnit("updateGameDetail(" + gameId + ")") {
            public void invokePersist () throws Exception {
                // note that this game was played
                _gameRepo.noteGamePlayed(
                    gameId, isMultiplayer(), _totalTrackedGames, playerMins, _totalAwardedFlow);
                // if it's time to recalc our payout factor, do that
                if (newFlowToNextRecalc > 0) {
                    _newData = _gameRepo.computeAndUpdatePayoutFactor(
                        gameId, newFlowToNextRecalc, hourlyRate);
                }
            }
            public void handleSuccess () {
                // update the in-memory detail record if we changed things
                if (_newData != null) {
                    _content.detail.payoutFactor = _newData[0];
                    _content.detail.avgSingleDuration = _newData[1];
                    _content.detail.avgMultiDuration = _newData[2];
                }
            }
            protected int[] _newData;
        });
    }

    @Override // from PlaceManagerDelegate
    public void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);

        // potentially create a flow record for this occupant
        if (!_flowRecords.containsKey(bodyOid) && plobj != null) {
            FlowRecord record = new FlowRecord(plobj.getMemberId(), plobj.getHumanity());
            _flowRecords.put(bodyOid, record);
            // if we're currently tracking, note that they're "starting" immediately
            if (_tracking) {
                record.beganStamp = now();
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
        resetTracking();
    }

    @Override // from RatingDelegate
    protected int minimumRatedDuration ()
    {
        return 10; // don't rate games that last less than 10 seconds
    }

    @Override // from RatingDelegate
    protected void updateRatingInMemory (int gameId, Rating rating)
    {
        // we don't keep in-memory ratings for whirled
    }

    /**
     * Called when a game ends to update various Passport-related stats.
     */
    protected void updatePlayerStats (Iterable<Integer> playerOids, Iterable<Integer> winnerOids)
    {
        // we're currently not persisting any stats for in-development games
        if (Game.isDeveloperVersion(_content.detail.gameId)) {
            return;
        }

        List<Integer> memberIds = playerOidsToMemberIds(playerOids, true);
        for (int memberId : memberIds) {
            // track total game sessions
            _worldClient.incrementStat(memberId, StatType.GAME_SESSIONS, 1);
            // track unique games played
            _worldClient.addToSetStat(memberId, StatType.UNIQUE_GAMES_PLAYED,
                _content.detail.gameId);

            if (isMultiplayer()) {
                // track unique game partners
                for (int otherMemberId : memberIds) {
                    if (otherMemberId != memberId) {
                        _worldClient.addToSetStat(
                            memberId, StatType.MP_GAME_PARTNERS, otherMemberId);
                    }
                }
            }
        }

        // track multiplayer game wins
        if (isMultiplayer()) {
            for (int memberId : playerOidsToMemberIds(winnerOids, true)) {
                _worldClient.incrementStat(memberId, StatType.MP_GAMES_WON, 1);
            }
        }
    }

    protected List<Integer> playerOidsToMemberIds (
        Iterable<Integer> playerOids, boolean pruneGuests)
    {
        final List<Integer> memberIds = Lists.newArrayList();
        for (int playerOid : playerOids) {
            DObject dobj = _omgr.getObject(playerOid);
            if (dobj instanceof PlayerObject) {
                int memberId = ((PlayerObject)dobj).getMemberId();
                if (!MemberName.isGuest(memberId) || !pruneGuests) {
                    memberIds.add(memberId);
                }
            }
        }
        return memberIds;
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

        log.info("Updated rating", "who", rating.playerName, "orat", orat, "erat", erat,
                 "diff", pctdiff, "K", K, "nrat", nrat);
    }

    protected void awardFlow (IntMap<Player> players, int payoutType)
    {
        if (players.size() == 0) { // sanity check
            return;
        }

        switch (payoutType) {
        case WhirledGameObject.WINNERS_TAKE_ALL: {
            // TODO: review and possibly remove
            // scale all available flow (even losers?) by their performance
            scaleAvailableFlowToPercentiles(players);

            // map the players by score
            TreeMultimap<Integer,Player> rankings = Multimaps.newTreeMultimap();
            for (Player player : players.values()) {
                rankings.put(player.score, player);
            }

            // all the losers contribute their flow to a pool
            int highestScore = rankings.keySet().last();
            int totalLoserFlow = 0;
            for (Integer score : rankings.keySet()) {
                if (score != highestScore) {
                    for (Player player : rankings.get(score)) {
                        totalLoserFlow += player.availFlow;
                    }
                }
            }

            // and the winners divide it up evenly
            int winners = rankings.get(highestScore).size();
            for (Player player : rankings.get(highestScore)) {
                player.flowAward = player.availFlow +
                    (totalLoserFlow / winners); // we keep the roundoff
            }
            break;
        } // end: case WINNERS_TAKE_ALL

        case WhirledGameObject.CASCADING_PAYOUT: {
            // TODO: review and possibly remove
            // scale all available flow (even losers?) by their performance
            scaleAvailableFlowToPercentiles(players);

            // compute the average score
            int totalScores = 0;
            for (Player player : players.values()) {
                totalScores += player.score;
            }
            float averageScore = totalScores / (float)players.size();

            // everyone below the average contributes 50% to the pool
            int totalLoserFlow = 0;
            for (Player player : players.values()) {
                if (player.score < averageScore) {
                    player.flowAward = player.availFlow / 2;
                    totalLoserFlow += (player.availFlow - player.flowAward);
                }
            }

            // everyone at or above the average divides up the pool proportionally
            int totalAboveAverageScores = 0;
            for (Player player : players.values()) {
                if (player.score >= averageScore) {
                    totalAboveAverageScores += Math.round(player.score - averageScore);
                }
            }
            for (Player player : players.values()) {
                if (player.score >= averageScore) {
                    int share = Math.round(player.score - averageScore);
                    if (totalAboveAverageScores > 0) {
                        player.flowAward = player.availFlow +
                            (totalLoserFlow * share / totalAboveAverageScores);
                    }
                }
            }
            break;
        } // end: case CASCADE_PAYOUT

        default:
        case WhirledGameObject.TO_EACH_THEIR_OWN:
            scaleAvailableFlowToPercentiles(players);
            for (Player player : players.values()) {
                player.flowAward = player.availFlow;
            }
            break;

        case WhirledGameObject.PROPORTIONAL: {
            int totalFlow = 0;
            int totalScore = 0;
            for (Player player : players.values()) {
                totalScore += player.score;
                totalFlow += player.availFlow;
            }
            for (Player player : players.values()) {
                player.flowAward = (int)Math.floor(((float)totalFlow) * player.score / totalScore);
            }
            break;
        } // end: case PROPORTIONAL
        }

        log.info("Awarding flow", "game", where(), "type", payoutType, "to" + players);

        // finally, award the flow and report it to the player
        boolean actuallyAward = !_content.game.isDeveloperVersion();
        for (Player player : players.values()) {
            FlowRecord record = _flowRecords.get(player.playerOid);
            if (record == null) {
                continue;
            }

            // update the player's member object on their world server
            if (actuallyAward && player.flowAward > 0) {
                _worldClient.reportFlowAward(record.memberId, player.flowAward);
            }

            // report to the game that this player earned some flow
            DObject user = _omgr.getObject(player.playerOid);
            if (user != null) {
                boolean hasCookie = (_plmgr instanceof MsoyGameManager) &&
                    ((WhirledGameObject)_plmgr.getPlaceObject()).userCookies.containsKey(
                    user.getOid());
                user.postMessage(WhirledGameObject.COINS_AWARDED_MESSAGE,
                                 player.flowAward, player.percentile, actuallyAward, hasCookie);
            }

            // accumulate their awarded flow into their flow record; we'll pay it all out in one
            // database action when they leave the room or the game is shutdown
            record.awarded += player.flowAward;
            record.played++;
        }
    }

    /**
     * Scale each player's flow by their percentile performance.
     */
    protected void scaleAvailableFlowToPercentiles (IntMap<Player> players)
    {
        for (Player player : players.values()) {
            player.availFlow = (int)Math.ceil(player.availFlow * (player.percentile / 99f));
        }
    }

    @Override // from RatingDelegate
    protected int getGameId ()
    {
        // single player ratings are stored as -gameId, multi-player as gameId
        int gameId = Math.abs(super.getGameId());
        return isMultiplayer() ? gameId : -gameId;
    }

    @Override
    protected boolean shouldRateGame ()
    {
        // we don't support ratings for non-published games
        return !_content.game.isDeveloperVersion() && super.shouldRateGame();
    }

    /**
     * Returns the average duration for this game in fractional minutes.
     */
    protected float getAverageGameDuration (int playerSeconds)
    {
        int avgSeconds = isMultiplayer() ?
            _content.detail.avgMultiDuration : _content.detail.avgSingleDuration;
        // if we have average duration data for this game, use it
        if (avgSeconds > 0) {
            return avgSeconds / 60f;
        } else {
            // otherwise use this player's time, but cap it
            return Math.min(playerSeconds, MAX_FRESH_GAME_DURATION) / 60f;
        }
    }

    protected boolean isMultiplayer ()
    {
        return ((MsoyGameManager) _gmgr).isMultiplayer();
    }

    protected Percentiler getScoreDistribution ()
    {
        // we want the "rating" game id so we use getGameId()
        Percentiler tiler = _gameReg.getScoreDistribution(getGameId(), isMultiplayer());
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
     * Reset tracking. Done at the end of a game so that the secondsPlayed is
     * fresh for the next round. This may change if games can reward flow without
     * ending the game.
     */
    protected void resetTracking ()
    {
        for (FlowRecord record : _flowRecords.values()) {
            record.resetSecondsPlayed();
        }
    }

    protected int getAwardableFlow (int now, int playerOid)
    {
        FlowRecord record = _flowRecords.get(playerOid);
        if (record == null) {
            return 0;
        }
        int playerSecs = record.getPlayTime(now);
        float avgMins = getAverageGameDuration(playerSecs);
        float playerMins = playerSecs/60f;

//        if (RAY_GETS_HIS_WAY) {
//            // What the fuck?
//            // Not our problem! If someone writes a buggy game, they can deal with the
//            // consequences, fix their game, and reset the score distribution. Why should we
//            // *always* do this wack-a-doo adjustment on all the NORMAL, PERFECTLY-FUNCTIONAL
//            // games just to protect against a theoretical "problem"? Wrong wrong wrong.
//            // This should be simplified so that games can do interesting things without
//            // getting penalized for being "wrong". Example: a racing game with a shortcut
//            // that's really hard to do, but if you can do it, you can shave massive time
//            // off your lap and should reap a corresponding reward.
//
//        } else {
//      // The non-ray-gets-his-way way, which by the way Zell believes is a good idea:
        // a player within 80% of the average time will receive a payout based on the average time
        // to accomodate games where faster performance is desirable; however, below 80% we scale
        // down to prevent players who manage to get a game to payout in a degenerately low time
        // from grinding its payout factor into the ground by exploiting their discovery
        float awardMins;
        if (playerMins >= 0.8f*avgMins) {
            awardMins = avgMins;
        } else if (playerMins >= 0.2f*avgMins) {
            awardMins = playerMins + 0.2f*avgMins;
        } else /* playerMins < 0.2f*avgMins */ {
            awardMins = 2*playerMins;
        }

        // log things for a while so we can see how often and to what extent this happens
        if (awardMins != avgMins) {
            log.info("Scaling player's awardable flow due to short game", "game", where(),
                     "memberId", record.memberId, "avgMins", avgMins, "playerMins", playerMins,
                     "awardMins", awardMins);
        }
//        }

        return Math.round(record.humanity * _flowPerMinute * awardMins);
    }

    protected void payoutPlayer (int oid)
    {
        // remove their flow record and grant them the flow
        final FlowRecord record = _flowRecords.remove(oid);
        if (record == null) {
            log.warning("No flow record found", "oid", oid);
            return;
        }

        // if they're leaving in the middle of things, update their secondsPlayed, just so that
        // it's correct for calculations below
        if (_tracking) {
            record.stopTracking(now());
        }
        // move any accumulated seconds into 'totalSecondsPlayed'
        record.resetSecondsPlayed();

        // if this player earned any flow, they contribute to the game's total accumulated
        // playtime, payouts and other metrics
        if (record.awarded > 0) {
            _totalTrackedSeconds += record.totalSecondsPlayed;
            _totalAwardedFlow += record.awarded;
            _totalTrackedGames += record.played;
        }

        // see if we even care
        if (record.awarded == 0 || MemberName.isGuest(record.memberId) ||
                _content.game.isDeveloperVersion()) {
            return;
        }

        // sanity check that we're initialized
        if (_flowPerMinute == -1) {
            log.warning("Unknown flow rate, but there's a grant. Wha?", "game", where());
            return;
        }

        // see how much they actually get (also uses their totalSecondsPlayed)
        final String details = _content.game.gameId + " " + record.totalSecondsPlayed;

        // actually grant their flow award; we don't need to update their in-memory flow value
        // because we've been doing that all along
        _invoker.postUnit(new Invoker.Unit("grantFlow") {
            public boolean invoke () {
                UserActionDetails action = new UserActionDetails(
                        record.memberId, UserAction.PLAYED_GAME, UserActionDetails.INVALID_ID,
                        _content.game.getType(), _content.game.itemId, details);
                try {
                    _memberRepo.getFlowRepository().grantFlow(action, record.awarded);
                    _gameReg.gamePayout(action, _content.game, record.awarded,
                                        record.totalSecondsPlayed);
                } catch (PersistenceException pe) {
                    log.warning("Failed to grant flow", "amount", record.awarded,
                                "action", action, pe);
                }
                return false;
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
     * A record of flow awarded.
     */
    protected static class FlowRecord
    {
        public float humanity;
        public int memberId;

        public int beganStamp;
        public int secondsPlayed;

        /** Tracks total seconds played even after restarts. */
        public int totalSecondsPlayed;

        public int played;
        public int awarded;

        public FlowRecord (int memberId, float humanity) {
            this.humanity = humanity;
            this.memberId = memberId;
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

        /**
         * To be called when the game is ended or before examining totalSecondsPlayed.
         */
        public void resetSecondsPlayed () {
            totalSecondsPlayed += secondsPlayed;
            secondsPlayed = 0;
        }
    }

    protected static class Player implements Comparable<Player>
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

        public int compareTo (Player other) {
            return Comparators.compare(playerOid, other.playerOid);
        }

        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** The metadata for the game being played. */
    protected GameContent _content;

    /** The base flow per player per minute rate that can be awarded by this game. */
    protected int _flowPerMinute = -1; // marker for 'unknown'.

//    /** The average duration (in seconds) of this game. */
//    protected int _averageDuration;
//
//    /** The number of samples used to compute {@link #_averageDuration}. */
//    protected int _averageSamples;

    /** If true, the clock is ticking and participants are earning flow potential. */
    protected boolean _tracking;

    /** Counts the total number of "games" played by the players during this session. */
    protected int _totalTrackedGames = 0;

    /** Counts the total number of seconds that have elapsed during 'tracked' time, for each
     * tracked member that is no longer present with a FlowRecord. */
    protected int _totalTrackedSeconds = 0;

    /** Counts the total amount of flow awarded to players in this game. */
    protected int _totalAwardedFlow = 0;

    /** Tracks accumulated playtime for all players in the game. */
    protected IntMap<FlowRecord> _flowRecords = IntMaps.newHashIntMap();

    /** Games for which we have no history earn no flow beyond this many minutes. */
    protected static final int MAX_FRESH_GAME_DURATION = 8*60;

    /** We require at least this many data points before we'll consider a percentile distribution
     * to be sufficiently valid that we use it to compute performance. */
    protected static final int MIN_VALID_SCORES = 10;

    /** If we lack a valid or sufficiently large score distribution, we use this performance. */
    protected static final int DEFAULT_PERCENTILE = 50;

    // our dependencies
    @Inject protected GameGameRegistry _gameReg;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GameRepository _gameRepo;
}
