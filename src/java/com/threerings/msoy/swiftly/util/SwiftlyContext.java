//
// $Id$

package com.threerings.msoy.swiftly.util;

import java.applet.AppletContext;

import com.threerings.crowd.util.CrowdContext;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.client.SimpleProgressBar;
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

    /** Returns the MemberName for the client */
    public MemberName getMember ();

    /** Displays an info level message to the user. */
    public void showInfoMessage (String message);

    /** Displays an error level message to the user. */
    public void showErrorMessage (String message);

    /** Show a progress bar for the supplied number of milliseconds. */
    public void showProgress (int time);

    /** Tell the progress bar to stop displaying progress. */
    public void stopProgress ();

    /** Return the progress bar defined in this context */
    public SimpleProgressBar getProgressBar ();
}
