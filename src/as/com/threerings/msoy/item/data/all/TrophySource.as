//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Contains the runtime data for a TrophySource item.
 */
public class TrophySource extends Item
{
    /** The required width for a trophy image. */
    public static const TROPHY_WIDTH :int = 60;

    /** The required height for a trophy image. */
    public static const TROPHY_HEIGHT :int = 60;

    /** An identifier for this trophy, used by the game code. */
    public var ident :String;

    public function TrophySource ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getThumbnailMedia();
    }

    // from Item
    override public function getType () :int
    {
        return TROPHY_SOURCE;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        ident = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(ident);
    }
}
}
