//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.world.data.FurniUpdate;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

/**
 * Converts sequences of furniture modification updates into a single large update, which is then
 * committed to the database.
 */
public class UpdateAccumulator
    implements ShutdownManager.Shutdowner
{
    @Inject public UpdateAccumulator (ShutdownManager shutmgr)
    {
        shutmgr.registerShutdowner(this);
    }

    public void init (MsoySceneRepository repo)
    {
        _repo = repo;
        _flusher = new Interval(_invoker) {
            public void expired () {
                checkAll(false);
            }
            public String toString () {
                return "UpdateAccumulator.checkAll";
            }
        };
        _flusher.schedule(FLUSH_INTERVAL, true);
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
        if (!(update instanceof FurniUpdate)) {
            _repo.persistUpdate(update);
            // but let any accumulator know that the version just got incremented
            if (wrapper != null) {
                wrapper.note(update);
            }
            return;
        }

        // if we can accumulate this update, pass it to the accumulator
        if (wrapper == null) {
            _pending.put(sceneId, wrapper = new UpdateWrapper());
        }
        wrapper.accumulate((FurniUpdate)update);
    }

    // from interface ShutdownManager.Shutdowner
    public void shutdown ()
    {
        // stop our flushing interval
        _flusher.cancel();

        _invoker.postUnit(new Invoker.Unit("UpdateAccumulator.shutdown") {
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
            UpdateWrapper wrapper = itr.next();
            if (forceFlushAll || wrapper.isCommitDesired(now)) {
                itr.remove(); // go ahead and remove it, even if the persist fails
                wrapper.filterAndStore(_repo);
            }
        }
    }

    /**
     * This helper class makes a copy of update, turning array fields into collections which are
     * easier to modify than simple arrays.
     */
    protected static class UpdateWrapper extends ArrayList<FurniUpdate>
    {
        /**
         * Returns true if the update hasn't been modified in sufficiently long that we should
         * write it to the database.
         */
        public boolean isCommitDesired (long now)
        {
            return (_lastUpdate + TARGET_AGE < now);
        }

        /**
         * Accumulates the version increment for this update into our increment. We have to do this
         * for all updates in between our starting update and our final update regardless of
         * whether we accumulate those updates or not.
         */
        public void note (SceneUpdate update)
        {
            _finalVersion = update.getSceneVersion() + update.getVersionIncrement();
        }

        /**
         * Adds this update to our list.
         */
        public void accumulate (FurniUpdate update)
        {
            add(update);
            note(update);
            _lastUpdate = System.currentTimeMillis();
        }

        /**
         * Filters out redundant updates from this list.
         */
        public void filterAndStore (MsoySceneRepository repo)
        {
            // look backwards for remove events and filter all events for that furni prior; also
            // look for change events and filter all changes prior to the last change
            Set<ItemIdent> removed = Sets.newHashSet(), changed = Sets.newHashSet();
            for (int ii = size()-1; ii >= 0; ii--) {
                FurniUpdate update = get(ii);
                ItemIdent ident = update.data.getItemIdent();
                if (removed.contains(ident)) {
                    remove(ii);
                } else if (update instanceof FurniUpdate.Change) {
                    if (changed.contains(ident)) {
                        remove(ii);
                    } else {
                        changed.add(ident);
                    }
                } else if (update instanceof FurniUpdate.Remove) {
                    removed.add(ident);
                }
            }

            // apply those updates to the database (it will catch and log any errors thrown while
            // modifying the persistent store)
            repo.persistUpdates(this, _finalVersion);
        }

        protected int _finalVersion;
        protected long _lastUpdate = System.currentTimeMillis();

        /** Max delay, in milliseconds, between when an accumulated update was last modified, and
         * when it should be commited. */
        protected static final long TARGET_AGE = 60000;
    }

    /** Internal storage for the updates being accumulated for each scene, indexed by scene id.
     * Since only furni updates are currently supported, the value is a cumulative update that
     * includes all changes accumulated so far. Scenes not included in this map do not have any
     * pending accumulated updates. */
    protected IntMap<UpdateWrapper> _pending = IntMaps.newHashIntMap();

    /** Repository reference. */
    protected MsoySceneRepository _repo;

    /** Handles flushing our room updates periodically. Note that we're setting up an Interval that
     * posts to the invoker thread. */
    protected Interval _flusher;

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;

    /** How often accumulated updates should be checked, in milliseconds between checks. */
    protected static final long FLUSH_INTERVAL = 2000;
}
