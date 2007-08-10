//
// $Id$

package com.threerings.msoy.swiftly.util;

import java.applet.AppletContext;

import com.threerings.crowd.util.CrowdContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.util.MessageManager;

/**
 * Provides necessary services, and juicy goodness.
 */
public interface SwiftlyContext extends CrowdContext
{
    /**
     * Provides access to translation message bundles.
     */
    public MessageManager getMessageManager ();

    /** Translates the specified message using the specified message bundle. */
    public String xlate (String bundle, String message);

    /** Returns the applet context */
    public AppletContext getAppletContext ();

    /** Returns the MemberObject for the client */
    public MemberObject getMemberObject ();

    /** Displays an info level message to the user. */
    public void showInfoMessage (String message);

    /** Displays an error level message to the user. */
    public void showErrorMessage (String message);
}
