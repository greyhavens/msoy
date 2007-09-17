//
// $Id$

package com.threerings.msoy.swiftly.client;

/**
 * An object which wants to be informed when the base application is being shutdown.
 */
public interface ShutdownNotifier
{
    /**
     * Notification that the application is shutting down.
     */
    public void shuttingDown ();
}
