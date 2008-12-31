//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.Client;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.data.ServerRegistryObject;

/**
 * Service for use by a bureau launcher.
 */
public interface BureauLauncherService extends InvocationService
{
    /**
     * Lets the server know that this launcher is ready to go.
     */
    void launcherInitialized (Client caller);

    /**
     * Query the id of the game server registry object, passing the result back to the given
     * listener as an Integer. When a game server is queried, this should always be 0.
     * @see ServerRegistryObject
     */
    void getGameServerRegistryOid (Client caller, ResultListener listener);

    /**
     * Updates the msoy server with information about a launcher.
     */
    void setBureauLauncherInfo (Client caller, BureauLauncherInfo info);
}
