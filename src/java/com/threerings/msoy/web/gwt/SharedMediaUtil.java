//
// $Id$

package com.threerings.msoy.web.gwt;

import com.threerings.msoy.data.all.ConstrainedMediaDesc;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MediaDescSize;

/**
 * Utility routines for displaying media or generating html code to display media.
 */
public class SharedMediaUtil
{
    /** Represents the size of an html image (&lt;img/&gt;). */
    public static class Dimensions
    {
        /** The width of the image. */
        public final String width;

        /** The height of the image. */
        public final String height;

        public Dimensions (String width, String height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Resolves the correct width and height for embedding an image media descriptor as an html
     * image. If no specific values are required, null is returned.
     */
    public static Dimensions resolveImageSize (MediaDesc desc, int width, int height)
    {
        if (!(desc instanceof ConstrainedMediaDesc)) {
            return null;
        }
        switch (((ConstrainedMediaDesc)desc).getConstraint()) {
        case ConstrainedMediaDesc.HALF_HORIZONTALLY_CONSTRAINED:
            return (width < MediaDescSize.THUMBNAIL_WIDTH) ?
                new Dimensions(width + "px", "auto") : null;

        case ConstrainedMediaDesc.HALF_VERTICALLY_CONSTRAINED:
            return (height < MediaDescSize.THUMBNAIL_HEIGHT) ?
                new Dimensions("auto", height + "px") : null;

        case ConstrainedMediaDesc.HORIZONTALLY_CONSTRAINED:
            return new Dimensions(width + "px", "auto");

        case ConstrainedMediaDesc.VERTICALLY_CONSTRAINED:
            return new Dimensions("auto", height + "px");

        default:
            return null;
        }
    }
}
