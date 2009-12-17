//
// $Id$

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
public class MediaDesc extends MediaDescBase
    implements Streamable
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
    public var constraint :int;

    /**
     * Computes the constraining dimension for an image (if any) based on the supplied target and
     * actual dimensions.
     */
    public static function computeConstraint (size :int, actualWidth :int, actualHeight :int) :int
    {
        throw new Error("Unimplemented");
    }

    /**
     * Creates a MediaDesc from a colon-delimited String.
     */
    public static function stringToMD (str :String) :MediaDesc
    {
        var data :Array = str.split(":");
        if (data.length != 3) {
            return null;
        }

        var hash :ByteArray = stringToHash(data[0]);
        if (hash == null) {
            return null;
        }
        var mimeType :int = parseInt(data[1]);
        var constraint :int = parseInt(data[2]);
        return new MediaDesc(hash, mimeType, constraint);
    }

    /**
     * Creates either a configured or blank media descriptor.
     */
    public function MediaDesc (
        hash :ByteArray = null, mimeType :int = 0, constraint :int = NOT_CONSTRAINED)
    {
        super(hash, mimeType);
        this.constraint = constraint;
    }

    /**
     * Is this a zip of some sort?
     */
    public function isRemixed () :Boolean
    {
        switch (mimeType) {
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
        return (mimeType == MediaMimeTypes.APPLICATION_ZIP);
    }

    /**
     * Return true if this media has a visual component that can be shown
     * in flash.
     */
    public function hasFlashVisual () :Boolean
    {
        switch (mimeType) {
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

    /**
     * Is this media bleepable?
     */
    public function isBleepable () :Boolean
    {
        return (hash != null);
    }

    /**
     * Get some identifier that can be used to refer to this media across
     * sessions (used as a key in prefs).
     */
    public function getMediaId () :String
    {
        return hashToString(hash);
    }

    // documentation inherited from Hashable
    override public function equals (other :Object) :Boolean
    {
        if (other is MediaDesc) {
            var that :MediaDesc = (other as MediaDesc);
            return super.equals(other) && this.constraint == that.constraint;
        }
        return false;
    }

    // from Object
    override public function toString () :String
    {
        // Note: stringToMD() above relies on this precise format.
        return hashToString(hash) + ":" + mimeType + ":" + constraint;
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        hash = (ins.readField(ByteArray) as ByteArray);
        mimeType = ins.readByte();
        constraint = ins.readByte();
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(hash);
        out.writeByte(mimeType);
        out.writeByte(constraint);
    }
}
}
