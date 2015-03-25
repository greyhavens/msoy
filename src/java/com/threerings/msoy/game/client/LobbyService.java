//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

/**
 * A service for locating the lobby for a particular game.
 */
public interface LobbyService extends InvocationService<ClientObject>
{
    /**
     * Return the oid of the lobby for the specified game item.
     *
     * @param gameId the item id of a Game-type item.
     */
    void identifyLobby (int gameId, ResultListener listener);

    /**
     * Requests to immediately start playing a game. If a game could be started and/or located, 0
     * will be returned, otherwise the oid of the lobby will be returned so that the client can
     * fall back to displaying the lobby.

     * @param playerId the player whose game we wish to join or 0.
     */
    void playNow (int gameId, int playerId, ResultListener listener);
}
