//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.CacheInvalidator;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Funcs;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.FullText;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameThumbnail;
import com.threerings.msoy.game.gwt.MochiGameInfo;

import static com.threerings.msoy.Log.log;

/**
 * Handles the persistence side of everything about games.
 */
@Singleton
public class MsoyGameRepository extends DepotRepository
{
    /** Game logs for in development games will be purged after this many days when
     * {@link #purgeTraceLogs()} is called. */
    public static final int DAYS_TO_KEEP_DEV_GAME_LOGS = 2;

    /** Game logs for listed development games will be purged after this many days when
     * {@link #purgeTraceLogs()} is called. */
    public static final int DAYS_TO_KEEP_LISTED_GAME_LOGS = 7;

    /** Used by {@link #loadGenreCounts}. */
    @Entity @Computed(shadowOf=GameInfoRecord.class)
    public static class GenreCountRecord extends PersistentRecord
    {
        /** The genre in question. */
        public GameGenre genre;

        /** The number of games in that genre .*/
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    @Inject public MsoyGameRepository (PersistenceContext ctx)
    {
        super(ctx);

        _ratingRepo = new RatingRepository(
            ctx, GameInfoRecord.GAME_ID, GameInfoRecord.RATING_SUM, GameInfoRecord.RATING_COUNT) {
            @Override protected Class<? extends PersistentRecord> getTargetClass () {
                return GameInfoRecord.class;
            }
            @Override protected Class<RatingRecord> getRatingClass () {
                return coerceRating(MsoyGameRatingRecord.class);
            }
        };
    }

    /**
     * Returns the repository used to manage game ratings.
     */
    public RatingRepository getRatingRepository ()
    {
        return _ratingRepo;
    }

    /**
     * Loads up the detail records for all games in the specified set that are published. The game
     * records will be returned in arbitrary order.
     */
    public List<GameInfoRecord> loadPublishedGames (Collection<Integer> gameIds)
    {
        if (gameIds.isEmpty()) {
            return Collections.emptyList();
        }
        return findAll(
            GameInfoRecord.class,
            new Where(Ops.and(GameInfoRecord.GAME_ID.in(gameIds),
                              GameInfoRecord.GENRE.notEq(GameGenre.HIDDEN))));
    }

    /**
     * Loads the count of how many published, integrated games we have in each genre.
     */
    public Map<GameGenre, Integer> loadGenreCounts ()
    {
        Map<GameGenre, Integer> counts = Maps.newHashMap();
        for (GenreCountRecord gcr : findAll(
                 GenreCountRecord.class,
                 new Where(Ops.and(GameInfoRecord.GENRE.notEq(GameGenre.HIDDEN),
                                   GameInfoRecord.INTEGRATED.eq(Boolean.TRUE))),
                 new GroupBy(GameInfoRecord.GENRE))) {
            counts.put(gcr.genre, gcr.count);
        }
        return counts;
    }

    /**
     * Loads all listed game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or ALL to load all games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     */
    public List<GameInfoRecord> loadGenre (GameGenre genre, int limit)
    {
        return loadGenre(genre, limit, null);
    }

    /**
     * Loads all game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or ALL to load all published games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     * @param search string to search for in the title, tags and description
     */
    public List<GameInfoRecord> loadGenre (GameGenre genre, int limit, String search)
    {
        List<QueryClause> clauses = Lists.newArrayList();

        // build the where clause with genre and/or search string
        List<SQLExpression> whereBits = Lists.newArrayList();
        if (genre == GameGenre.ALL || genre == GameGenre.HIDDEN) {
            whereBits.add(GameInfoRecord.GENRE.notEq(GameGenre.HIDDEN));
        } else {
            whereBits.add(GameInfoRecord.GENRE.eq(genre));
        }

        if (!StringUtil.isBlank(search)) {
            FullText fts = new FullText(GameInfoRecord.class, GameInfoRecord.FTS_ND, search);
            whereBits.add(fts.match());
            clauses.add(OrderBy.descending(fts.rank()));

        } else {
            // if we have no search ranking, sort by descending average rating
            SQLExpression count =  Funcs.greatest(GameInfoRecord.RATING_COUNT, Exps.value(1.0));
            clauses.add(OrderBy.descending(GameInfoRecord.RATING_SUM.div(count)));
        }

        // filter out games that aren't integrated with Whirled
        whereBits.add(GameInfoRecord.INTEGRATED.eq(Boolean.TRUE));
        clauses.add(new Where(Ops.and(whereBits)));

        // add the limit if specified
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }

        return findAll(GameInfoRecord.class, clauses);
    }

    /**
     * Load the latest mochi games.
     */
    public Collection<MochiGameInfo> loadLatestMochiGames (int count)
    {
        return findAll(MochiGameInfoRecord.class,
            OrderBy.descending(MochiGameInfoRecord.ID), new Limit(0, count))
            .map(MochiGameInfoRecord.TO_INFO);
    }

    /**
     * Add the specified mochi game to the database.
     */
    public void addMochiGame (MochiGameInfo info)
    {
        MochiGameInfoRecord rec = new MochiGameInfoRecord();
        rec.update(info);
        store(rec);
    }

    /**
     * Just puke if we can't find it.
     */
    public MochiGameInfo loadMochiGame (String tag)
    {
        return load(MochiGameInfoRecord.class, new Where(MochiGameInfoRecord.TAG.eq(tag)))
            .toGameInfo();
    }

    /**
     * Loads the entries for the given arcade portal.
     */
    public List<ArcadeEntryRecord> loadArcadeEntries (ArcadeData.Portal portal, boolean useCache)
    {
        CacheStrategy cache = useCache ? CacheStrategy.BEST : CacheStrategy.NONE;
        return findAll(ArcadeEntryRecord.class, cache, new Where(ArcadeEntryRecord.PORTAL, portal),
            OrderBy.ascending(ArcadeEntryRecord.ORDER));
    }

    /**
     * Adds a new game to the arcade entries for the given portal.
     */
    public void addArcadeEntry (ArcadeData.Portal portal, int gameId, boolean featured)
    {
        ArcadeEntryRecord rec = new ArcadeEntryRecord();
        rec.portal = portal;
        rec.gameId = gameId;
        rec.featured = featured;
        rec.order = Integer.MAX_VALUE;
        insert(rec);
    }

    /**
     * Removes an entry from the given arcade portal.
     */
    public void removeArcadeEntry (ArcadeData.Portal portal, int gameId)
    {
        delete(ArcadeEntryRecord.getKey(portal, gameId));
    }

    /**
     * Sets or clears the featured flag for some entries on the given arcade portal.
     */
    public void updateFeatured (ArcadeData.Portal portal, final Set<Integer> gameIds,
        boolean featured)
    {
        if (gameIds.size() > 0) {
            Where where = new Where(Ops.and(ArcadeEntryRecord.PORTAL.eq(portal),
                ArcadeEntryRecord.GAME_ID.in(gameIds)));
            updatePartial(ArcadeEntryRecord.class, where,
                new CacheInvalidator.TraverseWithFilter<ArcadeEntryRecord>(
                        ArcadeEntryRecord.class) {
                    @Override protected boolean testForEviction (
                        Serializable key, ArcadeEntryRecord record) {
                        return gameIds.contains(record.gameId);
                    }
                }, ArcadeEntryRecord.FEATURED, featured);
        }
    }

    /**
     * Updates the ordering of the entries on the given arcade portal.
     */
    public void updateArcadeEntriesOrder (ArcadeData.Portal portal, List<Integer> entries)
    {
        for (int ii = 0, ll = entries.size(); ii < ll; ++ii) {
            updatePartial(ArcadeEntryRecord.getKey(portal, entries.get(ii)),
                ArcadeEntryRecord.ORDER, ii);
        }
    }

    /**
     * Returns all games by the specified creator (hidden and published).
     */
    public List<GameInfoRecord> loadGamesByCreator (int creatorId)
    {
        return findAll(GameInfoRecord.class, new Where(GameInfoRecord.CREATOR_ID, creatorId));
    }

    /**
     * Returns the {@link GameInfoRecord} for the specified game or null if the id is unknown.
     */
    public GameInfoRecord loadGame (int gameId)
    {
        return load(GameInfoRecord.getKey(Math.abs(gameId)));
    }

    // TEMP
    public Collection<Integer> getAVRGameIds ()
    {
        return findAllKeys(GameInfoRecord.class, false, new Where(GameInfoRecord.IS_AVRG.eq(true)))
            .map(Key.<GameInfoRecord>toInt());
    }

    /**
     * Loads the code record for the specified game. If the id is negative, the development record
     * will be loaded, if positive, the published record will be loaded.
     */
    public GameCode loadGameCode (int gameId, boolean skipCache)
    {
        CacheStrategy cache = skipCache ? CacheStrategy.NONE : CacheStrategy.BEST;
        boolean isDevelopment = (gameId < 0);
        GameCodeRecord code = load(GameCodeRecord.getKey(Math.abs(gameId), isDevelopment), cache);
        return (code != null) ? code.toGameCode() : null;
    }

    /**
     * Loads the metrics record for the specified game.
     */
    public GameMetricsRecord loadGameMetrics (int gameId)
    {
        return load(GameMetricsRecord.getKey(Math.abs(gameId)));
    }

    /**
     * Loads the Facebook info for the specified game. If no info is registered for the game in
     * question a blank record is created with gameId filled in but no key or secret.
     */
    public FacebookInfo loadFacebookInfo (int gameId)
    {
        FacebookInfoRecord info = load(FacebookInfoRecord.getKey(gameId));
        if (info != null) {
            return info.toFacebookInfo();
        }
        FacebookInfo blank = new FacebookInfo();
        blank.gameId = gameId;
        return blank;
    }

    /**
     * Creates or updates the Facebook info for the game referenced by the supplied record.
     */
    public void updateFacebookInfo (FacebookInfo info)
    {
        store(FacebookInfoRecord.fromFacebookInfo(info));
    }

    /**
     * Loads all thumbnails assigned to the given game, sorted by position.
     */
    public List<GameThumbnailRecord> loadAllThumbnails (int gameId)
    {
        return findAll(GameThumbnailRecord.class, new Where(
            GameThumbnailRecord.GAME_ID.eq(gameId)), OrderBy.ascending(GameThumbnailRecord.POS));
    }

    /**
     * Loads all thumbnails assigned to the given game of the given type, sorted by position.
     */
    public List<GameThumbnailRecord> loadThumbnails (GameThumbnail.Type type, int gameId)
    {
        return findAll(GameThumbnailRecord.class, new Where(Ops.and(
            GameThumbnailRecord.GAME_ID.eq(0),GameThumbnailRecord.TYPE.eq(type))),
            OrderBy.ascending(GameThumbnailRecord.POS));
    }

    /**
     * Sets the given thumbnails to be the ones assigned to the given game. I.e. DELETES all
     * current thumbnails for the given game then stores the new ones.
     */
    public void saveThumbnails (int gameId, List<GameThumbnailRecord> thumbs)
    {
        deleteAll(GameThumbnailRecord.class, new Where(GameThumbnailRecord.GAME_ID.eq(gameId)));
        for (GameThumbnailRecord thumb : thumbs) {
            insert(thumb);
        }
    }

    /**
     * Creates a new game using the data in the supplied info record. A GameMetricsRecord is also
     * created for the new game.
     */
    public void createGame (GameInfoRecord info)
    {
        // insert the info record, which will generate a gameId for this game
        insert(info);

        GameMetricsRecord metrics = new GameMetricsRecord();
        metrics.gameId = info.gameId;
        metrics.payoutFactor = GameMetricsRecord.DEFAULT_PAYOUT_FACTOR;
        metrics.flowToNextRecalc = GameMetricsRecord.INITIAL_RECALC_FLOW;
        insert(metrics);
    }

    /**
     * Updates the supplied game info record.
     */
    public void updateGameInfo (GameInfoRecord info)
    {
        update(info);
    }

    /**
     * Updates the supplied game code record.
     */
    public void updateGameCode (GameCodeRecord code)
    {
        code.lastUpdated = new Timestamp(System.currentTimeMillis());
        store(code);
    }

    /**
     * Publishes the supplied game's dev code record to its published code record.
     */
    public void publishGameCode (int gameId)
    {
        GameCodeRecord code = load(GameCodeRecord.class, CacheStrategy.NONE,
                                   GameCodeRecord.getKey(gameId, true));
        if (code != null) {
            code.isDevelopment = false;
            store(code);
        }
    }

    /**
     * Sets the specified game's payout factor to the specified value. This is used for AVRGs which
     * manage their own payout factor.
     */
    public void updatePayoutFactor (int gameId, int newFactor, int flowToNextRecalc)
    {
        updatePartial(GameMetricsRecord.getKey(Math.abs(gameId)),
                      GameMetricsRecord.PAYOUT_FACTOR, newFactor,
                      GameMetricsRecord.FLOW_TO_NEXT_RECALC, flowToNextRecalc);
    }

    /**
     * Records a {@link GamePlayRecord} for this game and updates its {@link GameInfoRecord} to
     * reflect this gameplay.
     */
    public void noteGamePlayed (int gameId, boolean multiPlayer, int playerGames,
                                int playerMins, int flowAwarded)
    {
        // record a gameplay record for this play session
        GamePlayRecord gprec = new GamePlayRecord();
        gprec.gameId = gameId;
        gprec.recorded = new Timestamp(System.currentTimeMillis());
        gprec.multiPlayer = multiPlayer;
        gprec.playerGames = playerGames;
        gprec.playerMins = playerMins;
        gprec.flowAwarded = flowAwarded;
        insert(gprec);

        // update our games played and flow to next recalc in the detail record
        SQLExpression add = GameMetricsRecord.GAMES_PLAYED.plus(playerGames);
        SQLExpression sub = GameMetricsRecord.FLOW_TO_NEXT_RECALC.minus(flowAwarded);
        updatePartial(GameMetricsRecord.getKey(Math.abs(gprec.gameId)),
                      ImmutableMap.of(GameMetricsRecord.GAMES_PLAYED, add,
                                      GameMetricsRecord.FLOW_TO_NEXT_RECALC, sub,
                                      GameMetricsRecord.LAST_PAYOUT, Exps.value(gprec.recorded)));

        // note that this game has API integration
        updatePartial(GameInfoRecord.getKey(gameId),
                      ImmutableMap.of(GameInfoRecord.INTEGRATED, true));
    }

    /**
     * Gets all game plays that occurred between the start and end dates given.
     *
     * @param start Start of the date range, inclusive, in milliseconds since 1/1/1970
     * @param end End of the data range, exclusive, in milliseconds since 1/1/1970
     * @return All found game plays.
     */
    public Collection<GamePlayRecord> getGamePlaysBetween (long start, long end)
    {
        // where recorded >= {start} and recorded < {end}
        Where where = new Where(Ops.and(
            GamePlayRecord.RECORDED.greaterEq(new Timestamp(start)),
            GamePlayRecord.RECORDED.lessThan(new Timestamp(end))
        ));

        return findAll(GamePlayRecord.class, where);
    }

    /**
     * Grinds through this game's recent gameplay data and computes an updated payout factor and
     * new average game durations. Updates the {@link GameMetricsRecord} with those values.
     *
     * @return a triplet of new values for (payoutFactor, avgSingleDuration, avgMultiDuration).
     */
    public int[] computeAndUpdatePayoutFactor (int gameId, int flowToNextRecalc, int hourlyRate)
    {
        // load up all of our extant gameplay records and sum up some bits
        int singlePlayerMins = 0, singlePlayerGames = 0;
        int multiPlayerMins = 0, multiPlayerGames = 0;
        int totalPlayerMins = 0, totalFlowAwarded = 0;
        Where where = new Where(GamePlayRecord.GAME_ID, gameId);
        for (GamePlayRecord gprec : findAll(GamePlayRecord.class, where)) {
            if (gprec.multiPlayer) {
                multiPlayerGames += gprec.playerGames;
                multiPlayerMins += gprec.playerMins;
            } else {
                singlePlayerGames += gprec.playerGames;
                singlePlayerMins += gprec.playerMins;
            }
            totalPlayerMins += gprec.playerMins;
            totalFlowAwarded += gprec.flowAwarded;
        }

        // now compute our new payout factor and average durations
        int avgSingleDuration = (singlePlayerGames == 0) ? 0 :
            60 * singlePlayerMins / singlePlayerGames;
        int avgMultiDuration = (multiPlayerGames == 0) ? 0 :
            60 * multiPlayerMins / multiPlayerGames;

        // our factor is the target hourly payout rate over our actual payout rate (we'll multiply
        // future awards by this factor to scale them to the target hourly rate)
        float awardedPerHour = totalFlowAwarded / (totalPlayerMins/60f);
        float payoutRatio = Math.min(hourlyRate / awardedPerHour, MAX_PAYOUT_ADJUST);
        int payoutFactor = Math.round(payoutRatio * 256);

        log.info("Updating payout factor [game=" + gameId +
                 ", accumMins=" + totalPlayerMins + ", accumFlow=" + totalFlowAwarded +
                 ", aph=" + awardedPerHour + ", payoutRatio=" + payoutRatio + "].");

        // update the detail record
        updatePartial(GameMetricsRecord.getKey(gameId),
                      GameMetricsRecord.PAYOUT_FACTOR, payoutFactor,
                      GameMetricsRecord.FLOW_TO_NEXT_RECALC, flowToNextRecalc,
                      GameMetricsRecord.AVG_SINGLE_DURATION, avgSingleDuration,
                      GameMetricsRecord.AVG_MULTI_DURATION, avgMultiDuration);

        // lastly, prune old gameplay records
        final Timestamp cutoff = new Timestamp(System.currentTimeMillis() - THIRTY_DAYS);
        deleteAll(GamePlayRecord.class,
                  new Where(GamePlayRecord.RECORDED.lessThan(cutoff)));

        return new int[] { payoutFactor, avgSingleDuration, avgMultiDuration };
    }

    /**
     * Returns the instructions for the specified game or null if it has none.
     */
    public String loadInstructions (int gameId)
    {
        InstructionsRecord irec = load(InstructionsRecord.getKey(gameId));
        return (irec == null) ? null : irec.instructions;
    }

    /**
     * Updates the instructions for the specified game.
     */
    public void updateInstructions (int gameId, String instructions)
    {
        if (StringUtil.isBlank(instructions)) {
            delete(InstructionsRecord.getKey(gameId));
        } else {
            InstructionsRecord irec = new InstructionsRecord();
            irec.gameId = gameId;
            irec.instructions = instructions;
            store(irec);
        }
    }

    public GameTraceLogRecord loadTraceLog (int logId)
    {
        return load(GameTraceLogRecord.getKey(logId));
    }

    public List<GameTraceLogEnumerationRecord> enumerateTraceLogs (int gameId)
    {
        return findAll(
            GameTraceLogEnumerationRecord.class,
            new Where(GameTraceLogEnumerationRecord.GAME_ID, gameId),
            new FromOverride(GameTraceLogRecord.class));
    }

    public void storeTraceLog (int gameId, String traceLog)
    {
        insert(new GameTraceLogRecord(gameId, traceLog));
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // delete all game ratings by these members
        _ratingRepo.purgeMembers(memberIds);
    }

    /**
     * Delete all old development and listed logs using the default time limits
     * {@link #DAYS_TO_KEEP_DEV_GAME_LOGS} and {@link #DAYS_TO_KEEP_LISTED_GAME_LOGS}.
     */
    public void purgeTraceLogs ()
    {
        purgeTraceLogs(DAYS_TO_KEEP_DEV_GAME_LOGS, DAYS_TO_KEEP_LISTED_GAME_LOGS);
    }

    /**
     * Delete all development and listed game logs that are older than the given respective number
     * of days.
     */
    public void purgeTraceLogs (int developmentDaysToKeep, int listedDaysToKeep)
    {
        // Round down to the current minute for easier reading by humans
        GregorianCalendar now = new GregorianCalendar();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        // Subtract the cutoff days for dev
        GregorianCalendar devCutoff = new GregorianCalendar();
        devCutoff.setTimeInMillis(now.getTimeInMillis());
        devCutoff.add(Calendar.DAY_OF_YEAR, -developmentDaysToKeep);

        // Subtract the cutoff days for listed
        GregorianCalendar listedCutoff = new GregorianCalendar();
        listedCutoff.setTimeInMillis(now.getTimeInMillis());
        listedCutoff.add(Calendar.DAY_OF_YEAR, -listedDaysToKeep);

        // Convert to time stamps
        Timestamp devCutoffTimestamp = new Timestamp(devCutoff.getTimeInMillis());
        Timestamp listedCutoffTimestamp = new Timestamp(listedCutoff.getTimeInMillis());

        // Create conditionals for distinguishing dev from listed games
        ColumnExp gameId = GameTraceLogRecord.GAME_ID;
        SQLExpression isDev = gameId.lessThan(0);
        SQLExpression isListed = gameId.greaterThan(0);

        // Perform deletion
        ColumnExp recorded = GameTraceLogRecord.RECORDED;
        int rows = deleteAll(GameTraceLogRecord.class, new Where(Ops.or(
            Ops.and(isDev, recorded.lessThan(devCutoffTimestamp)),
            Ops.and(isListed, recorded.lessThan(listedCutoffTimestamp)))));

        log.info("Deleted trace logs", "devCutoff", devCutoffTimestamp,
                 "listedCutoff", listedCutoffTimestamp, "rows", rows);
    }

    /**
     * Deletes all information associated with the supplied game. This only handles data managed by
     * this repository. See GameServlet.deleteGame for the full gamut of things deleted when a game
     * is deleted.
     */
    public void deleteGame (int gameId)
    {
        deleteAll(ArcadeEntryRecord.class, new Where(ArcadeEntryRecord.GAME_ID, gameId), null);
        delete(FacebookInfoRecord.getKey(gameId));
        delete(GameCodeRecord.getKey(gameId, true));
        delete(GameCodeRecord.getKey(gameId, false));
        delete(GameInfoRecord.getKey(gameId));
        delete(GameMetricsRecord.getKey(gameId));
        deleteAll(GamePlayRecord.class, new Where(GamePlayRecord.GAME_ID, gameId), null);
        int devGameId = GameInfo.toDevId(gameId);
        deleteAll(GameTraceLogRecord.class, new Where(GameTraceLogRecord.GAME_ID, gameId), null);
        deleteAll(GameTraceLogRecord.class, new Where(GameTraceLogRecord.GAME_ID, devGameId), null);
        delete(InstructionsRecord.getKey(gameId));
        _ratingRepo.deleteRatings(gameId);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ArcadeEntryRecord.class);
        classes.add(FacebookInfoRecord.class);
        classes.add(GameCodeRecord.class);
        classes.add(GameInfoRecord.class);
        classes.add(GameMetricsRecord.class);
        classes.add(GamePlayRecord.class);
        classes.add(GameThumbnailRecord.class);
        classes.add(GameTraceLogRecord.class);
        classes.add(InstructionsRecord.class);
        classes.add(MochiGameInfoRecord.class);
    }

    protected RatingRepository _ratingRepo;

    /** We will not adjust a game's payout higher than 2x to bring it in line with our desired
     * payout rates to avoid potential abuse. Games that consistently award very low amounts can
     * fix their scoring algorithms. */
    protected static final float MAX_PAYOUT_ADJUST = 2f;

    /** Thirty (average) days in milliseconds. */
    protected static final long THIRTY_DAYS = 30*24*60*60*1000L;
}
