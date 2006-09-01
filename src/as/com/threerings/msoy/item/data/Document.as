//
// $Id$

package com.threerings.msoy.item.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * A digital item representing a simple text document.
 */
public class Document extends MediaItem
{
    /** The title of this document (max length 255 characters). */
    public var title :String;

    override public function getType () :String
    {
        return "DOCUMENT";
    }

    override public function getInventoryDescrip () :String
    {
        return toInventoryDescrip(title);
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(title);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        title = (ins.readField(String) as String);
    }
}
}
