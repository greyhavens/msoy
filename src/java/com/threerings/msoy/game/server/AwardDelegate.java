//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.StringUtil;

import com.threerings.media.util.MathUtil;
import com.threerings.util.Name;
import com.threerings.util.TimeUtil;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.rating.server.RatingDelegate;
import com.threerings.parlor.rating.util.Percentiler;

import com.whirled.game.client.WhirledGameService;
import com.whirled.game.data.WhirledGameObject;
import com.whirled.game.server.WhirledGameManager;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.GameGameRegistry.MetricType;
import com.threerings.msoy.game.server.PlayerLocator;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

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

    /**
     * Flushes the pending coin earnings for the specified player (if they are not a player of this
     * game or have no pending coin earnings, this method does nothing).
     */
    public void flushCoinEarnings (final int playerId)
    {
        // flow record is indexed by oid not playerId so we have to search
        FlowRecord record = _flowRecords.get(playerId);
        if (record != null) {
            // payout their pending earnings (this will NOOP if they have nothing pending)
            payoutCoins(record.name.getMemberId(), record.getAndNoteAward(),
                        record.getAndNoteSecondsPlayed());
        }
    }

    /**
     * Handles {@link WhirledGameService#endGameWithScores}.
     */
    public void endGameWithScores (ClientObject caller, int[] playerIds, int[] scores,
                                   int payoutType, int gameMode,
                                   InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayerOrAgent(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }

        // convert the players into record indexed on player oid which will weed out duplicates and
        // avoid funny business
        long now = System.currentTimeMillis();
        int highestScore = Integer.MIN_VALUE;
        IntMap<Player> players = IntMaps.newHashIntMap();
        for (int ii = 0; ii < playerIds.length; ii++) {
            int availFlow = getAwardableFlow(now, playerIds[ii]);
            Player player = createPlayer(playerIds[ii], scores[ii], availFlow);
            if (player != null) {
                players.put(playerIds[ii], player);
                highestScore = Math.max(highestScore, player.score); // used capped score
            }
        }

        // note whether any guests were involved in this game
        _gameInvolvedGuest = Iterables.any(players.values(), IS_GUEST);

        log.info("endGameWithScores", "game", where(), "payoutType", payoutType,
                 "players", players.values());

        // if we have no non-zero scores then end the game without awarding flow or updating
        // ratings or percentilers
        if (highestScore <= 0) {
            _gmgr.endGame();
            return;
        }

        // update the various game-related stats
        updatePlayerStats(players.values(), highestScore);

        // record the scores of all players in the game
        Percentiler tiler = getScoreDistribution(gameMode);
        for (Player player : players.values()) {
            // we want to avoid hackers or bugs totally freaking out the score distribution, so we
            // do some sanity checking of the score value before recording it
            tiler.recordValue(getCappedScore(tiler, player));
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
                Player player = players.get(rating.playerId);
                if (player != null) {
                    updateScoreBasedRating(player, rating);
                }
            }

            int[] nratings = new int[_playerIds.length];
            for (int ii = 0; ii < nratings.length; ii ++) {
                // don't bother computing ratings for guests
                if (_playerIds[ii] != 0) {
                    nratings[ii] = computeRating(ii);
                }
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
    public void endGameWithWinners (ClientObject caller, int[] winnerIds, int[] loserIds,
                                    int payoutType, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayerOrAgent(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }
        long now = System.currentTimeMillis();

        // convert the players into records indexed on player oid to weed out duplicates and avoid
        // any funny business
        IntMap<Player> players = IntMaps.newHashIntMap();
        for (int winnerId : winnerIds) {
            Player pl = createPlayer(winnerId, 1, getAwardableFlow(now, winnerId));
            if (pl != null) {
                // everyone gets ranked as a 50% performance in multiplayer and we award portions of
                // the losers' winnings to the winners
                pl.percentile = 49;
                players.put(winnerId, pl);
            }
        }
        for (int loserId : loserIds) {
            Player pl = createPlayer(loserId, 0, getAwardableFlow(now, loserId));
            if (pl != null) {
                pl.percentile = 49;
                players.put(loserId, pl);
            }
        }

        // note whether any guests were involved in this game
        _gameInvolvedGuest = Iterables.any(players.values(), IS_GUEST);

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // update the various game-related stats
        updatePlayerStats(players.values(), 1);

        // tell the game manager about our winners which will be used to compute ratings, etc.
        if (_gmgr instanceof WhirledGameManager) {
            List<Name> winners = Lists.newArrayList();
            for (Player player : players.values()) {
                if (player.score == 1) {
                    winners.add(player.name);
                }
            }
            ((WhirledGameManager)_gmgr).setWinners(winners.toArray(new Name[winners.size()]));
        } else {
            log.warning("Unable to configure WhirledGameManager with winners", "game", where(),
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
        float minuteRate = _runtime.money.hourlyGameFlowRate / 60f;
        _flowPerMinute = Math.round(minuteRate * _content.metrics.getPayoutFactor());
    }

    @Override
    public void didShutdown ()
    {
        super.didShutdown();

        stopTracking();
        resetTracking();

        // pay out to all the players who have not yet been paid
        int[] ids = _flowRecords.intKeySet().toIntArray();
        for (int id : ids) {
            payoutPlayer(id);
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
            totalMinutes = Math.round(capDuration * _totalTrackedGames / 60f);
            log.info("Capping player minutes at 120% of average", "game", where(),
                     "pgames", _totalTrackedGames, "average", avgDuration,
                     "current", perPlayerDuration, "capped", capDuration,
                     "totalMins", totalMinutes);
        }

        // record that games were played and potentially update our payout factor
        _gameReg.updateGameMetrics(_content.metrics, isMultiplayer() ? MetricType.MULTI_PLAYER :
            MetricType.SINGLE_PLAYER, totalMinutes, _totalTrackedGames, _totalAwardedFlow);
    }

    @Override // from PlaceManagerDelegate
    public void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);

        // potentially create a flow record for this occupant
        if (plobj != null && !_flowRecords.containsKey(plobj.getMemberId())) {
            FlowRecord record = new FlowRecord(plobj);
            _flowRecords.put(plobj.getMemberId(), record);
            // if we're currently tracking, note that they're "starting" immediately
            if (_tracking) {
                record.startTracking(System.currentTimeMillis());
            }
        }
    }

    @Override // from PlaceManagerDelegate
    public void bodyLeft (final int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // when a player leaves the game, pay out their earned flow (flow records are not mapped by
        // oid, so we have to search)
        FlowRecord record = Iterables.find(_flowRecords.values(), new Predicate<FlowRecord>() {
            public boolean apply (FlowRecord record) {
                return record.bodyOid == bodyOid;
            }
        });
        if (record != null) {
            payoutPlayer(record.name.getMemberId());
        }
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
        return 60; // don't rate games that last less than 60 seconds
    }

    @Override // from RatingDelegate
    protected void updateRatingInMemory (int gameId, Rating rating)
    {
        // we don't keep in-memory ratings for whirled
    }

    /**
     * Called when a game ends to update various Passport-related stats.
     */
    protected void updatePlayerStats (Iterable<Player> players, int winningScore)
    {
        // we're currently not persisting any stats for in-development games,
        // nor games that didn't last long enough to be rated
        int gameSecs = TimeUtil.elapsedSeconds(_startStamp, System.currentTimeMillis());
        if (_content.isDevelopmentVersion || (gameSecs < minimumRatedDuration())) {
            return;
        }

        for (Player player : players) {
            int memberId = player.getMemberId();

            // track total game sessions
            _gameReg.incrementStat(memberId, StatType.GAME_SESSIONS, 1);
            // track unique games played
            // Note: commented out because we don't have a badge for this right now
            /*_gameReg.addToSetStat(
                memberId, StatType.UNIQUE_GAMES_PLAYED, _content.game.gameId);*/

            if (isMultiplayer()) {
                // track multiplayer game wins
                if (player.score == winningScore) {
                    _gameReg.incrementStat(memberId, StatType.MP_GAMES_WON, 1);
                }

                // track unique game partners
                // Note - commented out because we don't have a badge for this right now
                /*for (Player oplayer : players) {
                    int oMemberId = oplayer.getMemberId();
                    if (oMemberId != memberId) {
                        _gameReg.addToSetStat(memberId, StatType.MP_GAME_PARTNERS, oMemberId);
                    }
                }*/
            }
        }
    }

    protected Player createPlayer (int playerId, int score, int availFlow)
    {
        FlowRecord record = _flowRecords.get(playerId);
        if (record == null) {
            // playerId came from the game, which may have been smoking up the crack, so only
            // complain if they provide a positive playerId which does not correspond to a player
            if (playerId > 0) {
                log.warning("Missing flow record for player", "game", where(), "id", playerId);
            }
            return null;
        }
        return new Player(record.name, record.isGuest, score, availFlow);
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

        log.info("Updated rating", "game", where(), "who", rating.playerName, "orat", orat,
                 "erat", erat, "diff", pctdiff, "K", K, "nrat", nrat);
    }

    protected void awardFlow (IntMap<Player> players, int payoutType)
    {
        if (players.size() == 0) { // sanity check
            return;
        }

        switch (payoutType) {
        case WhirledGameObject.WINNERS_TAKE_ALL: {
            // scale all available flow (even losers?) by their performance
            scaleAvailableFlowToPercentiles(players);

            // map the players by score
            TreeMultimap<Integer,Player> rankings = TreeMultimap.create();
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

        log.info("Awarding flow", "game", where(), "type", payoutType, "to", players.values());

        // finally, award the flow and report it to the player
        boolean actuallyAward = !_content.isDevelopmentVersion;
        for (Player player : players.values()) {
            FlowRecord record = _flowRecords.get(player.name.getMemberId());
            if (record == null) {
                continue;
            }

            // update the player's member object on their world server
            if (actuallyAward && player.flowAward > 0) {
                _gameReg.reportCoinAward(record.name.getMemberId(), player.flowAward);
            }

            // report to the game that this player earned some flow
            PlayerObject user = _locator.lookupPlayer(player.name);
            if (user != null) {
                boolean hasCookie = (_plmgr instanceof ParlorGameManager) &&
                    ((WhirledGameObject)_plmgr.getPlaceObject()).userCookies.containsKey(
                    user.getOid());
                user.postMessage(WhirledGameObject.COINS_AWARDED_MESSAGE,
                                 player.flowAward, player.percentile, actuallyAward, hasCookie);
            }

            // accumulate their awarded flow into their flow record; we'll pay it all out in one
            // database action when they leave the room or the game is shutdown
            record.increaseAward(player.flowAward);
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

    /**
     * Returns the average duration for this game in fractional minutes.
     */
    protected float getAverageGameDuration (int playerSeconds)
    {
        int avgSeconds = isMultiplayer() ?
            _content.metrics.avgMultiDuration : _content.metrics.avgSingleDuration;
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
        return ((ParlorGameManager) _gmgr).isMultiplayer();
    }

    protected Percentiler getScoreDistribution (int gameMode)
    {
        // we want the "rating" game id so we use getGameId()
        return _gameReg.getScoreDistribution(getGameId(), isMultiplayer(), gameMode);
    }

    protected int getPercentile (Percentiler tiler, int score)
    {
        return (tiler.getRecordedCount() < MIN_VALID_SCORES) ?
            DEFAULT_PERCENTILE : tiler.getPercentile(score);
    }

    protected int getCappedScore (Percentiler tiler, Player player)
    {
        // we cap scores at +/- 150% outside a range of [-100,100], we allow +/-100 to ensure that
        // the scores can grow to a size where +/- 150% does not round back into the existing range
        // and thereby break our "slow expansion" allowance
        int range = tiler.getMaxScore() - tiler.getMinScore();
        int minmin = Math.min(tiler.getMinScore() - range/2, -100);
        int maxmax = Math.max(tiler.getMaxScore() + range/2, 100);

        if (tiler.getRecordedCount() < MIN_VALID_SCORES) {
            return player.score;
        } else if (player.score < minmin) {
            log.warning("Capping extremely low score", "game", where(), "player", player.name,
                        "score", player.score, "min", tiler.getMinScore(), "range", range);
            return minmin;
        } else if (player.score > maxmax) {
            log.warning("Capping extremely high score", "game", where(), "player", player.name,
                        "score", player.score, "max", tiler.getMaxScore(), "range", range);
            return maxmax;
        } else {
            return player.score;
        }
    }

    protected void startTracking ()
    {
        if (_tracking) {
            return;
        }
        _tracking = true;

        // note the time at which we started for flow calculations
        long startStamp = System.currentTimeMillis();
        for (FlowRecord record : _flowRecords.values()) {
            record.startTracking(startStamp);
        }
    }

    protected void stopTracking ()
    {
        if (!_tracking) {
            return;
        }
        _tracking = false;

        // note all remaining player's seconds played
        long endStamp = System.currentTimeMillis();
        for (FlowRecord record : _flowRecords.values()) {
            record.stopTracking(endStamp);
        }
    }

    /**
     * Reset tracking. Done at the end of a game so that the secondsPlayed is fresh for the next
     * round. This may change if games can reward flow without ending the game.
     */
    protected void resetTracking ()
    {
        for (FlowRecord record : _flowRecords.values()) {
            record.accumSecondsPlayed();
        }
    }

    protected int getAwardableFlow (long now, int playerId)
    {
        FlowRecord record = _flowRecords.get(playerId);
        if (record == null) {
            return 0;
        }
        int playerSecs = record.getPlayTime(now);
        float avgMins = getAverageGameDuration(playerSecs);
        float playerMins = playerSecs/60f;

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
                     "who", record.name, "avgMins", avgMins, "playerMins", playerMins,
                     "awardMins", awardMins);
        }

        return Math.round(record.humanity * _flowPerMinute * awardMins);
    }

    protected void payoutPlayer (int playerId)
    {
        // remove their flow record and grant them the flow
        FlowRecord record = _flowRecords.remove(playerId);
        if (record == null) {
            log.warning("No flow record found", "game", where(), "id", playerId);
            return;
        }

        // if they're leaving in the middle of things, update their secondsPlayed, just so that
        // it's correct for calculations below
        if (_tracking) {
            record.stopTracking(System.currentTimeMillis());
        }
        // note any pending accumulated time
        record.accumSecondsPlayed();

        // if this player earned any flow, they contribute to the game's total accumulated
        // playtime, payouts and other metrics
        if (record.getTotalAward() > 0) {
            _totalTrackedSeconds += record.getTotalSecondsPlayed();
            _totalAwardedFlow += record.getTotalAward();
            _totalTrackedGames += record.played;
        }

        // see if we even care
        if (record.getTotalAward() == 0 || _content.isDevelopmentVersion) {
            return;
        }

        // sanity check that we're initialized
        if (_flowPerMinute == -1) {
            log.warning("Unknown flow rate, but there's a grant. Wha?", "game", where());
            return;
        }

        // actually grant their flow award (this may only be partial as we may have flushed pending
        // coins due to an earlier request)
        payoutCoins(record.name.getMemberId(), record.getAndNoteAward(),
                    record.getAndNoteSecondsPlayed());

        // and let the game registry know that we paid out this player (this will be their total
        // award because we only do this when they finally leave the game)
        _gameReg.gameDidPayout(record.name.getMemberId(), _content.game, record.getTotalAward(),
                               record.getTotalSecondsPlayed());
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
        // don't rate games involving guests, and don't rate non-published games
        return !_gameInvolvedGuest && !_content.isDevelopmentVersion && super.shouldRateGame();
    }

    protected void payoutCoins (int memberId, int coinAward, int secondsPlayed)
    {
        if (coinAward > 0) {
            UserAction action = UserAction.playedGame(
                memberId, _content.game.name, _content.game.gameId, secondsPlayed);
            _gameReg.awardCoins(_content.game.gameId, action, coinAward);
        }
    }

    /**
     * A record of flow awarded.
     */
    protected static class FlowRecord
    {
        public int bodyOid;
        public MemberName name;
        public float humanity;
        public boolean isGuest;

        public int played;

        public FlowRecord (PlayerObject plobj) {
            this.bodyOid = plobj.getOid();
            this.name = plobj.memberName;
            this.humanity = plobj.getHumanity();
            this.isGuest = plobj.isPermaguest();
        }

        public int getPlayTime (long now) {
            int secondsOfPlay = _sessionSecondsPlayed;
            if (_beganStamp != 0) {
                secondsOfPlay += TimeUtil.elapsedSeconds(_beganStamp, now);
            }
            return secondsOfPlay;
        }

        public void startTracking (long startStamp) {
            _beganStamp = startStamp;
        }

        public void stopTracking (long endStamp) {
            if (_beganStamp != 0) {
                _sessionSecondsPlayed += TimeUtil.elapsedSeconds(_beganStamp, endStamp);
                _beganStamp = 0;
            }
        }

        public void accumSecondsPlayed () {
            _unnotedSecondsPlayed += _sessionSecondsPlayed;
            _sessionSecondsPlayed = 0;
        }

        public int getTotalAward () {
            return _unnotedAward + _notedAward;
        }

        public int getTotalSecondsPlayed () {
            return _unnotedSecondsPlayed + _notedSecondsPlayed;
        }

        public void increaseAward (int amount) {
            _unnotedAward += amount;
        }

        public void increasePlayTime (int secondsPlayed) {
            _unnotedSecondsPlayed += secondsPlayed;
        }

        public int getAndNoteAward () {
            int award = _unnotedAward;
            _notedAward += award;
            _unnotedAward = 0;
            return award;
        }

        public int getAndNoteSecondsPlayed () {
            int secondsPlayed = _unnotedSecondsPlayed;
            _notedSecondsPlayed += secondsPlayed;
            _unnotedSecondsPlayed = 0;
            return secondsPlayed;
        }

        protected int _unnotedAward;
        protected int _notedAward;

        protected long _beganStamp;
        protected int _sessionSecondsPlayed;

        protected int _unnotedSecondsPlayed;
        protected int _notedSecondsPlayed;
    }

    protected static class Player implements Comparable<Player>
    {
        public MemberName name;
        public boolean isGuest;
        public int score;
        public int availFlow;

        public int percentile;
        public int flowAward;

        public Player (MemberName name, boolean isGuest, int score, int availFlow) {
            this.name = name;
            this.isGuest = isGuest;
            this.score = Math.max(0, Math.min(MAX_ALLOWED_SCORE, score));
            this.availFlow = availFlow;
        }

        public int getMemberId () {
            return (name == null) ? 0 : name.getMemberId();
        }

        public int compareTo (Player other) {
            return name.compareTo(other.name);
        }

        @Override // from Object
        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** The metadata for the game being played. */
    protected GameContent _content;

    /** The base flow per player per minute rate that can be awarded by this game. */
    protected int _flowPerMinute = -1; // marker for 'unknown'.

    /** Indicates whether the most recently finished game involved a guest player. Set in {@link
     * #endGameWithWinners} or {@link #endGameWithScores} and used in {@link #shouldRateGame}. */
    protected boolean _gameInvolvedGuest;

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

    // our dependencies
    @Inject protected PlayerLocator _locator;
    @Inject protected GameGameRegistry _gameReg;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected RuntimeConfig _runtime;

    /** Returns whether or not a {@link Player} is a guest. */
    protected static final Predicate<Player> IS_GUEST = new Predicate<Player>() {
        public boolean apply (Player player) {
            return player.isGuest;
        }
    };

    /** Games for which we have no history earn no flow beyond this many minutes. */
    protected static final int MAX_FRESH_GAME_DURATION = 8*60;

    /** We require at least this many data points before we'll consider a percentile distribution
     * to be sufficiently valid that we use it to compute performance. */
    protected static final int MIN_VALID_SCORES = 10;

    /** If we lack a valid or sufficiently large score distribution, we use this performance. */
    protected static final int DEFAULT_PERCENTILE = 50;

    /** The highest allowed score. The lowest allowed score is zero. We choose this range to avoid
     * having to guard against integer overflow in our various score calculations. */
    protected static final int MAX_ALLOWED_SCORE = 1073741824; // 2^30
}
