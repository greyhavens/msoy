//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents a piece of furniture (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
public class Furniture extends Item
{
    /** An action associated with this furniture which is dispatched to the
     * virtual world client when the furniture is clicked on (max length 255
     * characters). */
    public var action :String = "";

    /** The x position of the hot spot to use for this furniture. */
    public var hotSpotX :int;

    /** The y position of the hot spot to use for this furniture. */
    public var hotSpotY :int;

    /**
     * Returns a {@link MediaDesc} configured to display a testing piece of furni.
     */
    public static function getTestingFurniMedia () :MediaDesc
    {
        return new DefaultItemMediaDesc(
            MediaDesc.APPLICATION_SHOCKWAVE_FLASH, FURNITURE, "testing");
    }

    public function Furniture ()
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
        return FURNITURE;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        action = (ins.readField(String) as String);
        hotSpotX = ins.readShort();
        hotSpotY = ins.readShort();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(action);
        out.writeShort(hotSpotX);
        out.writeShort(hotSpotY);
    }

    // from Item
    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return null; // there is no default
    }
}
}
