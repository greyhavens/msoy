//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;

import com.threerings.msoy.item.data.all.ItemIdent;
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
{
    // Current implementation of the accumulator only collapses ModifyFurniUpdate instances,
    // since those happen frequently enough to demand optimization. SceneAttrUpdates are
    // passed right through (and also cause the accumulated furni updates to get flushed).
    
    /**
     * How often accumulated updates should be checked, in milliseconds between checks.
     */
    protected static final long TIMER_DELAY = 2000;

    public UpdateAccumulator (MsoySceneRepository repo)
    {
        _repo = repo;
        _timer = new Timer(true);

        initializeTimer();
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
            commit(sceneId);
            _repo.persistUpdate(sceneId, update);
            return;
        } 

        // this update can be accumulated. let's save it.
        synchronized(_pending) {
            // do we have anything pending at the moment?
            if (! _pending.containsKey(sceneId)) {
                
                // there's nothing pending - just add the new one to the map
                _pending.put(sceneId, new UpdateWrapper((ModifyFurniUpdate)update));
            } else {
                
                // something is pending already. collapse the existing and the new update into one.
                accumulate(_pending.get(sceneId), (ModifyFurniUpdate)update);
            }

            UpdateWrapper acc = _pending.get(sceneId);
            log.info("Accumulating updates for scene " + sceneId +
                     ", version " + acc.targetVersion);
        }
    }
    
    /**
     * Initializes the internal timer that will check and commit updates on a regular basis.
     */
    protected void initializeTimer ()
    {
        TimerTask task = new TimerTask () {
            public void run () {
                // we're in the timer thread now; perform database operations on the
                // standard invoker thread instead.
                MsoyServer.invoker.postUnit(new Invoker.Unit("UpdateAccumulator task") {
                    public boolean invoke () {
                        checkAll();
                        return false;
                    }
                });
            }
        };

        _timer.scheduleAtFixedRate(task, 0, TIMER_DELAY);
    }
    
    /**
     * If any updates are being accumulated for this scene, force a repository write.
     */
    protected void commit (int sceneId)
    {
        synchronized(_pending) {
            try {
                if (_pending.containsKey(sceneId)) {
                    UpdateWrapper update = _pending.remove(sceneId);
                    _repo.persistUpdate(sceneId, update.unwrap());
                }
            } catch (Exception ex) {
                log.warning("Failed to commit an update accumulator for scene " + sceneId +
                            " [exception=" + ex + "]");
            }
        }
    }

    /**
     * Commits accumulated updates that are either old enough or contain enough modifications.
     */
    protected void checkAll ()
    {
        Vector<Integer> scenesToCommit = new Vector<Integer>();

        synchronized(_pending) {
            for (Map.Entry<Integer, UpdateWrapper> entry : _pending.entrySet()) {
                if (entry.getValue().isCommitDesired())
                {
                    // remember which scenes we should remove, after we're done iterating
                    scenesToCommit.add(entry.getKey());
                } 
            }
        }

        for (int sceneId : scenesToCommit) {
            commit(sceneId);
        }
    }
        
    /**
     * Destructively merges the contents of the newer update into the accumulated update.
     */
    protected void accumulate (UpdateWrapper acc, ModifyFurniUpdate update) 
    {
        // vector of item ids to be removed from the accumulator, and not copied over from update
        Vector<FurniData> redundantAdditions = new Vector<FurniData>();
        Vector<FurniData> redundantRemovals = new Vector<FurniData>();
        
        if (acc.targetVersion >= update.getSceneVersion()) {
            log.warning(
                "Merged update should have a higher version number! Got " +
                update.getSceneVersion() + ", expected more than " + acc.targetVersion + ".");
            return;
        }
        
        // update version
        acc.targetVersion = update.getSceneVersion();

        // find and mark any items that were being added before, but are now getting removed.
        // this pair of actions is redundant, and we get rid of them both.
        if (update.furniRemoved != null) {
            for (FurniData removed : update.furniRemoved) {
                for (FurniData added : acc.furniAdded) {
                    if (removed.getItemIdent().equals(added.getItemIdent())) {
                        // remember it for later removal, after we're done iterating
                        redundantAdditions.add(added);
                        redundantRemovals.add(removed);
                    }
                }
            }
        }

        // remove any redundant additions from the accumulator
        acc.furniAdded.removeAll(redundantAdditions);

        // copy from the update into the accumulator, ignoring redundant removals 
        if (update.furniAdded != null) {
            Collections.addAll(acc.furniAdded, update.furniAdded);
        }
        if (update.furniRemoved != null) {
            for (FurniData removed : update.furniRemoved) {
                if (! redundantRemovals.contains(removed)) {
                    acc.furniRemoved.add(removed);
                }
            }
        }
        
        acc.lastUpdate = System.currentTimeMillis();
        acc.modificationCount++;
    }
    
    /**
     * This helper class makes a copy of update, turning array fields into collections
     * which are easier to modify than simple arrays.
     */
    protected class UpdateWrapper
    {
        /**
         * Max delay, in milliseconds, between when an accumulated update was last modified,
         * and when it should be commited. 
         */
        public static final long TARGET_AGE = 10000;
        
        /**
         * Max number of updates that can be accumulated before a commit is triggered.
         */
        public static final int TARGET_COUNT = 10;
        
        public int targetId;
        public int targetVersion;
        public Vector<FurniData> furniRemoved;
        public Vector<FurniData> furniAdded;

        public long lastUpdate;
        public int modificationCount;

        public UpdateWrapper (ModifyFurniUpdate update)
        {
            targetId = update.getSceneId();
            targetVersion = update.getSceneVersion();
            furniAdded = new Vector<FurniData>();
            furniRemoved = new Vector<FurniData>();
            lastUpdate = System.currentTimeMillis();

            if (update.furniAdded != null) {
                Collections.addAll(furniAdded, update.furniAdded);
            }
            if (update.furniRemoved != null) {
                Collections.addAll(furniRemoved, update.furniRemoved);
            }
        }

        /** Returns true if the update hasn't been modified in a while, or
         *  has been changed more than the desired number of times. */
        public Boolean isCommitDesired ()
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
            furniRemoved.copyInto(update.furniRemoved);
            furniAdded.copyInto(update.furniAdded);
            return update;
        }
    }

    /** Timer object that periodically commits accumulated updates. */
    protected Timer _timer;
    
    /**
     * Internal storage for the updates being accumulated for each scene, indexed by scene id.
     * Since only furni updates are currently supported, the value is a cumulative update
     * that includes all changes accumulated so far. Scenes not included in this map
     * do not have any pending accumulated updates.
     */
    protected Map<Integer, UpdateWrapper> _pending =
        Collections.synchronizedMap(new TreeMap<Integer, UpdateWrapper>());

    /** Repository reference. */
    protected MsoySceneRepository _repo;
}
