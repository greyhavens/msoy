//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * A service for locating the lobby for a particular game.
 */
public interface LobbyService extends InvocationService
{
    /**
     * Return the oid of the lobby for the specified game item.
     *
     * @param gameId the item id of a Game-type item.
     */
    public void identifyLobby (Client client, int gameId, ResultListener listener);

    /**
     * Requests to immediately get into the specified game. If a game could be started and/or
     * located, 0 will be returned, otherwise the oid of the lobby will be returned so that the
     * client can fall back to displaying the lobby.
     */
    public void playNow (Client client, int gameId, boolean singlePlayer, ResultListener listener);

    /**
     * Return the Oid of the game that this player is in.  Returns -1 if they are currently not
     * in one.
     *
     * @param playerId the member id of the player to find.
     */
    public void joinPlayerGame (Client client, int playerId, ResultListener listener);
}
