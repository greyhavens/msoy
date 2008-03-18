//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

import static com.threerings.msoy.Log.log;

/**
 * Converts sequences of furniture modification updates into a single large update, which is then
 * committed to the database.
 */
public class UpdateAccumulator
    implements MsoyServer.Shutdowner
{
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
    public void add (SceneUpdate update)
        throws PersistenceException
    {
        int sceneId = update.getSceneId();
        UpdateWrapper wrapper = _pending.get(sceneId);

        // if we can't accumulate this update, just write it to the repository
        if (!(update instanceof ModifyFurniUpdate)) {
            _repo.persistUpdate(update);
            // but let any accumulator know that the version just got incremented
            if (wrapper != null) {
                wrapper.note(update);
            }
            return;
        }

        // if we can accumulate this update, pass it to the accumulator
        if (wrapper == null) {
            _pending.put(sceneId, new UpdateWrapper((ModifyFurniUpdate)update));
        } else {
            wrapper.accumulate((ModifyFurniUpdate)update);
        }

        // TODO: remove this logging after we've tested lots
        log.info("Accumulating update " + update + ".");
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
     * Commits accumulated updates that have been around long enough.  This is invoked on the
     * invoker thread.
     */
    protected void checkAll (boolean forceFlushAll)
    {
        long now = System.currentTimeMillis();
        for (Iterator<UpdateWrapper> itr = _pending.values().iterator(); itr.hasNext(); ) {
            UpdateWrapper update = itr.next();
            if (forceFlushAll || update.isCommitDesired(now)) {
                itr.remove(); // go ahead and remove it, even if the persist fails
                try {
                    _repo.persistUpdate(update.unwrap());
                } catch (PersistenceException pe) {
                    log.warning("Failed to commit an accumulated update [sceneId=" + update +
                                ", exception=" + pe + "]");
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
        public UpdateWrapper (ModifyFurniUpdate update)
        {
            _targetId = update.getSceneId();
            _targetVersion = update.getSceneVersion();
            _versionIncrement = update.getVersionIncrement();
            _furniAdded = Lists.newArrayList();
            _furniRemoved = Lists.newArrayList();
            _lastUpdate = System.currentTimeMillis();

            if (update.furniAdded != null) {
                Collections.addAll(_furniAdded, update.furniAdded);
            }
            if (update.furniRemoved != null) {
                Collections.addAll(_furniRemoved, update.furniRemoved);
            }
        }

        /**
         * Accumulates the version increment for this update into our increment. We have to do this
         * for all updates in between our starting update and our final update regardless of
         * whether we accumulate those updates or not.
         */
        public void note (SceneUpdate update)
        {
            _versionIncrement += update.getVersionIncrement();
        }

        /**
         * Destructively merges the contents of the newer update into this accumulated update.
         */
        public void accumulate (ModifyFurniUpdate update)
        {
            // update our version increment to account for this update
            note(update);

            // find and mark any items that were being added before, but are now getting removed.
            // this pair of actions is redundant, and we get rid of them both
            List<FurniData> redundantAdditions = Lists.newArrayList();
            List<FurniData> redundantRemovals = Lists.newArrayList();
            if (update.furniRemoved != null) {
                for (FurniData removed : update.furniRemoved) {
                    for (FurniData added : _furniAdded) {
                        if (removed.getItemIdent().equals(added.getItemIdent())) {
                            // remember it for later removal, after we're done iterating
                            redundantAdditions.add(added);
                            redundantRemovals.add(removed);
                        }
                    }
                }
            }

            // remove any redundant additions from the accumulator
            _furniAdded.removeAll(redundantAdditions);

            // copy from the update into this accumulator, ignoring redundant removals
            if (update.furniAdded != null) {
                Collections.addAll(_furniAdded, update.furniAdded);
            }
            if (update.furniRemoved != null) {
                for (FurniData removed : update.furniRemoved) {
                    if (!redundantRemovals.contains(removed)) {
                        _furniRemoved.add(removed);
                    }
                }
            }

            _lastUpdate = System.currentTimeMillis();
        }

        /** Returns true if the update hasn't been modified in sufficiently long that we should
         * write it to the database. */
        public boolean isCommitDesired (long now)
        {
            return (_lastUpdate + TARGET_AGE < now);
        }

        /** Create a real update object from this wrapper. */
        public ModifyFurniUpdate unwrap ()
        {
            ModifyFurniUpdate update = new ModifyFurniUpdate() {
                public int getVersionIncrement () {
                    return _versionIncrement;
                }
            };
            update.initialize(_targetId, _targetVersion,
                              _furniRemoved.toArray(new FurniData[_furniRemoved.size()]),
                              _furniAdded.toArray(new FurniData[_furniAdded.size()]));
            return update;
        }

        @Override // from Object
        public String toString ()
        {
            return "[tid=" + _targetId + ", tvers=" + _targetVersion +
                ", vinc=" + _versionIncrement + ", removed=" + _furniRemoved.size() +
                ", added=" + _furniAdded.size() + "]";
        }

        protected int _targetId;
        protected int _targetVersion;
        protected int _versionIncrement;

        protected List<FurniData> _furniRemoved;
        protected List<FurniData> _furniAdded;

        protected long _lastUpdate;

        /** Max delay, in milliseconds, between when an accumulated update was last modified, and
         * when it should be commited. */
        protected static final long TARGET_AGE = 10000;
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
