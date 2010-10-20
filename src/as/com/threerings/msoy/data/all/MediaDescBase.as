//
// $Id: MediaDesc.as 18283 2009-10-06 20:57:49Z jamie $

package com.threerings.msoy.data.all {

import flash.utils.ByteArray;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Equalable;
import com.threerings.util.Hashable;
import com.threerings.util.StringUtil;
import com.threerings.util.Util;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * A class containing metadata about a media object.
 */
public /* abstract */ class MediaDescBase
    implements Streamable, Equalable, EntityMedia
{
    /** The MIME type of the media associated with this item. */
    public var mimeType :int;

    /**
     * Creates either a configured or blank media descriptor.
     */
    public function MediaDescBase (mimeType :int = 0)
    {
        this.mimeType = mimeType;
    }

    // from EntityMedia
    public function getMimeType () :int
    {
        return mimeType;
    }

    // from EntityMedia
    public function getMediaPath () :String
    {
        throw new Error("abstract");
    }

    /**
     * Is this media purely audio?
     */
    public function isAudio () :Boolean
    {
        return MediaMimeTypes.isAudio(mimeType);
    }

    /**
     * Is this media merely an image type?
     */
    public function isImage () :Boolean
    {
        return MediaMimeTypes.isImage(mimeType);
    }

    /**
     * Is this media a SWF?
     */
    public function isSWF () :Boolean
    {
        return (mimeType == MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH);
    }

    /**
     * Is this media video?
     */
    public function isVideo () :Boolean
    {
        return MediaMimeTypes.isVideo(mimeType);
    }

    public function isExternal () :Boolean
    {
        return MediaMimeTypes.isExternal(mimeType);
    }

    // documentation inherited from Hashable
    public function equals (other :Object) :Boolean
    {
        return (other is MediaDescBase) && this.mimeType == (other as MediaDescBase).mimeType;
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        mimeType = ins.readByte();
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeByte(mimeType);
    }
}
}
