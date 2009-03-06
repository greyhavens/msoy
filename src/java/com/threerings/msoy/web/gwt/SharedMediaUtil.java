//
// $Id$

package com.threerings.msoy.web.gwt;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Utility routines for displaying media or generating html code to display media.
 */
public class SharedMediaUtil
{
    /**
     * Represents the size of an html image (<img/>).
     */
    public static class Dimensions
    {
        /**
         * Creates a new size.
         */
        public Dimensions (String width, String height)
        {
            this.width = width;
            this.height = height;
        }

        /** The width of the image. */
        public String width;

        /** The height of the image. */
        public String height;
    }

    /**
     * Resolves the correct width and height for embedding an image media descriptor as an html
     * image. If no specific values are required, null is returned.
     */
    public static Dimensions resolveImageSize (MediaDesc desc, int width, int height)
    {
        switch (desc.constraint) {
        case MediaDesc.HALF_HORIZONTALLY_CONSTRAINED:
            if (width < MediaDesc.THUMBNAIL_WIDTH) {
                return new Dimensions(width + "px", "auto");
            }
            break;

        case MediaDesc.HALF_VERTICALLY_CONSTRAINED:
            if (height < MediaDesc.THUMBNAIL_HEIGHT) {
                return new Dimensions("auto", height + "px");
            }
            break;

        case MediaDesc.HORIZONTALLY_CONSTRAINED:
            return new Dimensions(width + "px", "auto");

        case MediaDesc.VERTICALLY_CONSTRAINED:
            return new Dimensions("auto", height + "px");
        }
        return null;
    }
}
