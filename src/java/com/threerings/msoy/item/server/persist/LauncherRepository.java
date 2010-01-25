//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.XList;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.Where;
import com.samskivert.util.Tuple;

import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneFurniRecord;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link LauncherRecord} items.
 */
@Singleton
public class LauncherRepository extends ItemRepository<LauncherRecord>
{
    @Entity(name="LauncherMogMarkRecord")
    public static class LauncherMogMarkRecord extends MogMarkRecord
    {
    }

    @Entity(name="LauncherTagRecord")
    public static class LauncherTagRecord extends TagRecord
    {
    }

    @Entity(name="LauncherTagHistoryRecord")
    public static class LauncherTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public LauncherRepository (PersistenceContext ctx)
    {
        super(ctx);

        // There was a long-running screwup where launchers for AVRGs were not flagged as such,
        // so their LauncherRecords must be updated; unfortunately as a corollary, the actionType
        // of any SceneFurniRecord associated with such a launcher would also be incorrectly made
        // ACTION_LOBBY_GAME rather than ACTION_WORLD_GAME. We have to iterate over a large number
        // of records to fix this.
        registerMigration(new DataMigration("2009_01_25_fixAVRGLaunchersAndFurni") {
            @Override public void invoke () throws DatabaseException {
                // there's 336 AVRGs at the time of writing
                Collection<Integer> avrgIds = _mgameRepo.getAVRGameIds();
                HashSet<Integer> allIds = new HashSet<Integer>();
                for (Integer id : avrgIds) {
                    allIds.add(id);
                    allIds.add(-id);
                }

                // there's 111 of these launchers at the time of writing, update them
                int count = updatePartial(LauncherRecord.class,
                    new Where(LauncherRecord.GAME_ID.in(allIds)), null,
                    LauncherRecord.IS_AVRG, true);
                System.err.println("Updated " + count + " launchers.");

                // then load them
                XList<LauncherRecord> originals = findAll(LauncherRecord.class,
                    new Where(LauncherRecord.GAME_ID.in(allIds)));

                // make a collection of (scene, item) tuples to represent each in-use item
                Set<Tuple<Integer, Integer>> bySceneAndItem = Sets.newHashSet();
                for (ItemRecord rec : originals) {
                    if (rec.location != 0) {
                        bySceneAndItem.add(Tuple.newTuple(rec.location, rec.itemId));
                    }
                }

                // find the keys of those originals
                Collection<Integer> originalIds = originals.map(new Function<LauncherRecord, Integer>() {
                    public Integer apply (LauncherRecord arg) {
                        return arg.itemId;
                    }
                });

                // now figure out all the clones of those launcher originals; there's about 30k
                // of these (with non-zero locaion) in the production at the time of writing
                XList<LauncherCloneRecord> clones = findAll(LauncherCloneRecord.class,
                    new Where(Ops.and(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID).in(originalIds),
                                      getCloneColumn(CloneRecord.LOCATION).notEq(0))));

                // add all the clones onto the tuple collection
                for (CloneRecord rec : clones) {
                    bySceneAndItem.add(Tuple.newTuple(rec.location, rec.itemId));
                }

                log.info("Migrating " + bySceneAndItem.size() + " launcher units; this could take a while.");

                count = 0;
                int updated = 0;
                // finally go through all the tuples and update furni rows
                for (Tuple<Integer, Integer> launcher : bySceneAndItem) {
                    int sceneId = launcher.left;
                    int itemId = launcher.right;

                    count ++;
                    if ((count % 5000) == 0) {
                        log.info("At " + count + " launchers... still going...");
                    }

                    SceneFurniRecord record = _sceneRepo.loadFurni(sceneId, itemId);
                    if (record == null) {
                        log.warning("No such furni in scene during launcher migration!",
                            "sceneId", sceneId, "itemId", itemId, "rows modified");
                        continue;
                    }
                    if (record.actionType == FurniData.ACTION_WORLD_GAME) {
                        // no action needed
                        continue;
                    }
                    if (record.actionType != FurniData.ACTION_LOBBY_GAME) {
                        log.warning("Whoa, surprising actionType during launcher migration!",
                            "sceneId", sceneId, "itemId", itemId, "actionType", record.actionType);
                        continue;
                    }
                    int rows = _sceneRepo.updateActionType(
                        sceneId, itemId, FurniData.ACTION_WORLD_GAME);
                    if (rows != 1) {
                        log.warning("Freak out during launcher migration!", "sceneId", sceneId,
                            "itemId", itemId, "rows modified", rows);
                    } else {
                        updated ++;
                    }
                }
                log.info("Migration complete; " + updated + " records actually updated.");
            }
        });
        // END: TEMP
    }

    @Override
    protected Class<LauncherRecord> getItemClass ()
    {
        return LauncherRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(LauncherCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(LauncherCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(LauncherRatingRecord.class);
    }

    @Override
    protected MogMarkRecord createMogMarkRecord ()
    {
        return new LauncherMogMarkRecord();
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new LauncherTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new LauncherTagHistoryRecord();
    }

    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
}
