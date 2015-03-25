//
// $Id$

package com.threerings.msoy.bureau.data;

import javax.annotation.Generated;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.client.BureauLauncherService;

/**
 * Provides the implementation of the {@link BureauLauncherService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BureauLauncherService.java.")
public class BureauLauncherMarshaller extends InvocationMarshaller<ClientObject>
    implements BureauLauncherService
{
    /** The method id used to dispatch {@link #launcherInitialized} requests. */
    public static final int LAUNCHER_INITIALIZED = 1;

    // from interface BureauLauncherService
    public void launcherInitialized ()
    {
        sendRequest(LAUNCHER_INITIALIZED, new Object[] {
        });
    }

    /** The method id used to dispatch {@link #setBureauLauncherInfo} requests. */
    public static final int SET_BUREAU_LAUNCHER_INFO = 2;

    // from interface BureauLauncherService
    public void setBureauLauncherInfo (BureauLauncherInfo arg1)
    {
        sendRequest(SET_BUREAU_LAUNCHER_INFO, new Object[] {
            arg1
        });
    }
}
