//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link BureauLauncherService}.
 */
public interface BureauLauncherProvider extends InvocationProvider
{
    /**
     * Handles a {@link BureauLauncherService#launcherInitialized} request.
     */
    public void launcherInitialized (ClientObject caller);
}
