//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;

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

    /** Error reported when a client attempt to set a room property when they cannot. */
    public static final String E_CANNOT_SET_PROPERTY = "e.cannot_add_set_property";

    /** A room layer that is in front of normal furniture and such. */
    public static final byte FOREGROUND_LAYER = 0;

    /** The normal room layer where most things are placed. */
    public static final byte FURNITURE_LAYER = 1;

    /** The backmost layer, should only be occupied by decor objects. */
    public static final byte DECOR_LAYER = 2;

    /** Layout constant: normal layout. */
    public static final byte LAYOUT_NORMAL = 0;

    /** Layout constant: fill the visible room area. */
    public static final byte LAYOUT_FILL = 1;

    /** Static media descriptor for the default room snapshot in thumbnail size. */
    public static final MediaDesc DEFAULT_ROOM_THUMBNAIL = new StaticMediaDesc(
        MediaDesc.IMAGE_JPEG, "snapshot", "default_t", MediaDesc.HALF_VERTICALLY_CONSTRAINED);
}
