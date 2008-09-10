//
// $Id$

package com.threerings.msoy.game.server;

import com.google.common.base.Predicate;
import com.google.common.collect.Comparators;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
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
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.money.server.MoneyLogic;

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

        // convert the players into record indexed on player oid which will weed out duplicates and
        // avoid funny business
        final int now = now();
        int highestScore = Integer.MIN_VALUE;
        final IntMap<Player> players = IntMaps.newHashIntMap();
        for (int ii = 0; ii < playerOids.length; ii++) {
            final int availFlow = getAwardableFlow(now, playerOids[ii]);
            players.put(playerOids[ii], new Player(lookupName(playerOids[ii]), playerOids[ii],
                                                   scores[ii], availFlow));
            final int thisScore = scores[ii];
            highestScore = Math.max(highestScore, thisScore);
        }

        // note whether any guests were involved in this game
        _gameInvolvedGuest = Iterables.any(players.values(), IS_GUEST);

        log.info("endGameWithScores", "name", _content.game.name, "id", _content.game.gameId,
                 "payoutType", payoutType, "players", players.values());

        // if we have no non-zero scores then end the game without awarding flow or updating
        // ratings or percentilers
        if (highestScore <= 0) {
            _gmgr.endGame();
            return;
        }

        // update the various game-related stats
        updatePlayerStats(players.values(), highestScore);

        // record the scores of all players in the game
        final Percentiler tiler = getScoreDistribution();
        for (final Player player : players.values()) {
            // we want to avoid hackers or bugs totally freaking out the score distribution, so we
            // do some sanity checking of the score value before recording it
            tiler.recordValue(getCappedScore(tiler, player));
        }

        // convert scores to percentiles
        for (final Player player : players.values()) {
            player.percentile = getPercentile(tiler, player.score);
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // compute new ratings if appropriate
        if (shouldRateGame()) {
            for (final Rating rating : _ratings.values()) {
                final Player player = players.get(rating.playerOid);
                if (player != null) {
                    updateScoreBasedRating(player, rating);
                }
            }

            final int[] nratings = new int[_playerIds.length];
            for (int ii = 0; ii < nratings.length; ii ++) {
                nratings[ii] = computeRating(ii);
            }

            // and write them back to their rating records
            for (int ii = 0; ii < nratings.length; ii++) {
                final Rating rating = _ratings.get(_playerIds[ii]);
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
        final int now = now();

        // convert the players into records indexed on player oid to weed out duplicates and avoid
        // any funny business
        final IntMap<Player> players = IntMaps.newHashIntMap();
        for (int ii = 0; ii < winnerOids.length; ii++) {
            final Player pl = new Player(lookupName(winnerOids[ii]), winnerOids[ii], 1,
                                   getAwardableFlow(now, winnerOids[ii]));
            // everyone gets ranked as a 50% performance in multiplayer and we award portions of
            // the losers' winnings to the winners
            pl.percentile = 49;
            players.put(winnerOids[ii], pl);
        }
        for (int ii = 0; ii < loserOids.length; ii++) {
            final Player pl = new Player(lookupName(loserOids[ii]), loserOids[ii], 0,
                                   getAwardableFlow(now, loserOids[ii]));
            pl.percentile = 49;
            players.put(loserOids[ii], pl);
        }

        // note whether any guests were involved in this game
        _gameInvolvedGuest = Iterables.any(players.values(), IS_GUEST);

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // update the various game-related stats
        updatePlayerStats(players.values(), 1);

        // tell the game manager about our winners which will be used to compute ratings, etc.
        if (_gmgr instanceof WhirledGameManager) {
            final ArrayIntSet fWinnerOids = new ArrayIntSet();
            for (final Player player : players.values()) {
                if (player.score == 1) {
                    fWinnerOids.add(player.playerOid);
                }
            }
            ((WhirledGameManager)_gmgr).setWinners(fWinnerOids.toIntArray());
        } else {
            log.warning("Unable to configure WhirledGameManager with winners", "where", where(),
                        "isa", _gmgr.getClass().getName());
        }

        // now actually end the game
        _gmgr.endGame();
    }

    @Override
    public void didStartup (final PlaceObject plobj)
    {
        super.didStartup(plobj);

        // compute our flow per minute
        final float minuteRate = RuntimeConfig.server.hourlyGameFlowRate / 60f;
        _flowPerMinute = Math.round(minuteRate * _content.detail.getPayoutFactor());
    }

    @Override
    public void didShutdown ()
    {
        super.didShutdown();

        stopTracking();
        resetTracking();

        // pay out to all the players who have not yet been paid
        final int[] oids = _flowRecords.intKeySet().toIntArray();
        for (final int oid : oids) {
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
        final int perPlayerDuration = _totalTrackedSeconds/_totalTrackedGames;
        final int avgDuration = Math.round(60 * getAverageGameDuration(perPlayerDuration));
        final int capDuration = 5 * avgDuration / 4;
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
            @Override
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
            @Override
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
    public void bodyEntered (final int bodyOid)
    {
        super.bodyEntered(bodyOid);

        final PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);

        // potentially create a flow record for this occupant
        if (!_flowRecords.containsKey(bodyOid) && plobj != null) {
            final FlowRecord record = new FlowRecord(plobj.memberName, plobj.getHumanity());
            _flowRecords.put(bodyOid, record);
            // if we're currently tracking, note that they're "starting" immediately
            if (_tracking) {
                record.beganStamp = now();
            }
        }
    }

    @Override // from PlaceManagerDelegate
    public void bodyLeft (final int bodyOid)
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
    protected void updateRatingInMemory (final int gameId, final Rating rating)
    {
        // we don't keep in-memory ratings for whirled
    }

    /**
     * Called when a game ends to update various Passport-related stats.
     */
    protected void updatePlayerStats (final Iterable<Player> players, final int winningScore)
    {
        // we're currently not persisting any stats for in-development games
        if (Game.isDevelopmentVersion(_content.detail.gameId)) {
            return;
        }

        for (final Player player : players) {
            final int memberId = player.getMemberId();
            if (MemberName.isGuest(memberId)) {
                continue;
            }

            // track total game sessions
            _worldClient.incrementStat(memberId, StatType.GAME_SESSIONS, 1);
            // track unique games played
            // Note: commented out because we don't have a badge for this right now
            /*_worldClient.addToSetStat(
                memberId, StatType.UNIQUE_GAMES_PLAYED, _content.detail.gameId);*/

            if (isMultiplayer() && ((MsoyGameManager) _gmgr).getPlayerCount() > 1) {
                // track multiplayer game wins
                if (player.score == winningScore) {
                    _worldClient.incrementStat(memberId, StatType.MP_GAMES_WON, 1);
                }

                // track unique game partners
                // Note - commented out because we don't have a badge for this right now
                /*for (Player oplayer : players) {
                    int oMemberId = oplayer.getMemberId();
                    if (oMemberId != memberId && !MemberName.isGuest(oMemberId)) {
                        _worldClient.addToSetStat(memberId, StatType.MP_GAME_PARTNERS, oMemberId);
                    }
                }*/
            }
        }
    }

    protected MemberName lookupName (final int playerOid)
    {
        final FlowRecord record = _flowRecords.get(playerOid);
        return (record == null) ? null : record.name;
    }

    protected void updateScoreBasedRating (final Player player, final Rating rating)
    {
        // map our percentile to a rating value (0 - 33 map to 1000 and we scale linearly up from
        // there to 3000)
        final int erat = Math.max((player.percentile * MAXIMUM_RATING) / 100, MINIMUM_RATING);
        final int orat = MathUtil.bound(MINIMUM_RATING, rating.rating, MAXIMUM_RATING);

        // compute the K value. Low exp players get to move more quickly.
        final int sessions = rating.experience;
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
        final float pctdiff = ((float)(erat - orat) / orat);

        // update the player's rating
        final int nrat = Math.round(orat + pctdiff * K);

        // make sure the rating remains within a valid range
        rating.rating = MathUtil.bound(MINIMUM_RATING, nrat, MAXIMUM_RATING);
        rating.experience++;
        rating.modified = true;

        log.info("Updated rating", "who", rating.playerName, "orat", orat, "erat", erat,
                 "diff", pctdiff, "K", K, "nrat", nrat);
    }

    protected void awardFlow (final IntMap<Player> players, final int payoutType)
    {
        if (players.size() == 0) { // sanity check
            return;
        }

        switch (payoutType) {
        case WhirledGameObject.WINNERS_TAKE_ALL: {
            // scale all available flow (even losers?) by their performance
            scaleAvailableFlowToPercentiles(players);

            // map the players by score
            final TreeMultimap<Integer,Player> rankings = Multimaps.newTreeMultimap();
            for (final Player player : players.values()) {
                rankings.put(player.score, player);
            }

            // all the losers contribute their flow to a pool
            final int highestScore = rankings.keySet().last();
            int totalLoserFlow = 0;
            for (final Integer score : rankings.keySet()) {
                if (score != highestScore) {
                    for (final Player player : rankings.get(score)) {
                        totalLoserFlow += player.availFlow;
                    }
                }
            }

            // and the winners divide it up evenly
            final int winners = rankings.get(highestScore).size();
            for (final Player player : rankings.get(highestScore)) {
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
            for (final Player player : players.values()) {
                totalScores += player.score;
            }
            final float averageScore = totalScores / (float)players.size();

            // everyone below the average contributes 50% to the pool
            int totalLoserFlow = 0;
            for (final Player player : players.values()) {
                if (player.score < averageScore) {
                    player.flowAward = player.availFlow / 2;
                    totalLoserFlow += (player.availFlow - player.flowAward);
                }
            }

            // everyone at or above the average divides up the pool proportionally
            int totalAboveAverageScores = 0;
            for (final Player player : players.values()) {
                if (player.score >= averageScore) {
                    totalAboveAverageScores += Math.round(player.score - averageScore);
                }
            }
            for (final Player player : players.values()) {
                if (player.score >= averageScore) {
                    final int share = Math.round(player.score - averageScore);
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
            for (final Player player : players.values()) {
                player.flowAward = player.availFlow;
            }
            break;

        case WhirledGameObject.PROPORTIONAL: {
            int totalFlow = 0;
            int totalScore = 0;
            for (final Player player : players.values()) {
                totalScore += player.score;
                totalFlow += player.availFlow;
            }
            for (final Player player : players.values()) {
                player.flowAward = (int)Math.floor(((float)totalFlow) * player.score / totalScore);
            }
            break;
        } // end: case PROPORTIONAL
        }

        log.info("Awarding flow", "game", where(), "type", payoutType, "to", players.values());

        // finally, award the flow and report it to the player
        final boolean actuallyAward = !_content.game.isDevelopmentVersion();
        for (final Player player : players.values()) {
            final FlowRecord record = _flowRecords.get(player.playerOid);
            if (record == null) {
                continue;
            }

            // update the player's member object on their world server
            if (actuallyAward && player.flowAward > 0) {
                _worldClient.reportFlowAward(record.memberId, player.flowAward);
            }

            // report to the game that this player earned some flow
            final DObject user = _omgr.getObject(player.playerOid);
            if (user != null) {
                final boolean hasCookie = (_plmgr instanceof MsoyGameManager) &&
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
    protected void scaleAvailableFlowToPercentiles (final IntMap<Player> players)
    {
        for (final Player player : players.values()) {
            player.availFlow = (int)Math.ceil(player.availFlow * (player.percentile / 99f));
        }
    }

    /**
     * Returns the average duration for this game in fractional minutes.
     */
    protected float getAverageGameDuration (final int playerSeconds)
    {
        final int avgSeconds = isMultiplayer() ?
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
        final Percentiler tiler = _gameReg.getScoreDistribution(getGameId(), isMultiplayer());
        // if for whatever reason we don't have a score distribution, return a blank one which will
        // result in the default percentile being used
        return (tiler == null) ? new Percentiler() : tiler;
    }

    protected int getPercentile (final Percentiler tiler, final int score)
    {
        return (tiler.getRecordedCount() < MIN_VALID_SCORES) ?
            DEFAULT_PERCENTILE : tiler.getPercentile(score);
    }

    protected int getCappedScore (final Percentiler tiler, final Player player)
    {
        final int range = tiler.getMaxScore() - tiler.getMinScore();
        if (tiler.getRecordedCount() < MIN_VALID_SCORES) {
            return player.score;

        } else if (player.score < tiler.getMinScore() - range/2) {
            log.warning("Capping extremely low score", "game", where(), "player", player.name,
                        "score", player.score, "min", tiler.getMinScore(), "range", range);
            return tiler.getMinScore() - range/2;

        } else if (player.score > tiler.getMaxScore() + range/2) {
            log.warning("Capping extremely high score", "game", where(), "player", player.name,
                        "score", player.score, "max", tiler.getMaxScore(), "range", range);
            return tiler.getMaxScore() + range/2;

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
        final int startStamp = now();
        for (final FlowRecord record : _flowRecords.values()) {
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
        final int endStamp = now();
        for (final FlowRecord record : _flowRecords.values()) {
            record.stopTracking(endStamp);
        }
    }

    /**
     * Reset tracking. Done at the end of a game so that the secondsPlayed is fresh for the next
     * round. This may change if games can reward flow without ending the game.
     */
    protected void resetTracking ()
    {
        for (final FlowRecord record : _flowRecords.values()) {
            record.resetSecondsPlayed();
        }
    }

    protected int getAwardableFlow (final int now, final int playerOid)
    {
        final FlowRecord record = _flowRecords.get(playerOid);
        if (record == null) {
            return 0;
        }
        final int playerSecs = record.getPlayTime(now);
        final float avgMins = getAverageGameDuration(playerSecs);
        final float playerMins = playerSecs/60f;

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

        return Math.round(record.humanity * _flowPerMinute * awardMins);
    }

    protected void payoutPlayer (final int oid)
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
                _content.game.isDevelopmentVersion()) {
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
            @Override
            public boolean invoke () {
                try {
                    _moneyLogic.awardCoins(
                        record.memberId, _content.game.creatorId, 0, _content.game.getIdent(),
                        record.awarded, details, UserAction.PLAYED_GAME);
                    _gameReg.gamePayout(
                        record.memberId, _content.game, record.awarded, record.totalSecondsPlayed);
                } catch (final Exception e) {
                    log.warning("Failed to grant flow", "amount", record.awarded, e);
                }
                return false;
            }
        });
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
        return !_gameInvolvedGuest && !_content.game.isDevelopmentVersion() &&
            super.shouldRateGame();
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
        public MemberName name;

        public int beganStamp;
        public int secondsPlayed;

        /** Tracks total seconds played even after restarts. */
        public int totalSecondsPlayed;

        public int played;
        public int awarded;

        public FlowRecord (final MemberName name, final float humanity) {
            this.humanity = humanity;
            this.memberId = name.getMemberId();
            this.name = name;
        }

        public int getPlayTime (final int now) {
            int secondsOfPlay = secondsPlayed;
            if (beganStamp != 0) {
                secondsOfPlay += (now - beganStamp);
            }
            return secondsOfPlay;
        }

        public void stopTracking (final int endStamp) {
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
        public MemberName name;
        public int playerOid;
        public int score;
        public int availFlow;

        public int percentile;
        public int flowAward;

        public Player (MemberName name, int playerOid, int score, int availFlow) {
            this.name = name;
            this.playerOid = playerOid;
            this.score = score;
            this.availFlow = availFlow;
        }

        public int getMemberId () {
            return (name == null) ? 0 : name.getMemberId();
        }

        public int compareTo (Player other) {
            return Comparators.compare(playerOid, other.playerOid);
        }

        @Override
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
    @Inject protected GameGameRegistry _gameReg;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected MoneyLogic _moneyLogic;

    /** Returns whether or not a {@link Player} is a guest. */
    protected static final Predicate<Player> IS_GUEST = new Predicate<Player>() {
        public boolean apply (Player player) {
            // if we couldn't look up your name, alas we must treat you as a guest
            return (player.name == null) || player.name.isGuest();
        }
    };

    /** Games for which we have no history earn no flow beyond this many minutes. */
    protected static final int MAX_FRESH_GAME_DURATION = 8*60;

    /** We require at least this many data points before we'll consider a percentile distribution
     * to be sufficiently valid that we use it to compute performance. */
    protected static final int MIN_VALID_SCORES = 10;

    /** If we lack a valid or sufficiently large score distribution, we use this performance. */
    protected static final int DEFAULT_PERCENTILE = 50;
}
