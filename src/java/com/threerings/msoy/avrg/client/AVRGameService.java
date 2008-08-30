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
    public void startQuest (
        Client client, String questId, String status, ConfirmListener listener);

    public void updateQuest (
        Client caller, String questId, int step, String progress, ConfirmListener listener);

    public void completeQuest (
        Client caller, String questId, float payoutLevel, ConfirmListener listener);

    public void cancelQuest (
        Client caller, String questId, ConfirmListener listener);

    /**
     * Start a ticker that will send out timestamp information at the interval specified.
     *
     * @param msOfDelay must be at least 50, or 0 may be set to halt and clear a previously started
     * ticker.
     */
    public void setTicker (
        Client client, String tickerName, int msOfDelay, InvocationListener listener);
}
