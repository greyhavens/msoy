//
// $Id$

package com.threerings.msoy.avrg.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.room.data.MsoyLocation;

/**
 * A service for AVR (in-world) games.
 */
public interface AVRGameService extends InvocationService<ClientObject>
{
    /**
     * Instruct the server to compute a payout for this player linearly proportional to the
     * payoutLevel (which must lie in the interval [0, 1]). The identifier is not used, but
     * must not be null.
     *
     * In consequence of this call, a TASK_COMPLETED event is dispatched holding the supplied
     * quest identifier along with the actual number of coins awarded.
     */
    void completeTask (int playerId, String questId, float payoutLevel,
                       ConfirmListener listener);

    /**
     * Notifies the game server that the player has gone idle or returned to activity.
     */
    void setIdle (boolean nowIdle, ConfirmListener listener);

    /**
     * Loads persistent data for the specified player.
     */
    void loadOfflinePlayer (int playerId, ResultListener listener);

    /**
     * Updates a persistent property for the specified player.
     */
    void setOfflinePlayerProperty (int playerId, String propName, Object data,
                                   Integer key, boolean isArray, ConfirmListener listener);

    /**
     * Requests that the specified player be moved to the specified room.
     */
    void movePlayerToRoom (int playerId, int roomId, MsoyLocation exit,
                           InvocationListener listener);
}
