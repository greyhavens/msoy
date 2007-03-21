//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Set of room decor information, including room settings and a background bitmap.
 */
public class Decor extends Item
{
    /** A type constant indicating a normal room where defaultly
     * draw some walls. */
    public static final byte DRAWN_ROOM = 0;
    
    /** A type constant indicating a room where the background image should
     * be drawn covering everything, but layered behind everything else such
     * that the background image IS the scene to the viewer. */
    public static final byte IMAGE_OVERLAY = 1;

    /** A type constant indicating a image that does not scroll. */
    public static final byte FIXED_IMAGE = 2;

    /** The number of type constants. */
    public static final int TYPE_COUNT = 3;
    
    /** Room type. Controls how the background wallpaper image is handled. */
    public byte type;
    
    /** Room height, in pixels. */
    public short height;
    
    /** Room width, in pixels. */
    public short width;

    /** Room depth, in pixels. */
    public short depth;

    /** Horizon position, in [0, 1]. */
    public float horizon;

    // @Override from Item
    public byte getType ()
    {
        return DECOR;
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() &&
            type < TYPE_COUNT && width > 0 && height > 0 && depth > 0 &&
            horizon <= 1.0f && horizon >= 0.0f;
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        if (furniMedia != null && furniMedia.isImage()) {
            return furniMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return null; // there is no default
    }
}
