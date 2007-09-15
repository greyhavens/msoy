//
// $Id$

package com.threerings.msoy.swiftly.client.event;

/**
 * Provides feedback when a user joins or leaves the project.
 */
public interface OccupantListener
{
    /**
     * Called when a user joins the project room.
     */
    public void userEntered (String username);

    /**
     * Called when a user leaves the project room.
     */
    public void userLeft (String username);
}
