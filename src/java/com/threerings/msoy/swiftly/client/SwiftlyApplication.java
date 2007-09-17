//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.net.URL;

import com.threerings.msoy.swiftly.client.controller.PassiveNotifier;
import com.threerings.msoy.swiftly.client.view.SwiftlyWindowView;

/**
 * Provides a few useful services from the root view component to the rest of the client.
 */
public interface SwiftlyApplication
{
    /**
     * Request that the supplied SwifltyWindow be attached to the main application content pane.
     */
    public void attachWindow (SwiftlyWindowView window);

    /**
     * Create a PassiveNotifier connected to this SwiftlyApplication.
     */
    public PassiveNotifier createNotifier ();

    /**
     * Displays the supplied URL in an application specific way.
     */
    public void showURL (URL url);

    /**
     * Adds a ShutdownNotifier to this SwiftlyApplication.
     */
    public void addShutdownNotifier (ShutdownNotifier notifier);
}
