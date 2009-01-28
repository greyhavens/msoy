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

    /** The MIME type for Flash SWF files. */
    public static const APPLICATION_SHOCKWAVE_FLASH :int = 40;

    /** The MIME type for Java JAR files. */
    public static const APPLICATION_JAVA_ARCHIVE :int = 41;

    /** The MIME type for ZIP files. */
    public static const APPLICATION_ZIP :int = 42;

    /** The MIME type for ActionScript ABC files. */
    public static const COMPILED_ACTIONSCRIPT_LIBRARY :int = 43;

    /** The MIME type for ZIP files that have been marked "no remix". */
    public static const APPLICATION_ZIP_NOREMIX :int = 44;

    /** The MIME type for youtube video. */
    public static const EXTERNAL_YOUTUBE :int = 100;

    /** Identifies that a "quarter thumbnail" sized image is desired. */
    public static const QUARTER_THUMBNAIL_SIZE :int = 0;

    /** Identifies that a "half thumbnail" sized image is desired. */
    public static const HALF_THUMBNAIL_SIZE :int = 1;

    /** Identifies that a thumbnail sized image is desired. */
    public static const THUMBNAIL_SIZE :int = 2;

    /** Identifies that a preview sized image is desired. */
    public static const PREVIEW_SIZE :int = 3;

    /** The "thumbnail" size for scene snapshots. */
    public static const SNAPSHOT_THUMB_SIZE :int = 4;

    /** The full size for canonical scene snapshots. */
    public static const SNAPSHOT_FULL_SIZE :int = 5;

    /** The full size for game screenshots. */
    public static const GAME_SHOT_SIZE :int = 6;

    /** The smallest size of room snapshots. */
    public static const SNAPSHOT_TINY_SIZE :int = 7;

    /** Identifies the game splash logo size. */
    public static const GAME_SPLASH_SIZE :int = 8;

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
        175, 100, // scene snapshot thumb size
        350, 200, // full scene snapshot image size
        175, 125, // game screenshots
         40,  23, // tiny snapshots, same width as half thumbnail
        700, 500, // game splash image, same as the min game window
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
//        } else if (mimeType == "video/mpeg") {
//            return VIDEO_MPEG;
//        } else if (mimeType == "video/quicktime") {
//            return VIDEO_QUICKTIME;
//        } else if (mimeType == "video/msvideo") {
//            return VIDEO_MSVIDEO;
        } else if (mimeType == "application/x-shockwave-flash") {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else if (mimeType == "application/java-archive") {
            return APPLICATION_JAVA_ARCHIVE;
        } else if (mimeType == "application/zip") {
            return APPLICATION_ZIP;
        } else if (mimeType == "external/youtube") {
            return EXTERNAL_YOUTUBE;
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
        // note: this works if a full filename/url is passed, or just the extension
        var ext :String = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (ext) {
        case "txt": return TEXT_PLAIN;
        case "as": return TEXT_ACTIONSCRIPT;
        case "png": return IMAGE_PNG;
        case "jpg": return IMAGE_JPEG;
        case "gif": return IMAGE_GIF;
        case "mp3": return AUDIO_MPEG;
//        case "wav": return AUDIO_WAV;
        case "flv": return VIDEO_FLASH;
//        case "mpg": return VIDEO_MPEG;
//        case "mov": return VIDEO_QUICKTIME;
//        case "avi": return VIDEO_MSVIDEO;
        case "swf": return APPLICATION_SHOCKWAVE_FLASH;
        case "jar": return APPLICATION_JAVA_ARCHIVE;
        case "zip": return APPLICATION_ZIP;
        case "e00": return EXTERNAL_YOUTUBE;
        default: return INVALID_MIME_TYPE;
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
//        case VIDEO_MPEG: return ".mpg";
//        case VIDEO_QUICKTIME: return ".mov";
//        case VIDEO_MSVIDEO: return ".avi";
        case APPLICATION_SHOCKWAVE_FLASH: return ".swf";
        case APPLICATION_JAVA_ARCHIVE: return ".jar";
        case APPLICATION_ZIP_NOREMIX: // fall through to ZIP
        case APPLICATION_ZIP: return ".zip";
        case EXTERNAL_YOUTUBE: return ".e00";
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
//        case VIDEO_MPEG: return "video/mpeg";
//        case VIDEO_QUICKTIME: return "video/quicktime";
//        case VIDEO_MSVIDEO: return "video/msvideo";
        case APPLICATION_SHOCKWAVE_FLASH: return "application/x-shockwave-flash";
        case APPLICATION_JAVA_ARCHIVE: return "application/java-archive";
        case APPLICATION_ZIP: return "application/zip";
        case APPLICATION_ZIP_NOREMIX: return "application/zip-noremix";
        case EXTERNAL_YOUTUBE: return "external/youtube";
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
     * Returns the URL that references this media. Tip: if you are ever calling MediaDesc.getPath(),
     * you are probably doing something wrong. getPath() is only for end-level things that are
     * geared towards actually displaying the media. Media should almost always be displayed in
     * some subclass of MsoyMediaContainer so that it can be bleeped.
     */
    public function getMediaPath () :String
    {
        if (hash == null) {
            return null;
        }
        return DeploymentConfig.mediaURL + hashToString(hash) + mimeTypeToSuffix(mimeType);
    }

    /**
     * Is this a zip of some sort?
     */
    public function isRemixed () :Boolean
    {
        switch (mimeType) {
        case APPLICATION_ZIP:
        case APPLICATION_ZIP_NOREMIX:
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
     * Is this media a SWF?
     */
    public function isSWF () :Boolean
    {
        return (mimeType == APPLICATION_SHOCKWAVE_FLASH);
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
        case APPLICATION_SHOCKWAVE_FLASH:
        case EXTERNAL_YOUTUBE:
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
        switch (mimeType) {
        case VIDEO_FLASH:
        case EXTERNAL_YOUTUBE:
//        case VIDEO_MPEG:
//        case VIDEO_QUICKTIME:
//        case VIDEO_MSVIDEO:
            return true;

        default:
            return false;
        }
    }

    public function isExternal () :Boolean
    {
        switch (mimeType) {
        case EXTERNAL_YOUTUBE:
            return true;

        default:
            return false;
        }
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
        return hashToString(hash);
    }
}
}
