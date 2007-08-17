//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;
import com.threerings.whirled.data.SceneUpdate;

import static com.threerings.msoy.Log.log;

/**
 * Converts sequences of related scene updates into a single large update, which is then
 * committed to the database.
 */
public class UpdateAccumulator
    implements MsoyServer.Shutdowner
{
    // Current implementation of the accumulator only collapses ModifyFurniUpdate instances, since
    // those happen frequently enough to demand optimization. SceneAttrUpdates are passed right
    // through (and also cause the accumulated furni updates to get flushed).

    public UpdateAccumulator (MsoySceneRepository repo)
    {
        _repo = repo;
        // note carefully that we're setting up an Interval that posts to the invoker thread.
        new Interval(MsoyServer.invoker) {
            public void expired () {
                checkAll(false);
            }
            public String toString () {
                return "UpdateAccumulator.checkAll";
            }
        }.schedule(FLUSH_INTERVAL, true);
        MsoyServer.registerShutdowner(this);
    }

    /**
     * Adds a new update to the accumulator; if the update cannot be accumulated, we write out
     * whatever we had so far for this scene into the repository, followed by the new update.
     */
    public void add (int sceneId, SceneUpdate update)
        throws PersistenceException
    {
        // if this update doesn't support accumulation, flush whatever we've accumulated
        // for this scene so far, and send the new update over as well.
        if (! (update instanceof ModifyFurniUpdate)) {
            commitUpdates(sceneId);
            _repo.persistUpdate(sceneId, update);
            return;
        }

        // this update can be accumulated. let's save it.
        UpdateWrapper acc = _pending.get(sceneId);
        if (acc == null) {
            // there's nothing pending - just add the new one to the map
            _pending.put(sceneId, acc = new UpdateWrapper((ModifyFurniUpdate)update));
        } else {
            // something is pending already. collapse the existing and the new update into one.
            acc.accumulate((ModifyFurniUpdate)update);
        }

        // TODO: remove this logging after we've tested lots
        log.info("Accumulating updates for scene " + sceneId + ", version " + acc.targetVersion);
    }

    // from interface MsoyServer.Shutdowner
    public void shutdown ()
    {
        MsoyServer.invoker.postUnit(new Invoker.Unit("UpdateAccumulator.shutdown") {
            public boolean invoke () {
                checkAll(true);
                return false;
            }
        });
    }

    /**
     * If any updates are being accumulated for this scene, force a repository write.
     */
    protected void commitUpdates (int sceneId)
        throws PersistenceException
    {
        UpdateWrapper update = _pending.remove(sceneId);
        if (update != null) {
            persistUpdate(update);
        }
    }

    /**
     * Actually persist accumulated updates.
     */
    protected void persistUpdate (UpdateWrapper update)
        throws PersistenceException
    {
        _repo.persistUpdate(update.targetId, update.unwrap());
    }

    /**
     * Commits accumulated updates that are either old enough or contain enough modifications.
     * This gets scheduled to run on the invoker thread.
     */
    protected void checkAll (boolean forceFlushAll)
    {
        for (Iterator<UpdateWrapper> itr = _pending.values().iterator(); itr.hasNext(); ) {
            UpdateWrapper update = itr.next();
            if (forceFlushAll || update.isCommitDesired()) {
                itr.remove(); // go ahead and remove it, even if the persist fails
                try {
                    persistUpdate(update);
                } catch (PersistenceException pe) {
                    log.warning("Failed to commit an accumulated update " +
                        "[sceneId=" + update.targetId+ ", exception=" + pe + "]");
                }
            }
        }
    }

    /**
     * This helper class makes a copy of update, turning array fields into collections which are
     * easier to modify than simple arrays.
     */
    protected class UpdateWrapper
    {
        /**
         * Max delay, in milliseconds, between when an accumulated update was last modified, and
         * when it should be commited.
         */
        public static final long TARGET_AGE = 10000;

        /**
         * Max number of updates that can be accumulated before a commit is triggered.
         */
        public static final int TARGET_COUNT = 10;

        public int targetId;
        public int targetVersion;
        public ArrayList<FurniData> furniRemoved;
        public ArrayList<FurniData> furniAdded;

        public long lastUpdate;
        public int modificationCount;

        public UpdateWrapper (ModifyFurniUpdate update)
        {
            targetId = update.getSceneId();
            targetVersion = update.getSceneVersion();
            furniAdded = new ArrayList<FurniData>();
            furniRemoved = new ArrayList<FurniData>();
            lastUpdate = System.currentTimeMillis();

            if (update.furniAdded != null) {
                Collections.addAll(furniAdded, update.furniAdded);
            }
            if (update.furniRemoved != null) {
                Collections.addAll(furniRemoved, update.furniRemoved);
            }
        }

        /**
         * Destructively merges the contents of the newer update into this accumulated update.
         */
        protected void accumulate (ModifyFurniUpdate update)
        {
            if (targetVersion >= update.getSceneVersion()) {
                log.warning("Merged update should have a higher version number! " +
                    "[Got=" + update.getSceneVersion() + ", expected>=" + targetVersion + "].");
                return;
            }

            // vector of item ids to be removed from the accumulator, and not copied over from
            // update
            ArrayList<FurniData> redundantAdditions = new ArrayList<FurniData>();
            ArrayList<FurniData> redundantRemovals = new ArrayList<FurniData>();

            // update version
            targetVersion = update.getSceneVersion();

            // find and mark any items that were being added before, but are now getting removed.
            // this pair of actions is redundant, and we get rid of them both.
            if (update.furniRemoved != null) {
                for (FurniData removed : update.furniRemoved) {
                    for (FurniData added : furniAdded) {
                        if (removed.getItemIdent().equals(added.getItemIdent())) {
                            // remember it for later removal, after we're done iterating
                            redundantAdditions.add(added);
                            redundantRemovals.add(removed);
                        }
                    }
                }
            }

            // remove any redundant additions from the accumulator
            furniAdded.removeAll(redundantAdditions);

            // copy from the update into this accumulator, ignoring redundant removals
            if (update.furniAdded != null) {
                Collections.addAll(furniAdded, update.furniAdded);
            }
            if (update.furniRemoved != null) {
                for (FurniData removed : update.furniRemoved) {
                    if (! redundantRemovals.contains(removed)) {
                        furniRemoved.add(removed);
                    }
                }
            }

            lastUpdate = System.currentTimeMillis();
            modificationCount++;
        }

        /** Returns true if the update hasn't been modified in a while, or has been changed more
         *  than the desired number of times. */
        public boolean isCommitDesired ()
        {
            return (lastUpdate + TARGET_AGE < System.currentTimeMillis() ||
                    modificationCount >= TARGET_COUNT);
        }

        /** Create a real update object from this wrapper. */
        public ModifyFurniUpdate unwrap ()
        {
            ModifyFurniUpdate update = new ModifyFurniUpdate();
            update.initialize(targetId, targetVersion,
                              new FurniData[furniRemoved.size()],
                              new FurniData[furniAdded.size()]);
            furniRemoved.toArray(update.furniRemoved);
            furniAdded.toArray(update.furniAdded);
            return update;
        }
    }

    /**
     * Internal storage for the updates being accumulated for each scene, indexed by scene id.
     * Since only furni updates are currently supported, the value is a cumulative update that
     * includes all changes accumulated so far. Scenes not included in this map do not have any
     * pending accumulated updates.
     */
    protected HashIntMap<UpdateWrapper> _pending = new HashIntMap<UpdateWrapper>();

    /** Repository reference. */
    protected MsoySceneRepository _repo;

    /** How often accumulated updates should be checked, in milliseconds between checks. */
    protected static final long FLUSH_INTERVAL = 2000;
}
