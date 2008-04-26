//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.expression.ValueExp;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;

import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link Game} items.
 */
public class GameRepository extends ItemRepository<
        GameRecord,
        GameCloneRecord,
        GameCatalogRecord,
        GameRatingRecord>
{
    @Entity(name="GameTagRecord")
    public static class GameTagRecord extends TagRecord
    {
    }

    @Entity(name="GameTagHistoryRecord")
    public static class GameTagHistoryRecord extends TagHistoryRecord
    {
    }

    public GameRepository (PersistenceContext ctx)
    {
        super(ctx);

        // TEMP 02-22-2008
        _ctx.registerMigration(GameDetailRecord.class, new EntityMigration.Rename(
                                   7, "abuseFactor", "payoutFactor"));
        _ctx.registerMigration(GameDetailRecord.class, new EntityMigration.Rename(
                                   7, "lastAbuseRecalc", "lastPayoutRecalc"));
        // END TEMP
    }

    /**
     * Returns the total number of listed games in the repository.
     */
    public int getGameCount ()
        throws PersistenceException
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
        throws PersistenceException
    {
        return loadGameRecord(gameId, loadGameDetail(gameId));
    }

    /**
     * Returns the {@link GameDetailRecord} for the specified game or null if the id is unknown.
     */
    public GameDetailRecord loadGameDetail (int gameId)
        throws PersistenceException
    {
        return load(GameDetailRecord.class, Math.abs(gameId));
    }

    /**
     * Loads the appropriate {@link GameRecord} for the specified game detail.
     */
    public GameRecord loadGameRecord (int gameId, GameDetailRecord gdr)
        throws PersistenceException
    {
        if (gdr == null) {
            return null;
        }
        return loadItem(gameId < 0 ? gdr.sourceItemId : gdr.listedItemId);
    }

    /**
     * Loads all listed game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or -1 to load all (listed) games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     */
    public List<GameRecord> loadGenre (byte genre, int limit)
        throws PersistenceException
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Join(getItemClass(), ItemRecord.ITEM_ID,
                             getCatalogClass(), CatalogRecord.LISTED_ITEM_ID));
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }
        if (genre >= 0) {
            clauses.add(new Where(new Conditionals.Equals(GameRecord.GENRE_C, genre)));
        }

        // sort out the primary and secondary order by clauses
        List<SQLExpression> obExprs = Lists.newArrayList();
        List<OrderBy.Order> obOrders = Lists.newArrayList();
        addOrderByRating(obExprs, obOrders);
        clauses.add(new OrderBy(obExprs.toArray(new SQLExpression[obExprs.size()]),
                                obOrders.toArray(new OrderBy.Order[obOrders.size()])));

        // finally fetch all the game records of interest
        return findAll(getItemClass(), clauses.toArray(new QueryClause[clauses.size()]));
    }

    /**
     * Sets the specified game's payout factor to the specified value. This is used for AVRGs which
     * manage their own payout factor instead of using {@link
     * #updatePayoutFactor(GameDetailRecord,int)}.
     */
    public boolean updatePayoutFactor (int gameId, int factor, int minutes)
        throws PersistenceException
    {
        gameId = Math.abs(gameId); // how to handle playing the original?
        return 0 < updatePartial(GameDetailRecord.class, gameId,
                                 GameDetailRecord.PAYOUT_FACTOR, factor,
                                 GameDetailRecord.LAST_PAYOUT_RECALC, minutes);
    }

    /**
     * Grinds through a game's recent flow award data and updates its payout factor to adjust for
     * biases in its payout patterns.
     */
    public int noteGamePlayed (GameDetailRecord detail, int playerGames, int playerMins,
                               int flowAwarded, int recalcMins, int hourlyRate)
        throws PersistenceException
    {
        int currentMins = detail.multiPlayerMinutes + detail.singlePlayerMinutes + playerMins;
        int accumMins = currentMins - detail.lastPayoutRecalc;
        int newFactor = 0;

        // if we're ready for a payout factor recalculation, do that as well
        if (recalcMins > 0 && accumMins > recalcMins) {
            // compute our average flow per hour award for the period in question
            int accumFlow = detail.flowSinceLastRecalc + flowAwarded;
            float awardedPerHour = accumFlow / (accumMins / 60f);

            // sanity checks
            if (accumMins > 0 && accumFlow > 0) {
                // our factor is the target hourly rate over our actual rate (because we'll multiply
                // future awards by this factor to scale them to the target hourly rate)
                float targetRatio = hourlyRate / awardedPerHour;
                targetRatio = Math.min(Math.max(0, targetRatio), MAX_PAYOUT_ADJUST);
                int targetFactor = Math.round(targetRatio * 256);

                // set our factor to the average of these two values; move slowly toward our target
                newFactor = (detail.payoutFactor + targetFactor)/2;

                log.info("Updating payout factor [game=" + detail.gameId +
                         ", accumMins=" + accumMins + ", accumFlow=" + accumFlow +
                         ", aph=" + awardedPerHour + ", targetRatio=" + targetRatio +
                         ", newFactor=" + newFactor + "].");
            }
        }

        noteGamePlayed(detail.gameId, playerGames, playerMins, flowAwarded, newFactor, currentMins);

        return newFactor;
    }

    /**
     * Updates the specified {@link GameDetailRecord}, recording an increase in games played, total
     * player minutes and flow awarded.
     */
    public void noteGamePlayed (int gameId, int playerGames, int playerMins)
        throws PersistenceException
    {
        gameId = Math.abs(gameId); // TODO: don't record metrics for the original?
        noteGamePlayed(gameId, playerGames, playerMins, 0, 0, 0);
    }

    /**
     * Updates the instructions for the specified game.
     */
    public void updateGameInstructions (int gameId, String instructions)
        throws PersistenceException
    {
        updatePartial(GameDetailRecord.class, gameId, GameDetailRecord.INSTRUCTIONS, instructions);
    }

    @Override // from ItemRepository
    public void insertOriginalItem (GameRecord item, boolean catalogListing)
        throws PersistenceException
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
            insert(gdr);
            // source games use -gameId to differentiate themselves from all non-source games
            updatePartial(getItemClass(), item.itemId, GameRecord.GAME_ID, -gdr.gameId);

        } else if (catalogListing) {
            updatePartial(GameDetailRecord.class, item.gameId,
                          GameDetailRecord.LISTED_ITEM_ID, item.itemId);
        }
    }

    @Override // from ItemRepository
    public void deleteItem (int itemId)
        throws PersistenceException
    {
        // if we're deleting an original item; we need to potentially delete or update its
        // associated game detail record
        GameDetailRecord gdr = null;
        if (itemId > 0) {
            GameRecord item = load(getItemClass(), itemId);
            if (item != null && item.gameId != 0) {
                gdr = load(GameDetailRecord.class, item.gameId);
            }
            if (gdr != null) {
                if (gdr.sourceItemId == itemId) {
                    gdr.sourceItemId = 0;
                    if (gdr.listedItemId == 0) {
                        delete(gdr);
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

    @Override // from ItemRepository
    protected Class<GameRecord> getItemClass ()
    {
        return GameRecord.class;
    }

    @Override // from ItemRepository
    protected Class<GameCatalogRecord> getCatalogClass ()
    {
        return GameCatalogRecord.class;
    }

    @Override // from ItemRepository
    protected Class<GameCloneRecord> getCloneClass ()
    {
        return GameCloneRecord.class;
    }

    @Override // from ItemRepository
    protected Class<GameRatingRecord> getRatingClass ()
    {
        return GameRatingRecord.class;
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
    }

    /**
     * Helper function for the other {@link #noteGamePlayed} methods.
     */
    protected void noteGamePlayed (int gameId, int playerGames, int playerMins,
                                   int flowAwarded, int payoutFactor, int lastPayoutRecalc)
        throws PersistenceException
    {
        String gcname, mcname;
        SQLExpression gcol, mcol;
        if (playerGames > 1) {
            gcname = GameDetailRecord.MULTI_PLAYER_GAMES;
            gcol = GameDetailRecord.MULTI_PLAYER_GAMES_C;
            mcname = GameDetailRecord.MULTI_PLAYER_MINUTES;
            mcol = GameDetailRecord.MULTI_PLAYER_MINUTES_C;
        } else {
            gcname = GameDetailRecord.SINGLE_PLAYER_GAMES;
            gcol = GameDetailRecord.SINGLE_PLAYER_GAMES_C;
            mcname = GameDetailRecord.SINGLE_PLAYER_MINUTES;
            mcol = GameDetailRecord.SINGLE_PLAYER_MINUTES_C;
        }

        Map<String, SQLExpression> fieldMap = Maps.newHashMap();
        fieldMap.put(gcname, new Arithmetic.Add(gcol, playerGames));
        fieldMap.put(mcname, new Arithmetic.Add(mcol, playerMins));

        if (payoutFactor > 0) {
            fieldMap.put(GameDetailRecord.PAYOUT_FACTOR, new ValueExp(payoutFactor));
            fieldMap.put(GameDetailRecord.LAST_PAYOUT_RECALC, new ValueExp(lastPayoutRecalc));
            fieldMap.put(GameDetailRecord.FLOW_SINCE_LAST_RECALC, new ValueExp(0));
        } else if (flowAwarded > 0) {
            fieldMap.put(GameDetailRecord.FLOW_SINCE_LAST_RECALC,
                         new Arithmetic.Add(GameDetailRecord.FLOW_SINCE_LAST_RECALC_C, flowAwarded));
        }

        // if this addition would cause us to overflow the player minutes field, don't do it
        int overflow = Integer.MAX_VALUE - playerMins;
        Where where = new Where(
            new Logic.And(new Conditionals.Equals(GameDetailRecord.GAME_ID_C, gameId),
                          new Conditionals.LessThan(mcol, overflow)));
        updateLiteral(GameDetailRecord.class, where, GameDetailRecord.getKey(gameId), fieldMap);
    }

    /** We will not adjust a game's payout higher than 2x to bring it in line with our desired
     * payout rates to avoid potential abuse. Games that consistently award very low amounts can
     * fix their scoring algorithms. */
    protected static final float MAX_PAYOUT_ADJUST = 2f;
}
