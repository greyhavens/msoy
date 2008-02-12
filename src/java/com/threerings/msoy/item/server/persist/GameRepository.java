//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.FieldMarshaller;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;

import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.GameFlowGrantLogRecord;
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

        // TEMP
        _ctx.registerMigration(GameDetailRecord.class, new EntityMigration.Rename(
                                   6, "playerGames", "singlePlayerGames"));
        _ctx.registerMigration(GameDetailRecord.class, new EntityMigration.Rename(
                                   6, "playerMinutes", "singlePlayerMinutes"));
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
     * Updates the specified {@link GameDetailRecord}, recording an increase in games played and
     * total player minutes.
     *
     * @return null or the recalculated abuse factor if one was recalculated.
     */
    public Integer noteGamePlayed (int gameId, int playerGames, int playerMins, boolean recalc)
        throws PersistenceException
    {
        Integer newAbuse = null;
        gameId = Math.abs(gameId); // how to handle playing the original?

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

        // if game abuse reassessment is enabled, potentially recalculate that
        if (recalc) {
            // load all actions logged since our last assessment
//            List<GameFlowSummaryRecord> records =
//                findAll(GameFlowSummaryRecord.class,
//                    new Where(GameFlowGrantLogRecord.GAME_ID_C, gameId),
//                    new FromOverride(GameFlowGrantLogRecord.class),
//                    new FieldOverride(GameFlowSummaryRecord.GAME_ID,
//                                      GameFlowGrantLogRecord.GAME_ID_C),
//                    new FieldOverride(GameFlowSummaryRecord.AMOUNT,
//                                      new FunctionExp("sum", GameFlowGrantLogRecord.AMOUNT_C)),
//                    new GroupBy(GameFlowGrantLogRecord.GAME_ID_C));

            // TODO: write an algorithm that actually does something with 'records' here
            newAbuse = 123;
            fieldMap.put(GameDetailRecord.ABUSE_FACTOR, new LiteralExp("" + newAbuse));

            // then delete the records
            deleteAll(GameFlowGrantLogRecord.class,
                      new Where(GameFlowGrantLogRecord.GAME_ID_C, gameId), null);
        }

        // if this addition would cause us to overflow the player minutes field, don't do it
        int overflow = Integer.MAX_VALUE - playerMins;
        Where where = new Where(
            new Logic.And(new Conditionals.Equals(GameDetailRecord.GAME_ID_C, gameId),
                          new Conditionals.LessThan(mcol, overflow)));

        updateLiteral(GameDetailRecord.class, where, GameDetailRecord.getKey(gameId), fieldMap);
        return newAbuse;
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
            gdr.abuseFactor = GameDetailRecord.DEFAULT_ABUSE_FACTOR;
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
        classes.add(GameFlowGrantLogRecord.class);
    }
}
