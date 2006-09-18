//
// $Id$

package com.threerings.msoy.item.web {

import flash.utils.ByteArray;

import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * The base class for all digital items that have associated static media.
 */
public /*abstract*/ class MediaItem extends Item
{
    /** A hash code identifying the media associated with this item. */
    public var mediaHash :ByteArray;

    /** The MIME type of the media associated with this item. */
    public var mimeType :int;

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(mediaHash);
        out.writeByte(mimeType);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        mediaHash = (ins.readField(ByteArray) as ByteArray);
        mimeType = ins.readByte();
    }
}
}
