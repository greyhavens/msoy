//
// $Id$

package com.threerings.msoy.badge.server;

import com.samskivert.io.PersistenceException;
import com.threerings.presents.annotation.BlockingThread;
import com.threerings.stats.data.IntSetStat;
import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.Stat;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.badge.Log;

/**
 * Facilitates stat updating for stats that don't require the affected member to be online.
 */
@BlockingThread
public class MemberStatUtil
{
    /**
     * Increments an integer statistic for the specified player.
     *
     * @exception ClassCastException thrown if the registered type of the specified stat is not a
     * {@link IntStat}.
     */
    public static void incrementStat (int playerId, Stat.Type type, final int delta)
        throws PersistenceException
    {
        updateStat(playerId, type, new StatUpdater() {
            public boolean update (Stat stat) {
                ((IntStat)stat).increment(delta);
                return true;
            }
        });
    }

    /**
     * Adds an integer to an IntSetStat for the specified player.
     *
     * @exception ClassCastException thrown if the registered type of the specified stat is not a
     * {@link IntSetStat}.
     */
    public static void addToSetStat (int playerId, Stat.Type type, final int value)
        throws PersistenceException
    {
        updateStat(playerId, type, new StatUpdater() {
            public boolean update (Stat stat) {
                return ((IntSetStat)stat).add(value);
            }
        });
    }

    /**
     * Attempts to apply the given stat modification. If the stat modification fails MAX_TRIES
     * times, a warning will be logged; otherwise, a MemberNodeAction will be posted.
     *
     * @return true if the stat was successfully updated, false otherwise.
     */
    protected static boolean updateStat (int playerId, Stat.Type type, StatUpdater updater)
        throws PersistenceException
    {
        if (!DeploymentConfig.devDeployment) {
            // TODO remove this when the Passport system goes live
            return true;
        }

        boolean updateSuccessful = true;
        boolean needsUpdate = false;
        int ii = 0;
        Stat stat = null;
        for (ii = 0; ii < MAX_TRIES; ii++) {
            stat = getStat(playerId, type);
            needsUpdate = updater.update(stat);
            // if the stat was updated, try to commit it to the repo. otherwise, we're done.
            if (needsUpdate) {
                updateSuccessful = MsoyServer.statRepo.updateStatIfCurrent(playerId, stat);
            }

            if (!needsUpdate || updateSuccessful) {
                break;
            }
        }

        if (needsUpdate) {
            if (updateSuccessful) {
                // TODO: post a MemberNotification so that the member's stats can be reloaded
                // in memory if they're online
                Log.log.info("Successfully updated player stat after " + (ii + 1) + " attempts. " +
                    "[playerId= " + playerId + ", stat=" + stat + "]");
            } else {
                Log.log.warning("Failed to update player stat after " + MAX_TRIES + " attempts. " +
                    "[playerId= " + playerId + ", stat=" + stat + "]");
            }
        }

        return (!needsUpdate || updateSuccessful);
    }

    protected static Stat getStat (final int playerId, final Stat.Type type)
        throws PersistenceException
    {
        Stat stat = MsoyServer.statRepo.loadStat(playerId, type.code());
        return (stat != null ? stat : type.newStat());
    }

    protected interface StatUpdater
    {
        /**
         * Updates the stat. Returns true if the stat was actually updated and needs to be
         * updated in the repository, false otherwise.
         */
        boolean update (Stat stat);
    };

    protected static final int MAX_TRIES = 5;
}
