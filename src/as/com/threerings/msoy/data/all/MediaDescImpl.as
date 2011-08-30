//
// $Id: MediaDesc.as 19417 2010-10-20 20:52:22Z zell $

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Equalable;

import com.threerings.orth.data.MediaDesc;

/**
 * A class containing metadata about a media object.
 */
public /* abstract */ class MediaDescImpl
    implements MediaDesc, Streamable, Equalable
{
    /** A constant used to indicate that an image does not exceed half thumbnail size in either
     * dimension. */
    public static const NOT_CONSTRAINED :int = 0;

    /** A constant used to indicate that an image exceeds thumbnail size proportionally more in the
     * horizontal dimension. */
    public static const HORIZONTALLY_CONSTRAINED :int = 1;

    /** A constant used to indicate that an image exceeds thumbnail size proportionally more in the
     * vertical dimension. */
    public static const VERTICALLY_CONSTRAINED :int = 2;

    /** A constant used to indicate that an image exceeds half thumbnail size proportionally more
     * in the horizontal dimension but does not exceed thumbnail size in either dimension. */
    public static const HALF_HORIZONTALLY_CONSTRAINED :int = 3;

    /** A constant used to indicate that an image exceeds half thumbnail size proportionally more
     * in the vertical dimension but does not exceed thumbnail size in either dimension. */
    public static const HALF_VERTICALLY_CONSTRAINED :int = 4;

    /** The size constraint on this media, if any. See {@link #computeConstraint}. */
    public function get constraint () :int
    {
        return _constraint;
    }

    /** The MIME type of the media associated with this item. */
    public function get mimeType () :int
    {
        return _mimeType;
    }

    /**
     * Creates either a configured or blank media descriptor.
     */
    public function MediaDescImpl (mimeType :int = 0, constraint :int = NOT_CONSTRAINED)
    {
        _mimeType = mimeType;
        _constraint = constraint;
    }

    // from EntityMedia
    public function getMimeType () :int
    {
        return _mimeType;
    }

    // from EntityMedia
    public function getMediaPath () :String
    {
        throw new Error("abstract");
    }

    /**
     * Get some identifier that can be used to refer to this media across
     * sessions (used as a key in prefs).
     */
    public /* abstract */ function getMediaId () :String
    {
        throw new Error("abstract");
    }

    // from MediaDesc
    public function isBleepable () :Boolean
    {
        throw new Error("abstract");
    }

    /**
     * Is this media purely audio?
     */
    public function isAudio () :Boolean
    {
        return MediaMimeTypes.isAudio(_mimeType);
    }

    /**
     * Is this media merely an image type?
     */
    public function isImage () :Boolean
    {
        return MediaMimeTypes.isImage(_mimeType);
    }

    /**
     * Is this media a SWF?
     */
    public function isSWF () :Boolean
    {
        return (_mimeType == MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH);
    }

    /**
     * Is this media video?
     */
    public function isVideo () :Boolean
    {
        return MediaMimeTypes.isVideo(_mimeType);
    }

    public function isExternal () :Boolean
    {
        return MediaMimeTypes.isExternal(_mimeType);
    }

    /**
     * Computes the constraining dimension for an image (if any) based on the supplied target and
     * actual dimensions.
     */
    public static function computeConstraint (size :int, actualWidth :int, actualHeight :int) :int
    {
        throw new Error("Unimplemented");
    }

    /**
     * Is this a zip of some sort?
     */
    public function isRemixed () :Boolean
    {
        switch (_mimeType) {
        case MediaMimeTypes.APPLICATION_ZIP:
        case MediaMimeTypes.APPLICATION_ZIP_NOREMIX:
            return true;

        default:
            return false;
        }
    }

    /**
     * Is this media remixable?
     */
    public function isRemixable () :Boolean
    {
        return (_mimeType == MediaMimeTypes.APPLICATION_ZIP);
    }

    /**
     * Return true if this media has a visual component that can be shown
     * in flash.
     */
    public function hasFlashVisual () :Boolean
    {
        switch (_mimeType) {
        case MediaMimeTypes.IMAGE_PNG:
        case MediaMimeTypes.IMAGE_JPEG:
        case MediaMimeTypes.IMAGE_GIF:
        case MediaMimeTypes.VIDEO_FLASH:
        case MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH:
        case MediaMimeTypes.EXTERNAL_YOUTUBE:
            return true;

        default:
            return false;
        }
    }

    // documentation inherited from Equalable
    public function equals (other :Object) :Boolean
    {
        return (other is MediaDesc)
            && _constraint == (other as MediaDesc).constraint
            && _mimeType == (other as MediaDesc).mimeType;
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        _mimeType = ins.readByte();
        _constraint = ins.readByte();
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeByte(_mimeType);
        out.writeByte(_constraint);
    }

    protected var _mimeType :int;

    protected var _constraint :int;
}
}
