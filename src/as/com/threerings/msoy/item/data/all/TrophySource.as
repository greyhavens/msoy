//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for a TrophySource item.
 */
public class TrophySource extends SubItem
{
    /** The required width for a trophy image. */
    public static const TROPHY_WIDTH :int = 60;

    /** The required height for a trophy image. */
    public static const TROPHY_HEIGHT :int = 60;

    /** The order in which to display this trophy compared to other trophies. */
    public var sortOrder :int;

    /** Whether or not this trophy's description is a secret. */
    public var secret :Boolean;

    public function TrophySource ()
    {
    }

    // from Item
    override public function getType () :int
    {
        return TROPHY_SOURCE;
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return _thumbMedia;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        sortOrder = ins.readInt();
        secret = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(sortOrder);
        out.writeBoolean(secret);
    }
}
}
