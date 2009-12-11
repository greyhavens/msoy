package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

public class MediaDescBase implements IsSerializable
{
    /** The SHA-1 hash of this media's data. */
    public byte[] hash;

    /** The MIME type of the media associated with this item. */
    public byte mimeType;

    /**
     * Get the path of the URL for the media specified.
     */
    public static String getMediaPath (byte[] mediaHash, byte mimeType)
    {
        return getMediaPath(DeploymentConfig.mediaURL, mediaHash, mimeType);
    }

    /**
     * Get the path of the URL for the media specified.
     */
    protected static String getMediaPath (String prefix, byte[] mediaHash, byte mimeType)
    {
        if (mediaHash == null) {
            return null;
        }

        return prefix + hashToString(mediaHash) + MediaMimeTypes.mimeTypeToSuffix(mimeType);
    }

    /**
     * Convert the specified media hash into a String.
     */
    public static String hashToString (byte[] hash)
    {
        if (hash == null) {
            return "";
        }
        char[] chars = new char[hash.length * 2];
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

    /** Used for deserialization. */
    public MediaDescBase ()
    {
    }

    /**
     * Create a media descriptor from the specified info. Note
     * that the String will be turned into a byte[] differently
     * depending on the mimeType.
     */
    public MediaDescBase (String s, byte mimeType)
    {
        this(stringToHash(s), mimeType);
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public MediaDescBase (byte[] hash, byte mimeType)
    {
        this.hash = hash;
        this.mimeType = mimeType;
    }

    /**
     * Returns the path of the URL that references this media.
     */
    public String getMediaPath ()
    {
        return getMediaPath(hash, mimeType);
    }

    /**
     * Is this media merely an image type?
     */
    public boolean isImage ()
    {
        return MediaMimeTypes.isImage(mimeType);
    }

    /**
     * Is this media a SWF?
     */
    public boolean isSWF ()
    {
        return (mimeType == MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH);
    }

    /**
     * Is this media purely audio?
     */
    public boolean isAudio ()
    {
        return MediaMimeTypes.isAudio(mimeType);
    }

    /**
     * Is this media video?
     */
    public boolean isVideo ()
    {
        return MediaMimeTypes.isVideo(mimeType);
    }

    public boolean isExternal ()
    {
        return MediaMimeTypes.isExternal(mimeType);
    }


    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof MediaDesc) {
            MediaDesc that = (MediaDesc) other;
            return (this.mimeType == that.mimeType) && arraysEqual(this.hash, that.hash);
        }
        return false;
    }

    @Override // from Object
    public int hashCode ()
    {
        int code = 0;
        for (int ii = Math.min(3, hash.length - 1); ii >= 0; ii--) {
            code <<= 8;
            code |= hash[ii];
        }
        return code;
    }

    @Override // from Object
    public String toString ()
    {
        return hashToString(hash) + MediaMimeTypes.mimeTypeToSuffix(mimeType);
    }

    /**
     * Helper function for {@link #equals} because we must work in JavaScript
     * land.
     */
    protected static boolean arraysEqual (byte[] left, byte[] right)
    {
        if (left == right) {
            return true;
        }
        if (left == null || right == null || left.length != right.length) {
            return false;
        }
        for (int ii = 0; ii < left.length; ii++) {
            if (left[ii] != right[ii]) {
                return false;
            }
        }
        return true;
    }

    /** Hexadecimal digits. */
    protected static final String HEX = "0123456789abcdef";
}
