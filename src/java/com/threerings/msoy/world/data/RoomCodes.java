//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the Room services.
 */
public interface RoomCodes extends InvocationCodes
{
    /** A message event type dispatched on the room object. */
    public static final String SPRITE_MESSAGE = "sprMsg";

    /** An error (sort of) reported when an entity requests control but is already being controlled
     * by another client. */
    public static final String E_ALREADY_CONTROLLED = "e.already_controlled";
}
