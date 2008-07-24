//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.stats.data.IntSetStat;
import com.threerings.stats.data.IntSetStatAdder;
import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.IntStatIncrementer;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatModifier;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.web.client.DeploymentConfig;

import static com.threerings.msoy.Log.log;

/**
 * Services for modifying a member's stats from servlet code.
 */
@BlockingThread @Singleton
public class StatLogic
{
    /**
     * Increments an integer statistic for the specified player.
     *
     * @exception ClassCastException thrown if the registered type of the specified stat is not a
     * {@link IntStat}.
     */
    public void incrementStat (int playerId, Stat.Type type, int delta)
    {
        updateStat(playerId, new IntStatIncrementer(type, delta));
    }

    /**
     * Adds an integer to an IntSetStat for the specified player.
     *
     * @exception ClassCastException thrown if the registered type of the specified stat is not a
     * {@link IntSetStat}.
     */
    public void addToSetStat (int playerId, Stat.Type type, int value)
    {
        updateStat(playerId, new IntSetStatAdder(type, value));
    }

    /**
     * Attempts to apply the given stat modification. If the stat modification fails MAX_TRIES
     * times, a warning will be logged; otherwise, a MemberNodeAction will be posted.
     */
    protected <T extends Stat> void updateStat (int playerId, StatModifier<T> modifier)
    {
        if (!DeploymentConfig.devDeployment) {
            return; // TODO remove this when the Passport system goes live
        }

        try {
            // first update the stat in the database
            if (_statRepo.updateStat(playerId, modifier)) {
                // TODO: send this stat modifier to the server that is hosting this member's
                // runtime stats, if any (note: this functionality may also be provided by the
                // Vilya Stat system)
                log.info("updateStat succeeded", "playerId", playerId,
                    "statType", modifier.getType().name());
            }
        } catch (PersistenceException pe) {
            log.warning("updateStat failed", "playerId", playerId, "type", modifier.getType(), pe);
        }
    }

    @Inject StatRepository _statRepo;
}
