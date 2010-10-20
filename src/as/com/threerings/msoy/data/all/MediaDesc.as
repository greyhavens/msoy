//
// $Id$

package com.threerings.msoy.data.all {

import flash.utils.ByteArray;

import com.threerings.util.Equalable;
import com.threerings.util.Util;
import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * A class containing metadata about a media object.
 */
public /* abstract */ class MediaDesc extends MediaDescBase
    implements Streamable, Equalable
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
     * Creates either a configured or blank media descriptor.
     */
    public function MediaDesc (mimeType :int = 0, constraint :int = NOT_CONSTRAINED)
    {
        super(mimeType);
        this.constraint = constraint;
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

    // documentation inherited from Hashable
    override public function equals (other :Object) :Boolean
    {
        return (other is MediaDesc) && super.equals(other)
            && this.constraint == (other as MediaDesc).constraint;
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        constraint = ins.readByte();
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(constraint);
    }
}
}
