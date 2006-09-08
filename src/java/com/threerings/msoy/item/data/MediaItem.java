//
// $Id$

package com.threerings.msoy.item.data;

/**
 * The base class for all digital items that have associated static media.
 */
public abstract class MediaItem extends Item
{
    /** The MIME type for plain UTF-8 text. */
    public static final byte TEXT_PLAIN = 0;

    /** The MIME type for PNG image data. */
    public static final byte IMAGE_PNG = 10;

    /** The MIME type for JPEG image data. */
    public static final byte IMAGE_JPEG = 11;

    /** The MIME type for GIF image data. */
    public static final byte IMAGE_GIF = 12;

    /** The MIME type for MPEG audio data. */
    public static final byte AUDIO_MPEG = 20;

    /** The MIME type for WAV audio data. */
    public static final byte AUDIO_WAV = 21;

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

    /** A hash code identifying the media associated with this item. */
    public byte[] mediaHash;

    /** The MIME type of the media associated with this item. */
    public byte mimeType;

    /**
     * Returns the path of the URL that references this media.
     */
    public String getMediaPath ()
    {
        return getMediaPath(mediaHash, mimeType);
    }

    // REMOVE
    public String getHashAsString ()
    {
        return hashToString(mediaHash);
    }

    /**
     * Set the hash and mimetype of this item.
     */
    public void setHash (String strHash, byte newMimeType)
    {
        mediaHash = stringToHash(strHash);
        mimeType = newMimeType;
    }

    /**
     * Get the path of the URL for the media specified.
     */
    public static String getMediaPath (byte[] mediaHash, byte mimeType)
    {
        return "/media/" + hashToString(mediaHash) +
            mimeTypeToSuffix(mimeType);
    }

    /**
     * Convert the specified media hash into a String.
     */
    public static String hashToString (byte[] hash)
    {
        if (hash == null) {
            return "";
        }
        char[] chars= new char[hash.length * 2];
        for (int ii = 0; ii < hash.length; ii++) {
            int val = hash[ii];
            if (val < 0) {
                val += 256;
            }
            chars[2 * ii] = HEX.charAt(val/16);
            chars[2 * ii + 1] = HEX.charAt(val%16);
        }
        return new String(chars);
    }

    /**
     * Convert the specified String back into a media hash.
     */
    public static byte[] stringToHash (String hash)
    {
        if (hash == null || hash.length() % 2 != 0) {
            return null;
        }

        hash = hash.toLowerCase();
        byte[] data = new byte[hash.length() / 2];
        for (int ii = 0; ii < hash.length(); ii += 2) {
            int value = (byte) (HEX.indexOf(hash.charAt(ii)) << 4);
            value += HEX.indexOf(hash.charAt(ii + 1));

            // values over 127 are wrapped around, restoring negative bytes
            data[ii / 2] = (byte) value;
        }

        return data;
    }

    /**
     * Maps the supplied string representation of a mime type to our internal
     * integer code. Returns -1 if the mime type is unknown.
     */
    public static int stringToMimeType (String mimeType)
    {
        mimeType = mimeType.toLowerCase();
        if (mimeType.equals("text/plain")) {
            return TEXT_PLAIN;
        } else if (mimeType.equals("image/png")) {
            return IMAGE_PNG;
        } else if (mimeType.equals("image/jpeg")) {
            return IMAGE_JPEG;
        } else if (mimeType.equals("image/gif")) {
            return IMAGE_GIF;
        } else if (mimeType.equals("audio/mpeg")) {
            return AUDIO_MPEG;
        } else if (mimeType.equals("audio/wav")) {
            return AUDIO_WAV;
        } else if (mimeType.equals("video/flash")) {
            return VIDEO_FLASH;
        } else if (mimeType.equals("video/mpeg")) {
            return VIDEO_MPEG;
        } else if (mimeType.equals("video/quicktime")) {
            return VIDEO_QUICKTIME;
        } else if (mimeType.equals("video/msvideo")) {
            return VIDEO_MSVIDEO;
        } else if (mimeType.equals("application/x-shockwave-flash")) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else {
            return -1;
        }
    }

    /**
     * Maps the supplied filename suffix to a mime type. Returns -1 if the
     * suffix is unknown.
     */
    public static int suffixToMimeType (String filename)
    {
        filename = filename.toLowerCase();
        if (filename.endsWith(".txt")) {
            return TEXT_PLAIN;
        } else if (filename.endsWith(".png")) {
            return IMAGE_PNG;
        } else if (filename.endsWith(".jpg")) {
            return IMAGE_JPEG;
        } else if (filename.endsWith(".gif")) {
            return IMAGE_GIF;
        } else if (filename.endsWith(".mp3")) {
            return AUDIO_MPEG;
        } else if (filename.endsWith(".wav")) {
            return AUDIO_WAV;
        } else if (filename.endsWith(".flv")) {
            return VIDEO_FLASH;
        } else if (filename.endsWith(".mpg")) {
            return VIDEO_MPEG;
        } else if (filename.endsWith(".mov")) {
            return VIDEO_QUICKTIME;
        } else if (filename.endsWith(".avi")) {
            return VIDEO_MSVIDEO;
        } else if (filename.endsWith(".swf")) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else {
            return -1;
        }
    }

    /**
     * Returns a file suffix for use with the specified mime tpye or .dat if
     * mime type is unknown.
     */
    public static String mimeTypeToSuffix (int mimeType)
    {
        switch (mimeType) {
        case TEXT_PLAIN: return ".txt";
        case IMAGE_PNG: return ".png";
        case IMAGE_JPEG: return ".jpg";
        case IMAGE_GIF: return ".gif";
        case AUDIO_MPEG: return ".mp3";
        case AUDIO_WAV: return ".wav";
        case VIDEO_FLASH: return ".flv";
        case VIDEO_MPEG: return ".mpg";
        case VIDEO_QUICKTIME: return ".mov";
        case VIDEO_MSVIDEO: return ".avi";
        case APPLICATION_SHOCKWAVE_FLASH: return ".swf";
        default: return ".dat";
        }
    }

    /** Hexidecimal digits. */
    protected static final String HEX = "0123456789abcdef";
}
