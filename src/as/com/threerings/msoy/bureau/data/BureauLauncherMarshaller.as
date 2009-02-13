//
// $Id$

package com.threerings.msoy.bureau.data {

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.client.BureauLauncherService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the <code>BureauLauncherService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BureauLauncherMarshaller extends InvocationMarshaller
    implements BureauLauncherService
{
    /** The method id used to dispatch <code>launcherInitialized</code> requests. */
    public static const LAUNCHER_INITIALIZED :int = 1;

    // from interface BureauLauncherService
    public function launcherInitialized (arg1 :Client) :void
    {
        sendRequest(arg1, LAUNCHER_INITIALIZED, [
            
        ]);
    }

    /** The method id used to dispatch <code>setBureauLauncherInfo</code> requests. */
    public static const SET_BUREAU_LAUNCHER_INFO :int = 2;

    // from interface BureauLauncherService
    public function setBureauLauncherInfo (arg1 :Client, arg2 :BureauLauncherInfo) :void
    {
        sendRequest(arg1, SET_BUREAU_LAUNCHER_INFO, [
            arg2
        ]);
    }
}
}
