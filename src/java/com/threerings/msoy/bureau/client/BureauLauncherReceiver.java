//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.presents.client.InvocationReceiver;

/** Defines client exposure for bureau launching. */
public interface BureauLauncherReceiver extends InvocationReceiver
{
    /** Requests to launch a thane bureau with the given connect-back information. */
    void launchThane (String bureauId, String token);

    /** Tells the launcher to log off everything and shut down. */
    void shutdownLauncher ();
}
