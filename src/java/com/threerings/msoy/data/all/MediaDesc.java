//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Contains information about a piece of media.
 */
public class MediaDesc extends MediaDescBase implements Streamable, IsSerializable
{
    /** A constant used to indicate that an image does not exceed half thumbnail size in either
     * dimension. */
    public static final byte NOT_CONSTRAINED = 0;

    /** A constant used to indicate that an image exceeds thumbnail size proportionally more in the
     * horizontal dimension. */
    public static final byte HORIZONTALLY_CONSTRAINED = 1;

    /** A constant used to indicate that an image exceeds thumbnail size proportionally more in the
     * vertical dimension. */
    public static final byte VERTICALLY_CONSTRAINED = 2;

    /** A constant used to indicate that an image exceeds half thumbnail size proportionally more
     * in the horizontal dimension but does not exceed thumbnail size in either dimension. */
    public static final byte HALF_HORIZONTALLY_CONSTRAINED = 3;

    /** A constant used to indicate that an image exceeds half thumbnail size proportionally more
     * in the vertical dimension but does not exceed thumbnail size in either dimension. */
    public static final byte HALF_VERTICALLY_CONSTRAINED = 4;

    /** The size constraint on this media, if any. See {@link #computeConstraint}. */
    public byte constraint;

    /**
     * Returns true if the supplied mimeType represents a zip, basically.
     */
    public static boolean isRemixed (byte mimeType)
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
     * Returns true if the supplied mimeType represents a remixable type.
     */
    public static boolean isRemixable (byte mimeType)
    {
        return (mimeType == MediaMimeTypes.APPLICATION_ZIP);
    }

    /**
     * Computes the constraining dimension for an image (if any) based on the supplied target and
     * actual dimensions.
     */
    public static byte computeConstraint (int size, int actualWidth, int actualHeight)
    {
        float wfactor = (float)MediaDescSize.getWidth(size) / actualWidth;
        float hfactor = (float)MediaDescSize.getHeight(size) / actualHeight;
        if (wfactor > 1 && hfactor > 1) {
            // if we're computing the size of a thumbnail image, see if it is constrained at half
            // size or still unconstrained
            if (size == MediaDescSize.THUMBNAIL_SIZE) {
                return computeConstraint(MediaDescSize.HALF_THUMBNAIL_SIZE, actualWidth, actualHeight);
            } else {
                return NOT_CONSTRAINED;
            }
        } else if (wfactor < hfactor) {
            return (size == MediaDescSize.HALF_THUMBNAIL_SIZE) ?
                HALF_HORIZONTALLY_CONSTRAINED : HORIZONTALLY_CONSTRAINED;
        } else {
            return (size == MediaDescSize.HALF_THUMBNAIL_SIZE) ?
                HALF_VERTICALLY_CONSTRAINED : VERTICALLY_CONSTRAINED;
        }
    }

    /**
     * Converts a MediaDesc into a colon delimited String.
     */
    public static String mdToString (MediaDesc md)
    {
        if (md == null || md instanceof StaticMediaDesc) {
            return "";
        }
        StringBuilder buf = new StringBuilder(hashToString(md.hash));
        buf.append(":").append(md.mimeType);
        buf.append(":").append(md.constraint);
        return buf.toString();
    }

    /**
     * Creates a MediaDesc from a colon delimited String.
     */
    public static MediaDesc stringToMD (String str)
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

        return new MediaDesc(hash, mimeType, constraint);
    }

    /**
     * Creates and returns a media descriptor if the supplied hash is non-null, returns onNull
     * otherwise.
     */
    public static MediaDesc make (byte[] hash, byte mimeType, MediaDesc onNull)
    {
        return (hash == null) ? onNull : new MediaDesc(hash, mimeType);
    }

    /**
     * Creates and returns a media descriptor if the supplied hash is non-null, returns onNull
     * otherwise.
     */
    public static MediaDesc make (byte[] hash, byte mimeType, byte constraint, MediaDesc onNull)
    {
        return (hash == null) ? onNull : new MediaDesc(hash, mimeType, constraint);
    }

    /**
     * Returns the supplied media descriptor's hash or null if the descriptor is null.
     */
    public static byte[] unmakeHash (MediaDesc desc)
    {
        return (desc == null) ? null : desc.hash;
    }

    /**
     * Returns the supplied media descriptor's mime type or 0 if the descriptor is null.
     */
    public static byte unmakeMimeType (MediaDesc desc)
    {
        return (desc == null) ? MediaMimeTypes.INVALID_MIME_TYPE : desc.mimeType;
    }

    /**
     * Returns the supplied media descriptor's constraint or 0 if the descriptor is null.
     */
    public static byte unmakeConstraint (MediaDesc desc)
    {
        return (desc == null) ? NOT_CONSTRAINED : desc.constraint;
    }

    /** Used for deserialization. */
    public MediaDesc ()
    {
        super();
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public MediaDesc (byte[] hash, byte mimeType)
    {
        this(hash, mimeType, NOT_CONSTRAINED);
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public MediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        super(hash, mimeType);
        this.constraint = constraint;
    }

    /**
     * Create a media descriptor from the specified info. Note
     * that the String will be turned into a byte[] differently
     * depending on the mimeType.
     */
    public MediaDesc (String s, byte mimeType, byte constraint)
    {
        super(s, mimeType);
        this.constraint = constraint;
    }

    /**
     * TEMPORARY CONSTRUCTOR, for making it easy for me to
     * pre-initialize some media...
     */
    public MediaDesc (String filename)
    {
        this(stringToHash(filename.substring(0, filename.indexOf('.'))),
             MediaMimeTypes.suffixToMimeType(filename));
    }

    /**
     * Returns the path of the URL that loads this media proxied through our game server so that we
     * can work around Java applet sandbox restrictions.
     */
    public String getProxyMediaPath ()
    {
        return getMediaPath(DeploymentConfig.PROXY_PREFIX, hash, mimeType);
    }

    /**
     * Return true if this media has a visual component that can be shown in
     * flash.
     */
    public boolean hasFlashVisual ()
    {
        switch (mimeType) {
        case MediaMimeTypes.IMAGE_PNG:
        case MediaMimeTypes.IMAGE_JPEG:
        case MediaMimeTypes.IMAGE_GIF:
        case MediaMimeTypes.VIDEO_FLASH:
        case MediaMimeTypes.EXTERNAL_YOUTUBE:
        case MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH:
            return true;

        default:
            return false;
        }
    }

    /**
     * Is this a zip of some sort?
     */
    public boolean isRemixed ()
    {
        return isRemixed(mimeType);
    }

    /**
     * Is this media remixable?
     */
    public boolean isRemixable ()
    {
        return isRemixable(mimeType);
    }

    @Override // from MediaDescBase
    public boolean equals (Object other)
    {
        return super.equals(other) && (other instanceof MediaDesc) &&
            (this.constraint == ((MediaDesc) other).constraint);
    }
}
