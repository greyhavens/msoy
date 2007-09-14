//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;

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
        GameDetailRecord gdr = loadGameDetail(gameId);
        if (gdr == null) {
            return null;
        }
        return loadItem(gameId < 0 ? gdr.sourceItemId : gdr.listedItemId);
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
     * Updates the specified {@link GameDetailRecord}, recording an increase in games played and
     * total player minutes.
     */
    public void noteGamePlayed (int gameId, int playerGames, int playerMinutes, boolean recalc)
        throws PersistenceException
    {
        gameId = Math.abs(gameId); // how to handle playing the original?

        Map<String, SQLExpression> fieldMap = new HashMap<String, SQLExpression>();
        fieldMap.put(GameDetailRecord.PLAYER_GAMES,
                     new Arithmetic.Add(GameDetailRecord.PLAYER_GAMES_C, playerGames));
        fieldMap.put(GameDetailRecord.PLAYER_MINUTES,
                     new Arithmetic.Add(GameDetailRecord.PLAYER_MINUTES_C, playerMinutes));

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

            // write an algorithm that actually does something with 'records' here
            fieldMap.put(GameDetailRecord.ABUSE_FACTOR, new LiteralExp("123"));

            // then delete the records
            deleteAll(GameFlowGrantLogRecord.class,
                      new Where(GameFlowGrantLogRecord.GAME_ID_C, gameId), null);
        }

        // if this addition would cause us to overflow the player minutes field, don't do it
        int overflow = Integer.MAX_VALUE - playerMinutes;
        Where where = new Where(
            new Logic.And(new Conditionals.Equals(GameDetailRecord.GAME_ID_C, gameId),
                          new Conditionals.LessThan(GameDetailRecord.PLAYER_MINUTES_C, overflow)));

        updateLiteral(GameDetailRecord.class, where, GameDetailRecord.getKey(gameId), fieldMap);
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
