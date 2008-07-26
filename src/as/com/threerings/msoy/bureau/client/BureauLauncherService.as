//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java BureauLauncherService interface.
 */
public interface BureauLauncherService extends InvocationService
{
    // from Java interface BureauLauncherService
    function getGameServerRegistryOid (arg1 :Client, arg2 :InvocationService_ResultListener) :void;

    // from Java interface BureauLauncherService
    function launcherInitialized (arg1 :Client) :void;
}
}
