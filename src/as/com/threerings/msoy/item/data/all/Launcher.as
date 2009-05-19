//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for a Launcher item.
 */
public class Launcher extends GameItem
{
    /** Indicates whether or not the game we're launching is an AVRG. */
    public var isAVRG :Boolean;

    public function Launcher ()
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
        return LAUNCHER;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        isAVRG = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeBoolean(isAVRG);
    }
}
}
