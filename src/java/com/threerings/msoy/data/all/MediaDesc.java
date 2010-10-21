//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Contains information about a piece of media.
 */
public abstract class MediaDesc extends MediaDescBase implements Streamable, IsSerializable
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
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public MediaDesc (byte mimeType, byte constraint)
    {
        super(mimeType);
        this.constraint = constraint;
    }

    /**
     * Returns the path of the URL that loads this media proxied through our game server so that we
     * can work around Java applet sandbox restrictions. Subclasses may override this.
     */
    public String getProxyMediaPath ()
    {
        throw new IllegalArgumentException("Not implemented");
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
        return MediaMimeTypes.isRemixed(mimeType);
    }

    /**
     * Is this media remixable?
     */
    public boolean isRemixable ()
    {
        return MediaMimeTypes.isRemixable(mimeType);
    }

    @Override // from MediaDescBase
    public int hashCode ()
    {
        return (43 * super.hashCode()) + constraint;
    }

    @Override // from MediaDescBase
    public boolean equals (Object other)
    {
        return super.equals(other) && (other instanceof MediaDesc) &&
            (this.constraint == ((MediaDesc) other).constraint);
    }
}
