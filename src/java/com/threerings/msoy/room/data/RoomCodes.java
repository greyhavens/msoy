//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the Room services.
 */
public interface RoomCodes extends InvocationCodes
{
    /** A message event type dispatched on the room object. */
    public static final String SPRITE_MESSAGE = "sprMsg";

    /** A message event type dispatched on the room object. */
    public static final String SPRITE_SIGNAL = "sprSig";

    /** A message even dispatched on the member object to followers. */
    public static final String FOLLOWEE_MOVED = "folMov";

    /** An error (sort of) reported when an entity requests control but is already being controlled
     * by another client. */
    public static final String E_ALREADY_CONTROLLED = "e.already_controlled";

    /** Error reported when the entity is denied entrance to a scene. */
    public static final String E_ENTRANCE_DENIED = "e.entrance_denied";

    /** Error reported when a pet owner calls a pet into a room they cannot. */
    public static final String E_CANNOT_ADD_PET = "e.cannot_add_pet";

    /** A room layer that is in front of normal furniture and such. */
    public static final byte FOREGROUND_EFFECT_LAYER = 0;

    /** The normal room layer where most things are placed. */
    public static final byte FURNITURE_LAYER = 1;

    /** A room layer that is behind of normal furniture, but in front of decor. */
    public static final byte BACKGROUND_EFFECT_LAYER = 2;

    /** The backmost layer, should only be occupied by decor objects. */
    public static final byte DECOR_LAYER = 3;

    /** Layout constant: normal layout. */
    public static final byte LAYOUT_NORMAL = 0;

    /** Layout constant: fill the visible room area. */
    public static final byte LAYOUT_FILL = 1;

}
