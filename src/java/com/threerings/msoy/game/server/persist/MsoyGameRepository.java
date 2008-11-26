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
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.Arithmetic;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic.And;
import com.samskivert.depot.operator.Logic.Or;
import com.samskivert.depot.operator.Logic;

import com.threerings.msoy.server.persist.CountRecord;

import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import static com.threerings.msoy.Log.log;

/**
 * Handles the persistence side of everything about games except the actual item record which is
 * handled by {@link GameRepository}.
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

    @Inject public MsoyGameRepository (PersistenceContext ctx)
    {
        super(ctx);

        ctx.registerMigration(InstructionsRecord.class,
            new SchemaMigration.Retype(2, InstructionsRecord.INSTRUCTIONS));
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
        return _gameRepo.loadItem(GameRecord.isDeveloperVersion(gameId) ?
                                  gdr.sourceItemId : gdr.listedItemId);
    }

    /**
     * Loads up the listed {@link GameRecord} records for all games in the specified set that have
     * listed game records. Games which have no listed record are ommitted from the results. The
     * game records will be returned in arbitrary order.
     */
    public List<GameRecord> loadListedGameRecords (Collection<Integer> gameIds)
    {
        Set<Integer> itemIds = Sets.newHashSet();
        for (GameDetailRecord gdr : loadAll(GameDetailRecord.class, gameIds)) {
            if (gdr.listedItemId != 0) {
                itemIds.add(gdr.listedItemId);
            }
        }
        return _gameRepo.loadItems(itemIds);
    }

    /**
     * Called by ItemLogic when a new original game item is created.
     */
    public void gameCreated (GameRecord item)
    {
        // sanity check
        if (item.isCatalogMaster() && item.gameId == 0) {
            log.warning("Listed game with no assigned game id " + item + ".");
        }

        // if this item did not yet have a game id, create a new game detail record and wire it up
        if (item.gameId == 0) {
            GameDetailRecord gdr = new GameDetailRecord();
            gdr.sourceItemId = item.itemId;
            gdr.payoutFactor = GameDetailRecord.DEFAULT_PAYOUT_FACTOR;
            gdr.flowToNextRecalc = GameDetailRecord.INITIAL_RECALC_FLOW;
            insert(gdr);
            // source games use -gameId to differentiate themselves from all non-source games
            _gameRepo.updateGameId(item.itemId, -gdr.gameId);
            // fill the game id back into the newly created game record
            item.gameId = -gdr.gameId;

        } else if (item.isCatalogMaster()) {
            // update the game detail record with the new listed item id
            updatePartial(GameDetailRecord.class, item.gameId,
                          GameDetailRecord.LISTED_ITEM_ID, item.itemId);
        }
    }

    /**
     * Called by ItemLogic when a game item is deleted.
     */
    public void gameDeleted (GameRecord item)
    {
        // if we're deleting an original item; we need to potentially delete or update its
        // associated game detail record
        GameDetailRecord gdr = null;
        if (item.itemId > 0) {
            if (item.gameId != 0) {
                gdr = load(GameDetailRecord.class, Math.abs(item.gameId));
            }
            if (gdr != null) {
                if (gdr.sourceItemId == item.itemId) {
                    gdr.sourceItemId = 0;
                    if (gdr.listedItemId == 0) {
                        purgeGame(gdr.gameId);
                    } else {
                        update(gdr);
                    }
                }
                // zeroing out listedItemId is handled in gameDelisted()
            }
        }
    }

    /**
     * Called by ItemLogic when a game item is delisted.
     *
     * @param item the catalog master item that was delisted.
     */
    public void gameDelisted (GameRecord item)
    {
        // zero out the game detail record's listed game item id
        updatePartial(GameDetailRecord.class, item.gameId, GameDetailRecord.LISTED_ITEM_ID, 0);
    }

    /**
     * Updates the specified {@link GameDetailRecord}, recording an increase in games played and a
     * decrease in flow to next recalc.
     */
    protected void noteGamePlayed (GamePlayRecord gprec)
    {
        SQLExpression add = new Arithmetic.Add(GameDetailRecord.GAMES_PLAYED_C, gprec.playerGames);
        SQLExpression sub = new Arithmetic.Sub(
            GameDetailRecord.FLOW_TO_NEXT_RECALC_C, gprec.flowAwarded);

        updateLiteral(GameDetailRecord.class, Math.abs(gprec.gameId),
            ImmutableMap.of(GameDetailRecord.GAMES_PLAYED, add,
                GameDetailRecord.FLOW_TO_NEXT_RECALC, sub,
                GameDetailRecord.LAST_PAYOUT, new ValueExp(gprec.recorded)));
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
        noteGamePlayed(gprec);
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
        Where where = new Where(new Logic.And(
            new Conditionals.GreaterThanEquals(GamePlayRecord.RECORDED_C, new Timestamp(start)),
            new Conditionals.LessThan(GamePlayRecord.RECORDED_C, new Timestamp(end))
        ));
        
        return findAll(GamePlayRecord.class, where);
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

    /**
     * Deletes all ephemeral data associated with the specified game.
     */
    protected void purgeGame (int gameId)
    {
        delete(GameDetailRecord.class, gameId);
        delete(InstructionsRecord.class, gameId);
        deleteAll(GamePlayRecord.class, new Where(GamePlayRecord.GAME_ID_C, gameId));
        deleteAll(GameTraceLogRecord.class, new Where(GameTraceLogRecord.GAME_ID_C, gameId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GameDetailRecord.class);
        classes.add(GamePlayRecord.class);
        classes.add(InstructionsRecord.class);
        classes.add(GameTraceLogRecord.class);
    }

    @Inject protected GameRepository _gameRepo;

    /** We will not adjust a game's payout higher than 2x to bring it in line with our desired
     * payout rates to avoid potential abuse. Games that consistently award very low amounts can
     * fix their scoring algorithms. */
    protected static final float MAX_PAYOUT_ADJUST = 2f;

    /** Thirty (average) days in milliseconds. */
    protected static final long THIRTY_DAYS = 30*24*60*60*1000L;
}
