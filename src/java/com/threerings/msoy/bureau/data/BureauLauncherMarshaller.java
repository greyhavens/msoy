//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.client.BureauLauncherService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link BureauLauncherService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BureauLauncherMarshaller extends InvocationMarshaller
    implements BureauLauncherService
{
    /** The method id used to dispatch {@link #getGameServerRegistryOid} requests. */
    public static final int GET_GAME_SERVER_REGISTRY_OID = 1;

    // from interface BureauLauncherService
    public void getGameServerRegistryOid (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_GAME_SERVER_REGISTRY_OID, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #launcherInitialized} requests. */
    public static final int LAUNCHER_INITIALIZED = 2;

    // from interface BureauLauncherService
    public void launcherInitialized (Client arg1)
    {
        sendRequest(arg1, LAUNCHER_INITIALIZED, new Object[] {});
    }

    /** The method id used to dispatch {@link #setBureauLauncherInfo} requests. */
    public static final int SET_BUREAU_LAUNCHER_INFO = 3;

    // from interface BureauLauncherService
    public void setBureauLauncherInfo (Client arg1, BureauLauncherInfo arg2)
    {
        sendRequest(arg1, SET_BUREAU_LAUNCHER_INFO, new Object[] {
            arg2
        });
    }
}
