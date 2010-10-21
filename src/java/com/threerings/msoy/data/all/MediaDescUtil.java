//
// $Id: $


package com.threerings.msoy.data.all;

/**
 *
 */
public class MediaDescUtil
{
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
                return ConstrainedMediaDesc.NOT_CONSTRAINED;
            }
        } else if (wfactor < hfactor) {
            return (size == MediaDescSize.HALF_THUMBNAIL_SIZE) ?
                ConstrainedMediaDesc.HALF_HORIZONTALLY_CONSTRAINED : ConstrainedMediaDesc.HORIZONTALLY_CONSTRAINED;
        } else {
            return (size == MediaDescSize.HALF_THUMBNAIL_SIZE) ?
                ConstrainedMediaDesc.HALF_VERTICALLY_CONSTRAINED : ConstrainedMediaDesc.VERTICALLY_CONSTRAINED;
        }
    }

    /**
     * Returns the supplied media descriptor's constraint or 0 if the descriptor is null.
     */
    public static byte unmakeConstraint (ConstrainedMediaDesc desc)
    {
        return (desc == null) ? ConstrainedMediaDesc.NOT_CONSTRAINED : desc.getConstraint();
    }
}
