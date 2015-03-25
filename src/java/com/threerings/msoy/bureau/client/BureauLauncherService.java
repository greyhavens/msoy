//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;

/**
 * Service for use by a bureau launcher.
 */
public interface BureauLauncherService extends InvocationService<ClientObject>
{
    /**
     * Lets the server know that this launcher is ready to go.
     */
    void launcherInitialized ();

    /**
     * Updates the msoy server with information about a launcher.
     */
    void setBureauLauncherInfo (BureauLauncherInfo info);
}
