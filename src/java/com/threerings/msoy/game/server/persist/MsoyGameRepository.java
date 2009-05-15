//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DepotRepository;
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
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.Arithmetic;
import com.samskivert.depot.operator.Conditionals.*;
import com.samskivert.depot.operator.Logic.*;
import com.samskivert.depot.operator.SQLOperator;

import com.threerings.msoy.comment.server.persist.CommentRepository;
import com.threerings.msoy.item.server.persist.GameRatingRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.LevelPackRepository;
import com.threerings.msoy.item.server.persist.PrizeRepository;
import com.threerings.msoy.item.server.persist.PropRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;

import com.threerings.msoy.game.data.all.GameGenre;
import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.game.gwt.GameInfo;

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
        public byte genre;

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

        registerMigration(new DataMigration("2009_05_12_gameasaurus") {
            @Override public void invoke () {
                // create info records for all of our extant detail records
                int migrated = 0;
                IntIntMap suiteMigs = new IntIntMap();
                for (GameDetailRecord drec : findAll(GameDetailRecord.class, CacheStrategy.NONE,
                                                     Lists.<QueryClause>newArrayList())) {
                    GameRecord sgrec = _gameRepo.loadItem(drec.sourceItemId);
                    GameRecord lgrec = _gameRepo.loadItem(drec.listedItemId);
                    if (sgrec == null) {
                        log.warning("Missing source record, cannot migrate", "game", drec.gameId);
                        continue;
                    }
                    GameRecord grec = (lgrec == null) ? sgrec : lgrec;

                    GameInfoRecord irec = new GameInfoRecord();
                    irec.gameId = drec.gameId;
                    irec.name = StringUtil.truncate(grec.name, GameInfo.MAX_NAME_LENGTH);
                    irec.genre = grec.genre;
                    irec.isAVRG = GameCode.detectIsInWorld(grec.config);
                    irec.creatorId = grec.creatorId;
                    irec.description = StringUtil.truncate(
                        grec.description, GameInfo.MAX_DESCRIPTION_LENGTH);
                    irec.thumbMediaHash = grec.thumbMediaHash;
                    irec.thumbMimeType = grec.thumbMimeType;
                    irec.thumbConstraint = grec.thumbConstraint;
                    irec.shotMediaHash = grec.shotMediaHash;
                    irec.shotMimeType = grec.shotMimeType;
                    irec.groupId = grec.groupId;
                    irec.shopTag = grec.shopTag;
                    irec.ratingSum = grec.ratingSum;
                    irec.ratingCount = grec.ratingCount;
                    irec.integrated = (drec.lastPayout != null);
                    store(irec);

                    GameMetricsRecord mrec = new GameMetricsRecord();
                    mrec.gameId = drec.gameId;
                    mrec.gamesPlayed = drec.gamesPlayed;
                    mrec.avgSingleDuration = drec.avgSingleDuration;
                    mrec.avgMultiDuration = drec.avgMultiDuration;
                    mrec.payoutFactor = drec.payoutFactor;
                    mrec.flowToNextRecalc = drec.flowToNextRecalc;
                    mrec.lastPayout = drec.lastPayout;
                    store(mrec);

                    GameCodeRecord screc = new GameCodeRecord();
                    screc.gameId = drec.gameId;
                    screc.isDevelopment = true;
                    screc.config = sgrec.config;
                    screc.clientMediaHash = sgrec.gameMediaHash;
                    screc.clientMimeType = sgrec.gameMimeType;
                    screc.serverMediaHash = sgrec.serverMediaHash;
                    screc.serverMimeType = sgrec.serverMimeType;
                    screc.splashMediaHash = sgrec.splashMediaHash;
                    screc.splashMimeType = sgrec.splashMimeType;
                    screc.lastUpdated = sgrec.lastTouched;
                    store(screc);
                    suiteMigs.put(sgrec.itemId, -drec.gameId);

                    if (lgrec != null) {
                        GameCodeRecord lcrec = new GameCodeRecord();
                        lcrec.gameId = drec.gameId;
                        lcrec.isDevelopment = false;
                        lcrec.config = lgrec.config;
                        lcrec.clientMediaHash = lgrec.gameMediaHash;
                        lcrec.clientMimeType = lgrec.gameMimeType;
                        lcrec.serverMediaHash = lgrec.serverMediaHash;
                        lcrec.serverMimeType = lgrec.serverMimeType;
                        lcrec.splashMediaHash = lgrec.splashMediaHash;
                        lcrec.splashMimeType = lgrec.splashMimeType;
                        lcrec.lastUpdated = lgrec.lastTouched;
                        store(lcrec);
                        suiteMigs.put(-lgrec.catalogId, drec.gameId);
                    }

                    migrated++;
                }
                log.info("Migrated " + migrated + " games to new Whirled order.");

                // pass the buck to the various repositories to migrate the suites
                _lpackRepo.migrateSuites(suiteMigs);
                _ipackRepo.migrateSuites(suiteMigs);
                _tsourceRepo.migrateSuites(suiteMigs);
                _prizeRepo.migrateSuites(suiteMigs);
                _propRepo.migrateSuites(suiteMigs);
            }
        });

        registerMigration(new DataMigration("2009_05_12_gamecomments") {
            @Override public void invoke () {
                IntIntMap idmap = new IntIntMap();
                Where where = new Where(new And(new GreaterThan(GameRecord.GAME_ID, 0),
                                                new NotEquals(GameRecord.CATALOG_ID, 0)));
                for (GameRecord game : findAll(GameRecord.class, where)) {
                    idmap.put(game.catalogId, game.gameId);
                }
                _commentRepo.migrateGameComments(idmap);
            }
        });

        registerMigration(new DataMigration("2009_05_12_gameratings") {
            @Override public void invoke () {
                IntIntMap idMap = new IntIntMap();
                for (GameDetailRecord drec : findAll(GameDetailRecord.class, CacheStrategy.NONE,
                                                     Lists.<QueryClause>newArrayList())) {
                    idMap.put(drec.listedItemId, drec.gameId);
                }
                for (GameRatingRecord grr : findAll(GameRatingRecord.class, CacheStrategy.NONE,
                                                    Lists.<QueryClause>newArrayList())) {
                    MsoyGameRatingRecord mgrr = new MsoyGameRatingRecord();
                    if (!idMap.containsKey(grr.targetId)) {
                        log.warning("Missing mapping for rating record", "itemId", grr.targetId);
                    } else {
                        mgrr.targetId = idMap.get(grr.targetId);
                        mgrr.memberId = grr.memberId;
                        mgrr.rating = grr.rating;
                        store(mgrr);
                    }
                }
            }
        });
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
        return findAll(
            GameInfoRecord.class,
            new Where(new And(new In(GameInfoRecord.GAME_ID, gameIds),
                              new NotEquals(GameInfoRecord.GENRE, GameGenre.HIDDEN))));
    }

    /**
     * Loads the count of how many published, integrated games we have in each genre.
     */
    public IntIntMap loadGenreCounts ()
    {
        IntIntMap counts = new IntIntMap();
        for (GenreCountRecord gcr : findAll(
                 GenreCountRecord.class,
                 new Where(new And(new NotEquals(GameInfoRecord.GENRE, GameGenre.HIDDEN),
                                   new Equals(GameInfoRecord.INTEGRATED, Boolean.TRUE))),
                 new GroupBy(GameInfoRecord.GENRE))) {
            counts.put(gcr.genre, gcr.count);
        }
        return counts;
    }

    /**
     * Loads all listed game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or {@link GameGenre#ALL} to load all games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     */
    public List<GameInfoRecord> loadGenre (byte genre, int limit)
    {
        return loadGenre(genre, limit, null);
    }

    /**
     * Loads all game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or -1 to load all published games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     * @param search string to search for in the title, tags and description
     */
    public List<GameInfoRecord> loadGenre (byte genre, int limit, String search)
    {
        List<QueryClause> clauses = Lists.newArrayList();

        // build the where clause with genre and/or search string
        List<SQLOperator> whereBits = Lists.newArrayList();
        if (genre < 0) { // ALL == -1 but we want to avoid showing HIDDEN always
            whereBits.add(new NotEquals(GameInfoRecord.GENRE, GameGenre.HIDDEN));
        } else {
            whereBits.add(new Equals(GameInfoRecord.GENRE, genre));
        }

        if (!StringUtil.isBlank(search)) {
// TODO: search name and description
//             whereBits.add(buildSearchClause(new WordSearch(search)));
        }

        // filter out games that aren't integrated with Whirled
        whereBits.add(new Equals(GameInfoRecord.INTEGRATED, Boolean.TRUE));
        clauses.add(new Where(new And(whereBits)));

        // sort by descending average rating
        FunctionExp count = new FunctionExp(
            "GREATEST", GameInfoRecord.RATING_COUNT, new ValueExp(1.0));
        clauses.add(OrderBy.descending(new Arithmetic.Div(GameInfoRecord.RATING_SUM, count)));

        // add the limit if specified
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }

        return findAll(GameInfoRecord.class, clauses);
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
        return load(GameInfoRecord.class, Math.abs(gameId));
    }

    /**
     * Loads the code record for the specified game. If the id is negative, the development record
     * will be loaded, if positive, the published record will be loaded. Returns a blank code
     * record if the game has no record of the requested type.
     */
    public GameCodeRecord loadGameCode (int gameId, boolean skipCache)
    {
        CacheStrategy cache = skipCache ? CacheStrategy.NONE : CacheStrategy.BEST;
        boolean isDevelopment = gameId < 0 ? true : false;
        GameCodeRecord code = load(GameCodeRecord.class, cache,
                                   GameCodeRecord.getKey(Math.abs(gameId), isDevelopment));
        return (code == null) ? GameCodeRecord.createBlank(gameId, isDevelopment) : code;
    }

    /**
     * Loads the metrics record for the specified game.
     */
    public GameMetricsRecord loadGameMetrics (int gameId)
    {
        return load(GameMetricsRecord.class, Math.abs(gameId));
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
     * Sets the specified game's payout factor to the specified value. This is used for AVRGs which
     * manage their own payout factor.
     */
    public void updatePayoutFactor (int gameId, int newFactor, int flowToNextRecalc)
    {
        updatePartial(GameMetricsRecord.class, Math.abs(gameId),
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
        SQLExpression add = new Arithmetic.Add(GameMetricsRecord.GAMES_PLAYED, playerGames);
        SQLExpression sub = new Arithmetic.Sub(GameMetricsRecord.FLOW_TO_NEXT_RECALC, flowAwarded);
        updateLiteral(GameMetricsRecord.class, Math.abs(gprec.gameId),
                      ImmutableMap.of(GameMetricsRecord.GAMES_PLAYED, add,
                                      GameMetricsRecord.FLOW_TO_NEXT_RECALC, sub,
                                      GameMetricsRecord.LAST_PAYOUT, new ValueExp(gprec.recorded)));

        // TODO: update GameInfoRecord.INTEGRATED
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
        Where where = new Where(new And(
            new GreaterThanEquals(GamePlayRecord.RECORDED, new Timestamp(start)),
            new LessThan(GamePlayRecord.RECORDED, new Timestamp(end))
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
        updatePartial(GameMetricsRecord.class, gameId,
                      GameMetricsRecord.PAYOUT_FACTOR, payoutFactor,
                      GameMetricsRecord.FLOW_TO_NEXT_RECALC, flowToNextRecalc,
                      GameMetricsRecord.AVG_SINGLE_DURATION, avgSingleDuration,
                      GameMetricsRecord.AVG_MULTI_DURATION, avgMultiDuration);

        // lastly, prune old gameplay records
        final Timestamp cutoff = new Timestamp(System.currentTimeMillis() - THIRTY_DAYS);
        deleteAll(GamePlayRecord.class,
                  new Where(new LessThan(GamePlayRecord.RECORDED, cutoff)));

        return new int[] { payoutFactor, avgSingleDuration, avgMultiDuration };
    }

    /**
     * Returns the instructions for the specified game or null if it has none.
     */
    public String loadInstructions (int gameId)
    {
        InstructionsRecord irec = load(InstructionsRecord.class, gameId);
        return (irec == null) ? null : irec.instructions;
    }

    /**
     * Updates the instructions for the specified game.
     */
    public void updateInstructions (int gameId, String instructions)
    {
        if (StringUtil.isBlank(instructions)) {
            delete(InstructionsRecord.class, gameId);
        } else {
            InstructionsRecord irec = new InstructionsRecord();
            irec.gameId = gameId;
            irec.instructions = instructions;
            store(irec);
        }
    }

    public GameTraceLogRecord loadTraceLog (int logId)
    {
        return load(GameTraceLogRecord.class, logId);
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
        LessThan isDev = new LessThan(gameId, 0);
        GreaterThan isListed = new GreaterThan(gameId, 0);

        // Perform deletion
        ColumnExp recorded = GameTraceLogRecord.RECORDED;
        int rows = deleteAll(GameTraceLogRecord.class, new Where(new Or(
            new And(isDev, new LessThan(recorded, devCutoffTimestamp)),
            new And(isListed, new LessThan(recorded, listedCutoffTimestamp)))));

        log.info("Deleted trace logs", "devCutoff", devCutoffTimestamp,
                 "listedCutoff", listedCutoffTimestamp, "rows", rows);
    }

    /**
     * Deletes all ephemeral data associated with the specified game.
     */
    protected void purgeGame (int gameId)
    {
        delete(GameInfoRecord.class, gameId);
        delete(InstructionsRecord.class, gameId);
        deleteAll(GamePlayRecord.class, new Where(GamePlayRecord.GAME_ID, gameId));
        deleteAll(GameTraceLogRecord.class, new Where(GameTraceLogRecord.GAME_ID, gameId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GameInfoRecord.class);
        classes.add(GameCodeRecord.class);
        classes.add(GameMetricsRecord.class);
        classes.add(GamePlayRecord.class);
        classes.add(InstructionsRecord.class);
        classes.add(GameTraceLogRecord.class);
    }

    protected RatingRepository _ratingRepo;

    // TEMP
    @Inject protected GameRepository _gameRepo;
    @Inject protected TrophySourceRepository _tsourceRepo;
    @Inject protected LevelPackRepository _lpackRepo;
    @Inject protected ItemPackRepository _ipackRepo;
    @Inject protected PrizeRepository _prizeRepo;
    @Inject protected PropRepository _propRepo;
    @Inject protected CommentRepository _commentRepo;
    // END TEMP

    /** We will not adjust a game's payout higher than 2x to bring it in line with our desired
     * payout rates to avoid potential abuse. Games that consistently award very low amounts can
     * fix their scoring algorithms. */
    protected static final float MAX_PAYOUT_ADJUST = 2f;

    /** Thirty (average) days in milliseconds. */
    protected static final long THIRTY_DAYS = 30*24*60*60*1000L;
}
