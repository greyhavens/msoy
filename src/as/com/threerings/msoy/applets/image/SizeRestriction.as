//
// $Id$

package com.threerings.msoy.applets.image {

import flash.geom.Point;

public class SizeRestriction
{
    /** The forced size, or null if none. */
    public var forced :Point;

    /** Maximum width, or NaN if none. */
    public var maxWidth :Number;

    /** Maximum height, or NaN if none. */
    public var maxHeight :Number;

    public function SizeRestriction (
        forcedWidth :Number = NaN, forcedHeight :Number = NaN,
        maxWidth :Number = NaN, maxHeight :Number = NaN)
    {
        if (!isNaN(forcedWidth) && !isNaN(forcedHeight)) {
            forced = new Point(forcedWidth, forcedHeight);
            this.maxWidth = forcedWidth;
            this.maxHeight = forcedHeight;

        } else {
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }
    }

    public function isValid (width :Number, height :Number) :Boolean
    {
        if (forced != null) {
            return (forced.x == width) && (forced.y == height);

        } else {
            return (isNaN(maxWidth) || (width < maxWidth)) &&
                (isNaN(maxHeight) || (height < maxHeight));
        }
    }
}
}
