//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * A digital item representing a simple text document.
 */
public class Document extends Item
{
    /** The document media. */
    public var docMedia :MediaDesc;

    /** The title of this document (max length 255 characters). */
    public var title :String;

    override public function getType () :int
    {
        return DOCUMENT;
    }

    override public function getDescription () :String
    {
        return title;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(docMedia);
        out.writeField(title);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        docMedia = (ins.readObject() as MediaDesc);
        title = (ins.readField(String) as String);
    }
}
}
