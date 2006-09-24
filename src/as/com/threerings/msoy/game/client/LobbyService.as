//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * A service for locating the lobby for a particular game.
 */
public interface LobbyService extends InvocationService
{
    /**
     * Return the Oid of the lobby for the specified game item.
     *
     * @param gameId the item id of a Game-type item.
     */
    function identifyLobby (
        client :Client, gameId :int,
        listener :InvocationService_ResultListener) :void;
}
}
