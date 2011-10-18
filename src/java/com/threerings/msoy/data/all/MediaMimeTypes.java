package com.threerings.msoy.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * Mime-type specific utility for the {@link com.threerings.orth.data.MediaDesc} family of classes.
 */
public abstract class MediaMimeTypes
{
    /** The unsupported MIME types. */
    public static final byte INVALID_MIME_TYPE = 0;
    /** The MIME type for plain UTF-8 text. */
    public static final byte TEXT_PLAIN = 1;
    /** The MIME type for Flash ActionScript files. */
    public static final byte TEXT_ACTIONSCRIPT = 2;
    /** The MIME type for PNG image data. */
    public static final byte IMAGE_PNG = 10;
    /** The MIME type for JPEG image data. */
    public static final byte IMAGE_JPEG = 11;
    /** The MIME type for GIF image data. */
    public static final byte IMAGE_GIF = 12;
    /** The MIME type for MPEG audio data. */
    public static final byte AUDIO_MPEG = 20;
    /** The MIME type for FLV video data. */
    public static final byte VIDEO_FLASH = 30;
    /** The MIME type for MPEG video data. */
    public static final byte VIDEO_MPEG = 31;
    /** The MIME type for Quicktime video data. */
    public static final byte VIDEO_QUICKTIME = 32;
    /** The MIME type for AVI video data. */
    public static final byte VIDEO_MSVIDEO = 33;
    /** The MIME type for Flash SWF files. */
    public static final byte APPLICATION_SHOCKWAVE_FLASH = 40;
    /** The MIME type for Java JAR files. */
    public static final byte APPLICATION_JAVA_ARCHIVE = 41;
    /** The MIME type for ZIP files. */
    public static final byte APPLICATION_ZIP = 42;
    /** The MIME type for Action Script ABC files. */
    public static final byte COMPILED_ACTIONSCRIPT_LIBRARY = 43;
    /** The MIME type for ZIP files that have been marked "no remix". */
    public static final byte APPLICATION_ZIP_NOREMIX = 44;
    /** MIME type for CSS. */
    public static final byte TEXT_CSS = 45;
    /** The MIME type for youtube video. */
    public static final byte EXTERNAL_YOUTUBE = 100;

    /**
     * Maps the supplied string representation of a mime type to our internal
     * integer code. Returns INVALID_MIME_TYPE if the mime type is unknown.
     */
    public static byte stringToMimeType (String mimeType)
    {
        if (mimeType == null) {
            return INVALID_MIME_TYPE;
        }
        mimeType = mimeType.toLowerCase();
        if (mimeType.equals("text/plain")) {
            return TEXT_PLAIN;
        } else if (mimeType.equals("text/x-actionscript")) {
            return TEXT_ACTIONSCRIPT;
        } else if (mimeType.equals("image/png")) {
            return IMAGE_PNG;
        } else if (mimeType.equals("image/jpeg")) {
            return IMAGE_JPEG;
        } else if (mimeType.equals("image/gif")) {
            return IMAGE_GIF;
        } else if (mimeType.equals("audio/mpeg")) {
            return AUDIO_MPEG;
            //        } else if (mimeType.equals("audio/wav")) {
            //            return AUDIO_WAV;
        } else if (mimeType.equals("video/flash")) {
            return VIDEO_FLASH;
            //        } else if (mimeType.equals("video/mpeg")) {
            //            return VIDEO_MPEG;
            //        } else if (mimeType.equals("video/quicktime")) {
            //            return VIDEO_QUICKTIME;
            //        } else if (mimeType.equals("video/msvideo")) {
            //            return VIDEO_MSVIDEO;
        } else if (mimeType.equals("application/x-shockwave-flash")) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else if (mimeType.equals("application/java-archive")) {
            return APPLICATION_JAVA_ARCHIVE;
        } else if (mimeType.equals("application/zip")) {
            return APPLICATION_ZIP;
        } else if (mimeType.equals("application/x-actionscript-bytecode")) {
            return COMPILED_ACTIONSCRIPT_LIBRARY;
        } else if (mimeType.equals("external/youtube")) {
            return EXTERNAL_YOUTUBE;
        } else if (mimeType.equals("text/css")) {
            return TEXT_CSS;
        } else {
            return INVALID_MIME_TYPE;
        }
    }

    /**
     * Maps the supplied filename suffix to a mime type. Returns INVALID_MIME_TYPE if the
     * suffix is unknown.
     */
    public static byte suffixToMimeType (String filename)
    {
        // note: this works if a full filename/url is passed, or just the extension
        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        int ix = ext.indexOf('?');
        if (ix > 0) {
            ext = ext.substring(0, ix);
        }
        ext = ext.toLowerCase();

        if ("txt".equals(ext)) {
            return TEXT_PLAIN;
        } else if ("as".equals(ext)) {
            return TEXT_ACTIONSCRIPT;
        } else if ("png".equals(ext)) {
            return IMAGE_PNG;
        } else if ("jpg".equals(ext)) {
            return IMAGE_JPEG;
        } else if ("gif".equals(ext)) {
            return IMAGE_GIF;
        } else if ("mp3".equals(ext)) {
            return AUDIO_MPEG;
            //        } else if ("wav".equals(ext)) {
            //            return AUDIO_WAV;
        } else if ("flv".equals(ext)) {
            return VIDEO_FLASH;
            //        } else if ("mpg".equals(ext)) {
            //            return VIDEO_MPEG;
            //        } else if ("mov".equals(ext)) {
            //            return VIDEO_QUICKTIME;
            //        } else if ("avi".equals(ext)) {
            //            return VIDEO_MSVIDEO;
        } else if ("swf".equals(ext)) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else if ("jar".equals(ext)) {
            return APPLICATION_JAVA_ARCHIVE;
        } else if ("zip".equals(ext)) {
            return APPLICATION_ZIP;
        } else if ("abc".equals(ext)) {
            return COMPILED_ACTIONSCRIPT_LIBRARY;
        } else if ("css".equals(ext)) {
            return TEXT_CSS;
        } else if ("e00".equals(ext)) {
            return EXTERNAL_YOUTUBE;
        } else {
            return INVALID_MIME_TYPE;
        }
    }

    /**
     * Returns a file suffix for use with the specified mime tpye or .dat if
     * mime type is unknown.
     */
    public static String mimeTypeToSuffix (byte mimeType)
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
    public static String mimeTypeToString (byte mimeType)
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
        case COMPILED_ACTIONSCRIPT_LIBRARY: return "application/x-actionscript-bytecode";
        case TEXT_CSS: return "text/css";
        case EXTERNAL_YOUTUBE: return "external/youtube";
        default: return "application/octet-stream";
        }
    }

    /**
     * Returns true if the supplied mime type is a supported image type.
     */
    public static boolean isImage (byte mimeType)
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

    public static boolean isAudio (byte mimeType)
    {
        switch (mimeType) {
        case MediaMimeTypes.AUDIO_MPEG:
//        case AUDIO_WAV:
            return true;

        default:
            return false;
        }
    }

    public static boolean isVideo (byte mimeType)
    {
        switch (mimeType) {
        case MediaMimeTypes.VIDEO_FLASH:
        case MediaMimeTypes.EXTERNAL_YOUTUBE:
//        case VIDEO_MPEG:
//        case VIDEO_QUICKTIME:
//        case VIDEO_MSVIDEO:
            return true;

        default:
            return false;
        }
    }

    public static boolean isExternal (byte mimeType)
    {
        switch (mimeType) {
        case MediaMimeTypes.EXTERNAL_YOUTUBE:
            return true;

        default:
            return false;
        }
    }

    /**
     * Returns true if the supplied mimeType represents a zip, basically.
     */
    public static boolean isRemixed (byte mimeType)
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
     * Returns true if the supplied mimeType represents a remixable type.
     */
    public static boolean isRemixable (byte mimeType)
    {
        return (mimeType == APPLICATION_ZIP);
    }

    /**
     * Returns the supplied media descriptor's mime type or 0 if the descriptor is null.
     */
    public static byte unmakeMimeType (MediaDesc desc)
    {
        return (desc == null) ? INVALID_MIME_TYPE : desc.getMimeType();
    }
}
