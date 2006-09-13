//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends MediaItem
{
    /** A caption for this photo (max length 255 characters). */
    public var caption :String;

    // from Item
    override public function getType () :String
    {
        return "PHOTO";
    }

    // from Item
    override public function getInventoryDescrip () :String
    {
        return toInventoryDescrip(caption);
    }

    // from Item
    override public function getThumbnailPath () :String
    {
        return getMediaPath();
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(caption);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        caption = (ins.readField(String) as String);
    }
}
}
