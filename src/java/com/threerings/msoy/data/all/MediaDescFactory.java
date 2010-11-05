//
// $Id: $


package com.threerings.msoy.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 *
 */
public class MediaDescFactory
{
    public static HashMediaDesc createMediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        return new HashMediaDesc(hash, mimeType, constraint);
    }

    public static HashMediaDesc createHashMediaDesc (byte[] hash, byte mimeType)
    {
        return createHashMediaDesc(hash, mimeType, MediaDesc.NOT_CONSTRAINED);
    }

    public static HashMediaDesc createHashMediaDesc (String s, byte mimeType, byte constraint)
    {
        return new HashMediaDesc(HashMediaDesc.stringToHash(s), mimeType, constraint);
    }

    public static HashMediaDesc createHashMediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        return new HashMediaDesc(hash, mimeType, constraint);
    }

    public static HashMediaDesc createHashMediaDesc (String filename)
    {
        return new HashMediaDesc(
            HashMediaDesc.stringToHash(filename.substring(0, filename.indexOf('.'))),
             MediaMimeTypes.suffixToMimeType(filename),
            MediaDesc.NOT_CONSTRAINED);
    }

    /**
     * Creates and returns a media descriptor if the supplied hash is non-null, returns onNull
     * otherwise.
     */
    public static MediaDesc make (byte[] hash, byte mimeType, MediaDesc onNull)
    {
        return (hash == null) ? onNull : createHashMediaDesc(hash, mimeType);
    }

    /**
     * Creates and returns a media descriptor if the supplied hash is non-null, returns onNull
     * otherwise.
     */
    public static MediaDesc make (byte[] hash, byte mimeType, byte constraint, MediaDesc onNull)
    {
        return (hash == null) ? onNull :
            createMediaDesc(hash, mimeType, constraint);
    }

    /**
     * Creates a MediaDesc from a colon delimited String.
     */
    public static HashMediaDesc stringToMD (String str)
    {
        String[] data = str.split(":");
        if (data.length != 3) {
            return null;
        }
        byte[] hash = HashMediaDesc.stringToHash(data[0]);
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

        return createMediaDesc(hash, mimeType, constraint);
    }
}
