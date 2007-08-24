//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

// TEMP
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.Tuple;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.threerings.ezgame.server.persist.GameCookieRecord;
import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.server.persist.SceneFurniRecord;
import static com.threerings.msoy.Log.log;
// END TEMP

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

    // TEMP
    public void bigGameIdMigration ()
        throws PersistenceException
    {
        final boolean TESTING = false;

        // compute the set of all "games" (which may encompass items)
        HashMap<String,Tuple<GameDetailRecord,ArrayIntSet>> mapping =
            new HashMap<String,Tuple<GameDetailRecord,ArrayIntSet>>();
        for (GameRecord record : findAll(getItemClass(), new Where(GameRecord.GAME_ID_C, 0))) {
            String name = record.name;
            int idx;
            // hackery for known names
            if ((idx = name.indexOf(" v1.")) != -1) {
                name = name.substring(0, idx);
            } else if ((idx = name.indexOf(" v0.")) != -1) {
                name = name.substring(0, idx);
            } else if ((idx = name.indexOf(" 1.")) != -1) {
                name = name.substring(0, idx);
            } else if ((idx = name.indexOf(" v.")) != -1) {
                name = name.substring(0, idx);
            } else if ((idx = name.indexOf(" 2007")) != -1) {
                name = name.substring(0, idx);
            }
            Tuple<GameDetailRecord,ArrayIntSet> info = mapping.get(name);
            if (info == null) {
                info = new Tuple<GameDetailRecord,ArrayIntSet>(
                    new GameDetailRecord(), new ArrayIntSet());
                mapping.put(name, info);
            }
            if (record.ownerId == 0 && record.itemId > info.left.listedItemId) {
                info.left.listedItemId = record.itemId;
            }
            if (record.ownerId != 0 && (info.left.sourceItemId == 0 ||
                                        record.itemId < info.left.sourceItemId)) {
                info.left.sourceItemId = record.itemId;
            }
            info.right.add(record.itemId);
        }

        int tempGameId = 0; // TESTING

        // create detail records for all games
        IntIntMap toGameId = new IntIntMap();
        ArrayList<GameDetailRecord> gdrs = new ArrayList<GameDetailRecord>();
        for (Map.Entry<String,Tuple<GameDetailRecord,ArrayIntSet>> entry : mapping.entrySet()) {
            GameDetailRecord gdr = entry.getValue().left;
            if (TESTING) {
                gdr.gameId = ++tempGameId;
            } else {
                insert(gdr);
            }
            gdrs.add(gdr);

            log.info("Mapping " + gdr + " -> " + entry.getValue().right);
            if (gdr.sourceItemId != 0) {
                entry.getValue().right.remove(gdr.sourceItemId);
                toGameId.put(gdr.sourceItemId, -gdr.gameId);
                if (!TESTING) {
                    updatePartial(
                        getItemClass(), gdr.sourceItemId, GameRecord.GAME_ID, -gdr.gameId);
                }
            }
            if (entry.getValue().right.size() > 0) {
                for (int itemId : entry.getValue().right.toIntArray()) {
                    toGameId.put(itemId, gdr.gameId);
                }
                if (!TESTING) {
                    updatePartial(getItemClass(),
                                  new Where(new Conditionals.In(GameRecord.ITEM_ID_C,
                                                                entry.getValue().right)), null,
                                  GameRecord.GAME_ID, gdr.gameId);
                }
            }
        }

        // renumber all game cookie records (collisions will be deleted)
        Collection<GameCookieRecord> cookies = findAll(GameCookieRecord.class);
        if (!TESTING) {
            for (GameCookieRecord gcr : cookies) {
                delete(gcr);
            }
        }
        for (GameCookieRecord gcr : cookies) {
            log.info("Moving " + gcr.gameId + ":" + gcr.userId + " to " + toGameId.get(gcr.gameId));
            gcr.gameId = toGameId.get(gcr.gameId);
            if (!TESTING) {
                try {
                    insert(gcr);
                } catch (DuplicateKeyException dke) {
                    log.info("Dropping " + gcr.gameId + ":" + gcr.userId + " due to duplication.");
                    // no problem
                }
            }
        }

        // renumber all rating records (collisions will be deleted)
        Collection<RatingRecord> ratings = findAll(RatingRecord.class);
        if (!TESTING) {
            for (RatingRecord rr : ratings) {
                delete(rr);
            }
        }
        for (RatingRecord rr : ratings) {
            log.info("Moving " + rr.gameId + ":" + rr.playerId + " to " + toGameId.get(rr.gameId));
            rr.gameId = toGameId.get(rr.gameId);
            if (!TESTING) {
                try {
                    insert(rr);
                } catch (DuplicateKeyException dke) {
                    log.info("Dropping " + rr.gameId + ":" + rr.playerId + " due to duplication.");
                    // no problem
                }
            }
        }

        // update all game items placed into scenes
        Where where = new Where(SceneFurniRecord.ACTION_TYPE_C, FurniData.ACTION_LOBBY_GAME);
        for (SceneFurniRecord sfr : findAll(SceneFurniRecord.class, where)) {
            int didx = (sfr.actionData == null) ? -1 : sfr.actionData.indexOf(":");
            if (didx == -1) {
                log.warning("Unable to update game furni record " + sfr.actionData + ".");
                continue;
            }
            int gameId;
            try {
                gameId = Integer.parseInt(sfr.actionData.substring(0, didx));
            } catch (Throwable t) {
                log.warning("Unable to parse game furni record id " + sfr.actionData + ".");
                continue;
            }
            String actionData = toGameId.get(gameId) + sfr.actionData.substring(didx);
            log.info("Switching " + sfr.actionData + " to " + actionData);
            sfr.actionData = actionData;
            if (!TESTING) {
                update(sfr);
            }
        }
    }
    // END TEMP

    /**
     * Loads the appropriate {@link GameRecord} for the specified game id. If the id is negative,
     * the source item record will be loaded, if positive, the listed item record will be loaded.
     * May return null if the game id is unknown or no valid listed or source item record is
     * available.
     */
    public GameRecord loadGameRecord (int gameId)
        throws PersistenceException
    {
        GameDetailRecord gdr = load(GameDetailRecord.class, Math.abs(gameId));
        if (gdr == null) {
            return null;
        }
        return loadItem(gameId < 0 ? gdr.sourceItemId : gdr.listedItemId);
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
}
