//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Set of room decor information, including room settings and a background bitmap.
 */
public class Decor extends Item
{
    /** A type constant indicating a normal room where defaultly
     * draw some walls. */
    public static const DRAWN_ROOM :int = 0;

    /** A type constant indicating a room where the background image should
     * be drawn covering everything, but layered behind everything else such
     * that the background image IS the scene to the viewer. */
    public static const IMAGE_OVERLAY :int = 1;

    /** A type constant indicating a background image that does not scroll. */
    public static const FIXED_IMAGE :int = 2;

    /** The number of type constants. */
    public static const TYPE_COUNT :int = 3;

    /** Room type. Controls how the background wallpaper image is handled. */
    public var type :int;
    
    /** Room height, in pixels. */
    public var height :int;
    
    /** Room width, in pixels. */
    public var width :int;

    /** Room depth, in pixels. */
    public var depth :int;

    /** Horizon position, in [0, 1]. */
    public var horizon :Number;

    // from Item
    override public function getType () :int
    {
        return DECOR;
    }

    override protected function getDefaultThumbnailMedia () :MediaDesc
    {
        if (furniMedia != null && furniMedia.isImage()) {
            return furniMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return null; // there is no default
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(type);
        out.writeShort(height);
        out.writeShort(width);
        out.writeShort(depth);
        out.writeFloat(horizon);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        type = ins.readByte();
        height = ins.readShort();
        width = ins.readShort();
        depth = ins.readShort();
        horizon = ins.readFloat();
    }
}
}
