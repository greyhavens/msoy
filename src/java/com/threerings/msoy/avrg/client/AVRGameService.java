//
// $Id$

package com.threerings.msoy.avrg.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * A service for joining AVR (in-world) games.
 */
public interface AVRGameService extends InvocationService
{
    /**
     * Awards the specified trophy to the requesting player.
     */
    public void awardTrophy (
        Client client, String ident, int playerId, InvocationListener listener);

    /**
     * Awards the specified prize to the requesting player.
     */
    public void awardPrize (
        Client client, String ident, int playerId, InvocationListener listener);

    /**
     * Instruct the server to compute a payout for this player linearly proportional to the
     * payoutLevel (which must lie in the interval [0, 1]). The identifier is not used, but
     * must not be null.
     *
     * In consequence of this call, a TASK_COMPLETED event is dispatched holding the supplied
     * quest identifier along with the actual number of coins awarded.
     */
    public void completeTask (
        Client caller, int playerId, String questId, float payoutLevel, ConfirmListener listener);

    /**
     * Start a ticker that will send out timestamp information at the interval specified.
     *
     * @param msOfDelay must be at least 50, or 0 may be set to halt and clear a previously started
     * ticker.
     */
    public void setTicker (
        Client client, String tickerName, int msOfDelay, InvocationListener listener);
}
