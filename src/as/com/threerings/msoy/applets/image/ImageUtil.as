//
// $Id$

package com.threerings.msoy.applets.image {

public class ImageUtil
{
    public static function normalizeImageDimension (n :int) :int
    {
        // images cannot be larger than 2880 in any direction.
        return Math.max(1, Math.min(2880, n));
    }
}
}
