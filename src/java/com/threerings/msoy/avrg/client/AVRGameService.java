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
    public void setProperty (
        Client client, String key, byte[] value, boolean persistent, ConfirmListener listener);

    public void deleteProperty (
        Client client, String key, ConfirmListener listener);

    public void setPlayerProperty (
        Client client, String key, byte[] value, boolean persistent, ConfirmListener listener);

    public void deletePlayerProperty (
        Client client, String key, ConfirmListener listener);

    public void sendMessage (
        Client client, String msg, Object data, int playerId, InvocationListener listener);

    public void startQuest (
        Client client, String questId, String status, ConfirmListener listener);

    public void updateQuest (
        Client caller, String questId, int step, String progress, ConfirmListener listener);

    public void completeQuest (
        Client caller, String questId, int payoutLevel, ConfirmListener listener);

    public void cancelQuest (
        Client caller, String questId, ConfirmListener listener);
}
