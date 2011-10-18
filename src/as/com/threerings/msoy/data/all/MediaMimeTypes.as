//
// $Id: $

package com.threerings.msoy.data.all {

import com.threerings.util.FileUtil;

/**
 * Mime-type specific utility for the {@link MediaDesc} family of classes.
 */
public class MediaMimeTypes
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

    /** MIME type for CSS. */
    public static const TEXT_CSS :int = 45;

    /** The MIME type for youtube video. */
    public static const EXTERNAL_YOUTUBE :int = 100;

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
        } else if (mimeType == "text/css") {
            return TEXT_CSS;
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
        switch (FileUtil.getDotSuffix(filename)) {
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
        case "css": return TEXT_CSS;
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
        case COMPILED_ACTIONSCRIPT_LIBRARY: return ".abc";
        case TEXT_CSS: return ".css";
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
        case TEXT_CSS: return "text/css";
        case EXTERNAL_YOUTUBE: return "external/youtube";
        default: return "application/octet-stream";
        }
    }

    public static function isAudio (mimeType :int) :Boolean
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

    public static function isVideo (mimeType :int) :Boolean
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

    public static function isExternal (mimeType :int) :Boolean
    {
        switch (mimeType) {
        case EXTERNAL_YOUTUBE:
            return true;

        default:
            return false;
        }
    }
}
}
