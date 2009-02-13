//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * An ActionScript version of the Java BureauLauncherService interface.
 */
public interface BureauLauncherService extends InvocationService
{
    // from Java interface BureauLauncherService
    function launcherInitialized (arg1 :Client) :void;

    // from Java interface BureauLauncherService
    function setBureauLauncherInfo (arg1 :Client, arg2 :BureauLauncherInfo) :void;
}
}
