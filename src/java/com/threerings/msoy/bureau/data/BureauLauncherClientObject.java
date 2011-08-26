//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.presents.data.ClientObject;

@com.threerings.util.ActionScript(omit=true)
public class BureauLauncherClientObject extends ClientObject
{
    /** The name of the host where this launcher client is running. */
    public transient String hostname;
}
