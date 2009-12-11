//
// $Id: MediaDesc.as 18283 2009-10-06 20:57:49Z jamie $

package com.threerings.msoy.data.all {

import flash.utils.ByteArray;

import com.threerings.util.Hashable;
import com.threerings.util.Util;
import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * A class containing metadata about a media object.
 */
public class MediaDescBase implements Hashable
{
    /** The SHA-1 hash of this media's data. */
    public var hash :ByteArray;

    /** The MIME type of the media associated with this item. */
    public var mimeType :int;

    /**
     * Convert the specified media hash into a String
     */
    public static function hashToString (hash :ByteArray) :String
    {
        return StringUtil.hexlate(hash);
    }

    /**
     * Convert the specified String back into a media hash.
     */
    public static function stringToHash (hash :String) :ByteArray
    {
        return StringUtil.unhexlate(hash);
    }

    /**
     * Creates either a configured or blank media descriptor.
     */
    public function MediaDescBase (hash :ByteArray = null, mimeType :int = 0)
    {
        this.hash = hash;
        this.mimeType = mimeType;
    }

    /**
     * Is this media purely audio?
     */
    public function isAudio () :Boolean
    {
        return MediaMimeTypes.isAudio(mimeType);
    }

    /**
     * Returns the URL that references this media. Tip: if you are ever calling getMediaPath()
     * you are probably doing something wrong. It's intended only for end-level things that are
     * geared towards actually displaying the media. Media should almost always be displayed in
     * some subclass of MsoyMediaContainer so that it can be bleeped.
     */
    public function getMediaPath () :String
    {
        if (hash == null) {
            return null;
        }
        return DeploymentConfig.mediaURL + hashToString(hash) +
            MediaMimeTypes.mimeTypeToSuffix(mimeType);
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
    public function hashCode () :int
    {
        var code :int = 0;
        for (var ii :int = Math.min(3, hash.length - 1); ii >= 0; ii--) {
            code = (code << 8) | hash[ii];
        }
        return code;
    }

    // documentation inherited from Hashable
    public function equals (other :Object) :Boolean
    {
        if (other is MediaDescBase) {
            var that :MediaDescBase = (other as MediaDescBase);
            return (this.mimeType == that.mimeType) &&
                Util.equals(this.hash, that.hash);
        }
        return false;
    }

    // from Object
    public function toString () :String
    {
        return hashToString(hash) + ":" + mimeType;
    }
}
}
