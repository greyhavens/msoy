//
// $Id$

package com.threerings.msoy.item.web {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * A digital item representing a simple text document.
 */
public class Document extends Item
{
    /** A hash code identifying the document media. */
    public var docMediaHash :ByteArray;

    /** The MIME type of the {@link #docMediaHash} media. */
    public var docMimeType :int;

    /** The title of this document (max length 255 characters). */
    public var title :String;

    /**
     * Returns a media descriptor for the actual document media.
     */
    public function getDocumentMedia () :MediaDesc
    {
        return new MediaDesc(docMediaHash, docMimeType);
    }

    override public function getType () :String
    {
        return "DOCUMENT";
    }

    override public function getDescription () :String
    {
        return title;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(docMediaHash);
        out.writeByte(docMimeType);
        out.writeField(title);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        docMediaHash = (ins.readField(ByteArray) as ByteArray);
        docMimeType = ins.readByte();
        title = (ins.readField(String) as String);
    }
}
}
