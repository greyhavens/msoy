//
// $Id$

package com.threerings.msoy.swiftly.client.event;

/**
 * Interface to be used by Swing components interested in access control events.
 *
 */
public interface AccessControlListener
{
    /**
     * Called to inform this component to display a writable interface.
     */
    public void writeAccessGranted();

    /**
     * Called to inform this component to display a read-only interface.
     */
    public void readOnlyAccessGranted();
}
