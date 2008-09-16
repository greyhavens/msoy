//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.samskivert.jdbc.depot.operator.Logic.Or;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link Game} items.
 */
@Singleton
public class GameRepository extends ItemRepository<GameRecord>
{
    /** Game logs for in development games will be purged after this many days when
     * {@link #purgeTraceLogs()} is called. */
    public static final int DAYS_TO_KEEP_DEV_GAME_LOGS = 2;
    
    /** Game logs for listed development games will be purged after this many days when
     * {@link #purgeTraceLogs()} is called. */
    public static final int DAYS_TO_KEEP_LISTED_GAME_LOGS = 7;

    @Entity(name="GameTagRecord")
    public static class GameTagRecord extends TagRecord
    {
    }

    @Entity(name="GameTagHistoryRecord")
    public static class GameTagHistoryRecord extends TagHistoryRecord
    {
    }

    /** Used by {@link #loadGenreCounts}. */
    @Entity @Computed(shadowOf=GameRecord.class)
    public static class GenreCountRecord extends PersistentRecord
    {
        /** The genre in question. */
        public byte genre;

        /** The number of games in that genre .*/
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    @Inject public GameRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Returns the total number of listed games in the repository.
     */
    public int getGameCount ()
    {
        Where where = new Where(new Conditionals.NotEquals(GameDetailRecord.LISTED_ITEM_ID_C, 0));
        return load(CountRecord.class, new FromOverride(GameDetailRecord.class), where).count;
    }

    /**
     * Loads the appropriate {@link GameRecord} for the specified game id. If the id is negative,
     * the source item record will be loaded, if positive, the listed item record will be loaded.
     * May return null if the game id is unknown or no valid listed or source item record is
     * available.
     */
    public GameRecord loadGameRecord (int gameId)
    {
        return loadGameRecord(gameId, loadGameDetail(gameId));
    }

    /**
     * Returns the {@link GameDetailRecord} for the specified game or null if the id is unknown.
     */
    public GameDetailRecord loadGameDetail (int gameId)
    {
        return load(GameDetailRecord.class, Math.abs(gameId));
    }

    /**
     * Loads the appropriate {@link GameRecord} for the specified game detail.
     */
    public GameRecord loadGameRecord (int gameId, GameDetailRecord gdr)
    {
        if (gdr == null) {
            return null;
        }
        return loadItem(GameRecord.isDeveloperVersion(gameId) ? gdr.sourceItemId : gdr.listedItemId);
    }

    /**
     * Loads the count of how many listed games we have in each genre.
     */
    public IntIntMap loadGenreCounts ()
    {
        IntIntMap counts = new IntIntMap();
        for (GenreCountRecord gcr : findAll(
                 GenreCountRecord.class, new Join(getItemClass(), ItemRecord.ITEM_ID,
                                                  getCatalogClass(), CatalogRecord.LISTED_ITEM_ID),
                 new GroupBy(getItemColumn(GameRecord.GENRE)))) {
            counts.put(gcr.genre, gcr.count);
        }
        return counts;
    }

    /**
     * Loads all listed game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or -1 to load all (listed) games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     */
    public List<GameRecord> loadGenre (byte genre, int limit)
    {
        return loadGenre(genre, limit, null);
    }

    /**
     * Loads all listed game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or -1 to load all (listed) games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     * @param searchQuery string to search for in the title, tags and description
     */
    public List<GameRecord> loadGenre (byte genre, int limit, String searchQuery)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Join(getItemClass(), ItemRecord.ITEM_ID,
                             getCatalogClass(), CatalogRecord.LISTED_ITEM_ID));
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }

        // sort out the primary and secondary order by clauses
        List<SQLExpression> obExprs = Lists.newArrayList();
        List<OrderBy.Order> obOrders = Lists.newArrayList();
        addOrderByRating(obExprs, obOrders);
        clauses.add(new OrderBy(obExprs.toArray(new SQLExpression[obExprs.size()]),
                                obOrders.toArray(new OrderBy.Order[obOrders.size()])));

        // build the where clause with genre and/or search string
        List<SQLOperator> whereBits = Lists.newArrayList();
        if (genre >= 0) {
            whereBits.add(new Conditionals.Equals(GameRecord.GENRE_C, genre));
        }
        if (searchQuery != null && searchQuery.length() > 0) {
            whereBits.add(buildSearchStringClause(searchQuery));
        }
        if (whereBits.size() > 0) {
            clauses.add(new Where(new And(whereBits)));
        }

        // finally fetch all the game records of interest
        return findAll(getItemClass(), clauses);
    }

    /**
     * Updates the specified {@link GameDetailRecord}, recording an increase in games played and a
     * decrease in flow to next recalc.
     */
    public void noteGamePlayed (int gameId, int playerGames, int flowAwarded)
    {
        SQLExpression add = new Arithmetic.Add(GameDetailRecord.GAMES_PLAYED_C, playerGames);
        SQLExpression sub = new Arithmetic.Sub(GameDetailRecord.FLOW_TO_NEXT_RECALC_C, flowAwarded);
        updateLiteral(GameDetailRecord.class, Math.abs(gameId),
                    ImmutableMap.of(GameDetailRecord.GAMES_PLAYED, add,
                                        GameDetailRecord.FLOW_TO_NEXT_RECALC, sub));
    }

    /**
     * Sets the specified game's payout factor to the specified value. This is used for AVRGs which
     * manage their own payout factor.
     */
    public void updatePayoutFactor (int gameId, int newFactor, int flowToNextRecalc)
    {
        updatePartial(GameDetailRecord.class, Math.abs(gameId),
                      GameDetailRecord.PAYOUT_FACTOR, newFactor,
                      GameDetailRecord.FLOW_TO_NEXT_RECALC, flowToNextRecalc);
    }

    /**
     * Records a {@link GamePlayRecord} for this game and updates its {@link GameDetailRecord} to
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
        noteGamePlayed(gameId, playerGames, flowAwarded);
    }

    /**
     * Grinds through this game's recent gameplay data and computes an updated payout factor and
     * new average game durations. Updates the {@link GameDetailRecord} with those values.
     *
     * @return a triplet of new values for (payoutFactor, avgSingleDuration, avgMultiDuration).
     */
    public int[] computeAndUpdatePayoutFactor (int gameId, int flowToNextRecalc, int hourlyRate)
    {
        // load up all of our extant gameplay records and sum up some bits
        int singlePlayerMins = 0, singlePlayerGames = 0;
        int multiPlayerMins = 0, multiPlayerGames = 0;
        int totalPlayerMins = 0, totalFlowAwarded = 0;
        Where where = new Where(GamePlayRecord.GAME_ID_C, gameId);
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
        updatePartial(GameDetailRecord.class, gameId,
                      GameDetailRecord.PAYOUT_FACTOR, payoutFactor,
                      GameDetailRecord.FLOW_TO_NEXT_RECALC, flowToNextRecalc,
                      GameDetailRecord.AVG_SINGLE_DURATION, avgSingleDuration,
                      GameDetailRecord.AVG_MULTI_DURATION, avgMultiDuration);

        // lastly, prune old gameplay records
        final Timestamp cutoff = new Timestamp(System.currentTimeMillis() - THIRTY_DAYS);
        deleteAll(GamePlayRecord.class,
                  new Where(new Conditionals.LessThan(GamePlayRecord.RECORDED_C, cutoff)));

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
            new Where(GameTraceLogEnumerationRecord.GAME_ID_C, gameId),
            new FromOverride(GameTraceLogRecord.class));
    }

    public void storeTraceLog (int gameId, String traceLog)
    {
        insert(new GameTraceLogRecord(gameId, traceLog));
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
        ColumnExp gameId = GameTraceLogRecord.GAME_ID_C;
        Conditionals.LessThan isDev = new Conditionals.LessThan(gameId, 0);
        Conditionals.GreaterThan isListed = new Conditionals.GreaterThan(gameId, 0);

        // Perform deletion
        ColumnExp recorded = GameTraceLogRecord.RECORDED_C;
        int rows = deleteAll(GameTraceLogRecord.class, new Where(new Or(
            new And(isDev, new Conditionals.LessThan(recorded, devCutoffTimestamp)),
            new And(isListed, new Conditionals.LessThan(recorded, listedCutoffTimestamp)))));

        log.info(
            "Deleted trace logs", "devCutoff", devCutoffTimestamp, "listedCutoff", 
            listedCutoffTimestamp, "rows", rows);
    }

    @Override // from ItemRepository
    public void insertOriginalItem (GameRecord item, boolean catalogListing)
    {
        super.insertOriginalItem(item, catalogListing);

        // sanity check
        if (catalogListing && item.gameId == 0) {
            log.warning("Listing game with no assigned game id " + item + ".");
        }

        // if this item did not yet have a game id, create a new game detail record and wire it up
        if (item.gameId == 0) {
            GameDetailRecord gdr = new GameDetailRecord();
            gdr.sourceItemId = item.itemId;
            gdr.payoutFactor = GameDetailRecord.DEFAULT_PAYOUT_FACTOR;
            gdr.flowToNextRecalc = GameDetailRecord.INITIAL_RECALC_FLOW;
            insert(gdr);
            // source games use -gameId to differentiate themselves from all non-source games
            updatePartial(getItemClass(), item.itemId, GameRecord.GAME_ID, -gdr.gameId);
            // fill the game id back into the newly created game record
            item.gameId = -gdr.gameId;

        } else if (catalogListing) {
            updatePartial(GameDetailRecord.class, item.gameId,
                          GameDetailRecord.LISTED_ITEM_ID, item.itemId);
        }
    }

    @Override // from ItemRepository
    public void deleteItem (int itemId)
    {
        // if we're deleting an original item; we need to potentially delete or update its
        // associated game detail record
        GameDetailRecord gdr = null;
        if (itemId > 0) {
            GameRecord item = load(getItemClass(), itemId);
            if (item != null && item.gameId != 0) {
                gdr = load(GameDetailRecord.class, Math.abs(item.gameId));
            }
            if (gdr != null) {
                if (gdr.sourceItemId == itemId) {
                    gdr.sourceItemId = 0;
                    if (gdr.listedItemId == 0) {
                        gameDeleted(gdr.gameId);
                    } else {
                        update(gdr);
                    }
                }
                // this should never happen as catalog originals are not (currently) deleted
                if (gdr.listedItemId == itemId) {
                    log.warning("Deleting listed item for game?! " + gdr + ".");
                }
            }
        }

        // now go ahead and do the standard item deletion
        super.deleteItem(itemId);
    }

    /**
     * Deletes all ephemeral data associated with the specified game.
     */
    protected void gameDeleted (int gameId)
    {
        delete(GameDetailRecord.class, gameId);
        delete(InstructionsRecord.class, gameId);
        deleteAll(GamePlayRecord.class, new Where(GamePlayRecord.GAME_ID_C, gameId));
        deleteAll(GameTraceLogRecord.class, new Where(GameTraceLogRecord.GAME_ID_C, gameId));
    }

    @Override // from ItemRepository
    protected Class<GameRecord> getItemClass ()
    {
        return GameRecord.class;
    }

    @Override // from ItemRepository
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(GameCatalogRecord.class);
    }

    @Override // from ItemRepository
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(GameCloneRecord.class);
    }

    @Override // from ItemRepository
    protected Class<RatingRecord> getRatingClass ()
    {
        return coerceRating(GameRatingRecord.class);
    }

    @Override // from ItemRepository
    protected TagRecord createTagRecord ()
    {
        return new GameTagRecord();
    }

    @Override // from ItemRepository
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new GameTagHistoryRecord();
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        super.getManagedRecords(classes);
        classes.add(GameDetailRecord.class);
        classes.add(GamePlayRecord.class);
        classes.add(InstructionsRecord.class);
        classes.add(GameTraceLogRecord.class);
    }

    /** We will not adjust a game's payout higher than 2x to bring it in line with our desired
     * payout rates to avoid potential abuse. Games that consistently award very low amounts can
     * fix their scoring algorithms. */
    protected static final float MAX_PAYOUT_ADJUST = 2f;

    /** Thirty (average) days in milliseconds. */
    protected static final long THIRTY_DAYS = 30*24*60*60*1000L;
}
