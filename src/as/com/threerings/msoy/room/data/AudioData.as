//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;

/**
 * Contains immutable information on background audio in a scene.
 */
public class AudioData extends SimpleStreamableObject
{
    /** Identifies the id of the item that was used to create this. */
    public var itemId :int;

    /** Media contained in the audio item. */
    public var media :MediaDesc;

//    /** Audio volume. */
//    public var volume :Number;

    /**
     * Constructor.
     */
    public function AudioData (itemId :int = 0, media :MediaDesc = null)
    {
        this.itemId = itemId;
        this.media = media;
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(itemId);
        out.writeObject(media);
        //out.writeFloat(volume);
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        itemId = ins.readInt();
        media = MediaDesc(ins.readObject());
        //volume = ins.readFloat();
    }
}
}
