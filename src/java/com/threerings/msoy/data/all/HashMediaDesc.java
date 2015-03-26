package com.threerings.msoy.data.all;

import com.threerings.orth.data.MediaDesc;

public class HashMediaDesc extends BasicMediaDesc
{
    /** The SHA-1 hash of this media's data. */
    public byte[] hash;

    /**
     * Get the path of the URL for the media specified.
     */
    public static String getMediaPath (byte[] mediaHash, byte mimeType)
    {
        return getMediaPath(DeploymentConfig.mediaURL, mediaHash, mimeType);
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

    /**
     * Creates a MediaDesc from a colon delimited String.
     */
    public static HashMediaDesc stringToHMD (String str)
    {
        String[] data = str.split(":");
        if (data.length != 3) {
            return null;
        }
        byte[] hash = stringToHash(data[0]);
        if (hash == null) {
            return null;
        }
        byte mimeType = MediaMimeTypes.INVALID_MIME_TYPE;
        byte constraint = 0;
        try {
            mimeType = Byte.parseByte(data[1]);
        } catch (NumberFormatException nfe) {
            // don't care
        }
        try {
            constraint = Byte.parseByte(data[2]);
        } catch (NumberFormatException nfe) {
            // don't care
        }

        return new HashMediaDesc(hash, mimeType, constraint);
    }

    /**
     * Converts a HashMediaDesc into a colon delimited String.
     */
    public static String hmdToString (HashMediaDesc hmd)
    {
        return new StringBuilder(hashToString(hmd.hash))
            .append(":").append(hmd.getMimeType())
            .append(":").append(hmd.getConstraint())
            .toString();
    }

    /**
     * Creates a HashMediaDesc based on a string hash.
     */
    public static HashMediaDesc create (String s, byte mimeType, byte constraint)
    {
        return new HashMediaDesc(stringToHash(s), mimeType, constraint);
    }

    /**
     * Returns the supplied media descriptor's hash or null if the descriptor is null. This
     * method throws an error if the supplied argument is not a {@link HashMediaDesc}.
     */
    public static byte[] unmakeHash (MediaDesc desc)
    {
        if (desc != null) {
            if (desc instanceof HashMediaDesc) {
                return ((HashMediaDesc) desc).hash;
            }
            throw new IllegalArgumentException(
                "Expecting hash-based media descriptor [" + desc + "=" + desc + "]");
        }
        return null;
    }

    /** For serialization. */
    public HashMediaDesc ()
    {
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public HashMediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        super(mimeType, constraint);
        this.hash = hash;
    }

    /* (non-Javadoc)
     * @see com.threerings.msoy.data.all.MediaDesc#getMediaPath()
     */
    public String getMediaPath ()
    {
        return HashMediaDesc.getMediaPath(hash, getMimeType());
    }

    // from MediaDesc
    public MediaDesc newWithConstraint (byte constraint)
    {
        return new HashMediaDesc(hash, getMimeType(), constraint);
    }

    public String getProxyMediaPath ()
    {
        return getMediaPath(DeploymentConfig.PROXY_PREFIX, hash, getMimeType());
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof HashMediaDesc) && super.equals(other) &&
            HashMediaDesc.arraysEqual(this.hash, ((HashMediaDesc) other).hash);
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
        return HashMediaDesc.hashToString(hash) + MediaMimeTypes.mimeTypeToSuffix(getMimeType());
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

    /** Hexadecimal digits. */
    protected static final String HEX = "0123456789abcdef";
}
