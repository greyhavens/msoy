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
    void userEntered (String username);

    /**
     * Called when a user leaves the project room.
     */
    void userLeft (String username);
}
