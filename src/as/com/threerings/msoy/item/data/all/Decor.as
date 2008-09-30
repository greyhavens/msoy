//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Set of room decor information, including room settings and a background bitmap.
 */
public class Decor extends Item
{
    /** Type constant for a room with no background, just bare walls. This constant is deprecated,
     *  please do not use. Legacy decor of this type will be drawn using default type. */
    public static const DRAWN_ROOM_DEPRECATED :int = 0;

    /** Type constant for a standard room. The room will use standard layout, and its background
     *  image will be drawn behind all furniture. */
    public static const IMAGE_OVERLAY :int = 1;

    /** Type constant for a room whose background is fixed to the viewport, instead of scene. */
    public static const FIXED_IMAGE :int = 2;

    /** Type constant for a room with non-standard, flat layout. */
    public static const FLAT_LAYOUT :int = 3;

    /** The number of type constants. */
    public static const TYPE_COUNT :int = 4;

    /** Room type. Specifies how the background wallpaper and layout are handled. */
    public var type :int;

    /** Room height, in pixels. */
    public var height :int;

    /** Room width, in pixels. */
    public var width :int;

    /** Room depth, in pixels. */
    public var depth :int;

    /** Horizon position, in [0, 1]. */
    public var horizon :Number;

    /** Specifies whether side walls should be displayed. */
    public var hideWalls :Boolean;

    /** Bitmap offset along the x axis, in room units. */
    public var offsetX :Number;

    /** Bitmap offset along the y axis, in room units. */
    public var offsetY :Number;

    /** The adjusted scale of entities in this room. */
    public var scale :Number;

    public function Decor ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getFurniMedia();
    }

    // from Item
    override public function getType () :int
    {
        return DECOR;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        type = ins.readByte();
        height = ins.readShort();
        width = ins.readShort();
        depth = ins.readShort();
        horizon = ins.readFloat();
        hideWalls = ins.readBoolean();
        offsetX = ins.readFloat();
        offsetY = ins.readFloat();
        scale = ins.readFloat();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(type);
        out.writeShort(height);
        out.writeShort(width);
        out.writeShort(depth);
        out.writeFloat(horizon);
        out.writeBoolean(hideWalls);
        out.writeFloat(offsetX);
        out.writeFloat(offsetY);
        out.writeFloat(scale);
    }

    // from Item
    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return null; // there is no default
    }
}
}
