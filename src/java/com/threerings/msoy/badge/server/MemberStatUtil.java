//
// $Id$

package com.threerings.msoy.badge.server;

import com.samskivert.io.PersistenceException;
import com.threerings.presents.annotation.BlockingThread;
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
     * @exception ClassCastException thrown if the registered type of the specified stat is not an
     * {@link IntStat}.
     */
    public static void incrementStat (int playerId, Stat.Type type, final int delta)
        throws PersistenceException
    {
        updateStat(playerId, type, new MemberStatUtil.StatUpdater() {
            public boolean update(int playerId, Stat.Type type) throws PersistenceException {
                IntStat stat = (IntStat) getStat(playerId, type);
                stat.increment(delta);
                return MsoyServer.statRepo.updateStatIfCurrent(playerId, stat);
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

        boolean success = false;
        for (int ii = 0; ii < MAX_TRIES; ii++) {
            if (updater.update(playerId, type)) {
                success = true;
                break;
            }
        }

        if (success) {
            // TODO: post a MemberNotification so that the member's stats can be reloaded
            // in memory if they're online
        } else {
            Log.log.warning("Failed to update player stat after " + MAX_TRIES + " attempts. " +
                "(playerId: " + playerId + ", statType: " + type.name() + ")");
        }

        return success;
    }

    protected static Stat getStat (final int playerId, final Stat.Type type)
        throws PersistenceException
    {
        Stat stat = MsoyServer.statRepo.loadStat(playerId, type.code());
        return (stat != null ? stat : type.newStat());
    }

    protected interface StatUpdater
    {
        boolean update (int playerId, Stat.Type type) throws PersistenceException;
    };

    protected static final int MAX_TRIES = 5;
}
