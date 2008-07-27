//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Set of room decor information, including room settings and a background bitmap.
 */
public class Decor extends Item
{
    /** Type constant for a room with no background, just bare walls. This constant is deprecated,
     *  please do not use. Legacy decor of this type will be drawn using default type. */
    public static final byte DRAWN_ROOM_DEPRECATED = 0;

    /** Type constant for a standard room. The room will use standard layout, and its background
     *  image will be drawn behind all furniture. */
    public static final byte IMAGE_OVERLAY = 1;

    /** Type constant for a room whose background is fixed to the viewport, instead of scene. */
    public static final byte FIXED_IMAGE = 2;

    /** Type constant for a room with non-standard, flat layout. */
    public static final byte FLAT_LAYOUT = 3;

    /** The number of type constants. */
    public static final int TYPE_COUNT = 4;

    /** Room type. Specifies how the background wallpaper and layout are handled. */
    public byte type;

    /** Room height, in pixels. */
    public short height;

    /** Room width, in pixels. */
    public short width;

    /** Room depth, in pixels. */
    public short depth;

    /** Horizon position, in [0, 1]. */
    public float horizon;

    /** Specifies whether side walls should be displayed. */
    public boolean hideWalls;

    /** Bitmap offset along the x axis, in room units. */
    public float offsetX;

    /** Bitmap offset along the y axis, in room units. */
    public float offsetY;

    @Override // from Item
    public byte getType ()
    {
        return DECOR;
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() &&
            nonBlank(name, MAX_NAME_LENGTH) &&
            type < TYPE_COUNT && width > 0 && height > 0 && depth > 0 &&
            horizon <= 1.0f && horizon >= 0.0f;
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return null; // there is no default
    }
}
