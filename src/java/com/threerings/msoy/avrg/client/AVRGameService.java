//
// $Id$

package com.threerings.msoy.avrg.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * A service for AVR (in-world) games.
 */
public interface AVRGameService extends InvocationService
{
    /**
     * Instruct the server to compute a payout for this player linearly proportional to the
     * payoutLevel (which must lie in the interval [0, 1]). The identifier is not used, but
     * must not be null.
     *
     * In consequence of this call, a TASK_COMPLETED event is dispatched holding the supplied
     * quest identifier along with the actual number of coins awarded.
     */
    void completeTask (Client caller, int playerId, String questId, float payoutLevel,
                       ConfirmListener listener);

    /**
     * Notifies the game server that the player has gone idle or returned to activity.
     */
    void setIdle (Client caller, boolean nowIdle, ConfirmListener listener);

    void loadOfflinePlayer (Client caller, int playerId, ResultListener listener);

    void setOfflinePlayerProperty (Client caller, int playerId, String propName, Object data,
                                   Integer key, boolean isArray, ConfirmListener listener);
}
