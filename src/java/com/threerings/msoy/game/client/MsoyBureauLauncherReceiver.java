//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.InvocationReceiver;

/** Defines client exposure for bureau launching. */
public interface MsoyBureauLauncherReceiver extends InvocationReceiver
{
    /** Requests to launch a thane bureau with the given connect-back information. */
    public void launchThane (
        String bureauId,
        String token,
        String server,
        int port);
}
