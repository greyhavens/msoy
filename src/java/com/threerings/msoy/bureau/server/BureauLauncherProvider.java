//
// $Id$

package com.threerings.msoy.bureau.server;

import javax.annotation.Generated;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.client.BureauLauncherService;

/**
 * Defines the server-side of the {@link BureauLauncherService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BureauLauncherService.java.")
public interface BureauLauncherProvider extends InvocationProvider
{
    /**
     * Handles a {@link BureauLauncherService#launcherInitialized} request.
     */
    void launcherInitialized (ClientObject caller);

    /**
     * Handles a {@link BureauLauncherService#setBureauLauncherInfo} request.
     */
    void setBureauLauncherInfo (ClientObject caller, BureauLauncherInfo arg1);
}
