//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.data.all.StaticMediaDesc;

/**
 * Codes and constants relating to the Room services.
 */
@com.threerings.util.ActionScript(omit=true)
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

    /** Can't have an unstamped furni in a themed room. */
    public static final String E_FURNI_NOT_STAMPED = "e.furni_not_stamped";

    /** Can't have an unlisted furni in a home room template. */
    public static final String E_TEMPLATE_FURNI_NOT_LISTED = "e.furni_not_listed";

    /** Can't have a furni in a room template whose listing is not hidden. */
    public static final String E_TEMPLATE_LISTING_NOT_HIDDEN = "e.template_listing_not_hidden";

    /** Can't have a furni in a room template whose listing is not owned by the brand. */
    public static final String E_TEMPLATE_LISTING_NOT_OWNED= "e.template_listing_not_owned";

    /** Can't have a derived furni in a room template. */
    public static final String E_TEMPLATE_LISTING_DERIVED = "e.template_listing_derived";

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

    /** Static media descriptor for the default room snapshot in full size. */
    public static final MediaDesc DEFAULT_SNAPSHOT_FULL = new StaticMediaDesc(
        MediaMimeTypes.IMAGE_JPEG, "snapshot", "default");

    /** Static media descriptor for the default room snapshot in thumbnail size. */
    public static final MediaDesc DEFAULT_SNAPSHOT_THUMB = new StaticMediaDesc(
        MediaMimeTypes.IMAGE_JPEG, "snapshot", "default_t");
}
