//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.MsoyBureauLauncherService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.net.Transport;

/**
 * Provides the implementation of the {@link MsoyBureauLauncherService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoyBureauLauncherMarshaller extends InvocationMarshaller
    implements MsoyBureauLauncherService
{
    /** The method id used to dispatch {@link #launcherInitialized} requests. */
    public static final int LAUNCHER_INITIALIZED = 1;

    // from interface MsoyBureauLauncherService
    public void launcherInitialized (Client arg1)
    {
        sendRequest(arg1, LAUNCHER_INITIALIZED, new Object[] {
            
        });
    }
}
