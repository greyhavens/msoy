//
// $Id$

package com.threerings.msoy.data.all {

import flash.geom.Point;

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
public class MediaDesc
    implements Streamable, Hashable
{
    /** The unsupported MIME types. */
    public static const INVALID_MIME_TYPE :int = 0;

    /** The MIME type for plain UTF-8 text. */
    public static const TEXT_PLAIN :int = 1;

    /** The MIME type for Flash ActionScript files. */
    public static const TEXT_ACTIONSCRIPT :int = 2;

    /** The MIME type for PNG image data. */
    public static const IMAGE_PNG :int = 10;

    /** The MIME type for JPEG image data. */
    public static const IMAGE_JPEG :int = 11;

    /** The MIME type for GIF image data. */
    public static const IMAGE_GIF :int = 12;

    /** The MIME type for MPEG audio data. */
    public static const AUDIO_MPEG :int = 20;

//    /** The MIME type for WAV audio data. */
//    public static final byte AUDIO_WAV = 21;

    /** The MIME type for FLV video data. */
    public static const VIDEO_FLASH :int = 30;

    /** The MIME type for MPEG video data. */
    public static const VIDEO_MPEG :int = 31;

    /** The MIME type for Quicktime video data. */
    public static const VIDEO_QUICKTIME :int = 32;

    /** The MIME type for AVI video data. */
    public static const VIDEO_MSVIDEO :int = 33;

    /** The MIME type for a youtube video. */
    public static const VIDEO_YOUTUBE_DEPRECATED :int = 34;

    /** The MIME type for Flash SWF files. */
    public static const APPLICATION_SHOCKWAVE_FLASH :int = 40;

    /** The MIME type for Java JAR files. */
    public static const APPLICATION_JAVA_ARCHIVE :int = 41;

    /** The MIME type for ZIP files. */
    public static const APPLICATION_ZIP :int = 42;

    /** Identifies that a "quarter thumbnail" sized image is desired. */
    public static const QUARTER_THUMBNAIL_SIZE :int = 0;

    /** Identifies that a "half thumbnail" sized image is desired. */
    public static const HALF_THUMBNAIL_SIZE :int = 1;

    /** Identifies that a thumbnail sized image is desired. */
    public static const THUMBNAIL_SIZE :int = 2;

    /** Identifies that a preview sized image is desired. */
    public static const PREVIEW_SIZE :int = 3;

    /** The thumbnail image width.  */
    public static const THUMBNAIL_WIDTH :int = 80;

    /** The thumbnail image height.  */
    public static const THUMBNAIL_HEIGHT :int = 60;

    /** Defines the dimensions of our various image sizes. */
    public static const DIMENSIONS :Array = [
        THUMBNAIL_WIDTH/4, THUMBNAIL_HEIGHT/4, // quarter thumbnail size
        THUMBNAIL_WIDTH/2, THUMBNAIL_HEIGHT/2, // half thumbnail size
        THUMBNAIL_WIDTH,   THUMBNAIL_HEIGHT,   // thumbnail size
        THUMBNAIL_WIDTH*4, THUMBNAIL_HEIGHT*4, // preview size
    ];

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

    /** The SHA-1 hash of this media's data. */
    public var hash :ByteArray;

    /** The MIME type of the media associated with this item. */
    public var mimeType :int;

    /** The size constraint on this media, if any. See {@link #computeConstraint}. */
    public var constraint :int;

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
     * Maps the supplied string representation of a mime type to our internal
     * integer code. Returns INVALID_MIME_TYPE if the mime type is unknown.
     */
    public static function stringToMimeType (mimeType :String) :int
    {
        if (mimeType == null) {
            return INVALID_MIME_TYPE;
        }
        mimeType = mimeType.toLowerCase();
        if (mimeType == "text/plain") {
            return TEXT_PLAIN;
        } else if (mimeType == "text/x-actionscript") {
            return TEXT_ACTIONSCRIPT;
        } else if (mimeType == "image/png") {
            return IMAGE_PNG;
        } else if (mimeType == "image/jpeg") {
            return IMAGE_JPEG;
        } else if (mimeType == "image/gif") {
            return IMAGE_GIF;
        } else if (mimeType == "audio/mpeg") {
            return AUDIO_MPEG;
//        } else if (mimeType == "audio/wav") {
//            return AUDIO_WAV;
        } else if (mimeType == "video/flash") {
            return VIDEO_FLASH;
        } else if (mimeType == "video/mpeg") {
            return VIDEO_MPEG;
        } else if (mimeType == "video/quicktime") {
            return VIDEO_QUICKTIME;
        } else if (mimeType == "video/msvideo") {
            return VIDEO_MSVIDEO;
        } else if (mimeType == "application/x-shockwave-flash") {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else if (mimeType == "application/java-archive") {
            return APPLICATION_JAVA_ARCHIVE;
        } else if (mimeType == "application/zip") {
            return APPLICATION_ZIP;
        } else {
            return INVALID_MIME_TYPE;
        }
    }

    /**
     * Maps the supplied filename suffix to a mime type. Returns INVALID_MIME_TYPE if the
     * suffix is unknown.
     */
    public static function suffixToMimeType (filename :String) :int
    {
        filename = filename.toLowerCase();
        if (StringUtil.endsWith(filename, ".txt")) {
            return TEXT_PLAIN;
        } else if (StringUtil.endsWith(filename, ".as")) {
            return TEXT_ACTIONSCRIPT;
        } else if (StringUtil.endsWith(filename, ".png")) {
            return IMAGE_PNG;
        } else if (StringUtil.endsWith(filename, ".jpg")) {
            return IMAGE_JPEG;
        } else if (StringUtil.endsWith(filename, ".gif")) {
            return IMAGE_GIF;
        } else if (StringUtil.endsWith(filename, ".mp3")) {
            return AUDIO_MPEG;
//        } else if (StringUtil.endsWith(filename, ".wav")) {
//            return AUDIO_WAV;
        } else if (StringUtil.endsWith(filename, ".flv")) {
            return VIDEO_FLASH;
        } else if (StringUtil.endsWith(filename, ".mpg")) {
            return VIDEO_MPEG;
        } else if (StringUtil.endsWith(filename, ".mov")) {
            return VIDEO_QUICKTIME;
        } else if (StringUtil.endsWith(filename, ".avi")) {
            return VIDEO_MSVIDEO;
        } else if (StringUtil.endsWith(filename, ".swf")) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else if (StringUtil.endsWith(filename, ".jar")) {
            return APPLICATION_JAVA_ARCHIVE;
        } else if (StringUtil.endsWith(filename, ".zip")) {
            return APPLICATION_ZIP;
        } else {
            return INVALID_MIME_TYPE;
        }
    }

    /**
     * Returns a file suffix for use with the specified mime tpye or .dat if
     * mime type is unknown.
     */
    public static function mimeTypeToSuffix (mimeType :int) :String
    {
        switch (mimeType) {
        case TEXT_PLAIN: return ".txt";
        case TEXT_ACTIONSCRIPT: return ".as";
        case IMAGE_PNG: return ".png";
        case IMAGE_JPEG: return ".jpg";
        case IMAGE_GIF: return ".gif";
        case AUDIO_MPEG: return ".mp3";
//        case AUDIO_WAV: return ".wav";
        case VIDEO_FLASH: return ".flv";
        case VIDEO_MPEG: return ".mpg";
        case VIDEO_QUICKTIME: return ".mov";
        case VIDEO_MSVIDEO: return ".avi";
        case APPLICATION_SHOCKWAVE_FLASH: return ".swf";
        case APPLICATION_JAVA_ARCHIVE: return ".jar";
        case APPLICATION_ZIP: return ".zip";
        default: return ".dat";
        }
    }

    /**
     * Maps the supplied integer representation of a mime type to the standard string
     * representation. Returns "application/octet-stream".
     */
    public static function mimeTypeToString (mimeType :int) :String
    {
        switch (mimeType) {
        case TEXT_PLAIN: return "text/plain";
        case TEXT_ACTIONSCRIPT: return "text/x-actionscript";
        case IMAGE_PNG: return "image/png";
        case IMAGE_JPEG: return "image/jpeg";
        case IMAGE_GIF: return "image/gif";
        case AUDIO_MPEG: return "audio/mpeg";
//        case AUDIO_WAV: return "audo/wav";
        case VIDEO_FLASH: return "video/flash";
        case VIDEO_MPEG: return "video/mpeg";
        case VIDEO_QUICKTIME: return "video/quicktime";
        case VIDEO_MSVIDEO: return "video/msvideo";
        case APPLICATION_SHOCKWAVE_FLASH: return "application/x-shockwave-flash";
        case APPLICATION_JAVA_ARCHIVE: return "application/java-archive";
        case APPLICATION_ZIP: return "application/zip";
        default: return "application/octet-stream";
        }
    }

    /**
     * Is this media merely an image type?
     */
    public static function isImage (mimeType :int) :Boolean
    {
        switch (mimeType) {
        case IMAGE_PNG:
        case IMAGE_JPEG:
        case IMAGE_GIF:
            return true;

        default:
            return false;
        }
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
     * Creates either a configured or blank media descriptor.
     */
    public function MediaDesc (
        hash :ByteArray = null, mimeType :int = 0, constraint :int = NOT_CONSTRAINED)
    {
        this.hash = hash;
        this.mimeType = mimeType;
        this.constraint = constraint;
    }

    /**
     * Is this media purely audio?
     */
    public function isAudio () :Boolean
    {
        switch (mimeType) {
        case AUDIO_MPEG:
//        case AUDIO_WAV:
            return true;

        default:
            return false;
        }
    }

    /**
     * Returns the URL that references this media.
     */
    public function getMediaPath () :String
    {
        if (hash == null) {
            return null;
        }

        switch (mimeType) {
        case VIDEO_YOUTUBE_DEPRECATED:
            return "http://www.youtube.com/v/" + StringUtil.fromBytes(hash);

        default:
            return DeploymentConfig.mediaURL + hashToString(hash) + mimeTypeToSuffix(mimeType);
        }
    }

    /**
     * Is this media remixable?
     */
    public function isRemixable () :Boolean
    {
        return (mimeType == APPLICATION_ZIP);
    }

    /**
     * Is this media merely an image type?
     */
    public function isImage () :Boolean
    {
        switch (mimeType) {
        case IMAGE_PNG:
        case IMAGE_JPEG:
        case IMAGE_GIF:
            return true;

        default:
            return false;
        }
    }

    /**
     * Return true if this media has a visual component that can be shown
     * in flash.
     */
    public function hasFlashVisual () :Boolean
    {
        switch (mimeType) {
        case IMAGE_PNG:
        case IMAGE_JPEG:
        case IMAGE_GIF:
        case VIDEO_FLASH:
        case VIDEO_YOUTUBE_DEPRECATED:
        case APPLICATION_SHOCKWAVE_FLASH:
            return true;

        default:
            return false;
        }
    }

    /**
     * Is this video that we host?
     */
     public function isWhirledVideo () :Boolean
     {
        switch (mimeType) {
        case VIDEO_FLASH:
        case VIDEO_MPEG:
        case VIDEO_QUICKTIME:
        case VIDEO_MSVIDEO:
            return true;

        default:
            return false;
        }
    }

    /**
     * Is this video hosted externally?
     */
     // TODO: remove this
     public function isExternalVideo () :Boolean
     {
        switch (mimeType) {
        case VIDEO_YOUTUBE_DEPRECATED:
            return true;

        default:
            return false;
        }
    }

    /**
     * Is this media video?
     */
    public function isVideo () :Boolean
    {
        return isWhirledVideo() || isExternalVideo();
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
        if (other is MediaDesc) {
            var that :MediaDesc = (other as MediaDesc);
            return (this.mimeType == that.mimeType) &&
                Util.equals(this.hash, that.hash);
        }
        return false;
    }

    // from Object
    public function toString () :String
    {
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

    /**
     * Get some identifier that can be used to refer to this media across
     * sessions (used as a key in prefs).
     */
    public function getMediaId () :String
    {
        switch (mimeType) {
        case VIDEO_YOUTUBE_DEPRECATED:
            return "_" + mimeType + ":" + StringUtil.fromBytes(hash);

        default:
            return hashToString(hash);
        }
    }

    /**
     * @return true if the media is clickable.
     */
    public function isInteractive () :Boolean
    {
        // TODO: this may need to be more complicated in the future
        switch (mimeType) {
        case APPLICATION_SHOCKWAVE_FLASH:
        case VIDEO_YOUTUBE_DEPRECATED:
            return true;

        default:
            return false;
        }
    }

    /**
     * Helper function for {@link #equals} because we must work in JavaScript land.
     */
    protected static function arraysEqual (a1 :ByteArray, a2 :ByteArray) :Boolean
    {
        throw new Error("Unimplemented");
    }
}
}
