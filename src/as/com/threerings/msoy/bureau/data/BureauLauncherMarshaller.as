//
// $Id$

package com.threerings.msoy.bureau.data {

import com.threerings.msoy.bureau.client.BureauLauncherService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

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
    public static const GET_GAME_SERVER_REGISTRY_OID :int = 1;

    // from interface BureauLauncherService
    public function getGameServerRegistryOid (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_GAME_SERVER_REGISTRY_OID, [
            listener2
        ]);
    }

    /** The method id used to dispatch {@link #launcherInitialized} requests. */
    public static const LAUNCHER_INITIALIZED :int = 2;

    // from interface BureauLauncherService
    public function launcherInitialized (arg1 :Client) :void
    {
        sendRequest(arg1, LAUNCHER_INITIALIZED, [
            
        ]);
    }
}
}
