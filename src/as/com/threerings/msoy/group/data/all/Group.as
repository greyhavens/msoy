//
// $Id$

package com.threerings.msoy.group.data.all {

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;

/**
 * A mere shadow of the java code.
 */
public class Group
{
    /**
     * Ensure that the specified MediaDesc is a group logo. If it's null, the
     * default logo is returned.
     */
    public static function logo (desc :MediaDesc) :MediaDesc
    {
        return (desc != null) ? desc : getDefaultGroupLogoMedia();
    }

    /**
     * Creates a default logo for use with groups that have no logo.
     */
    public static function getDefaultGroupLogoMedia () :MediaDesc
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, "photo", "group_logo",
            MediaDesc.HALF_VERTICALLY_CONSTRAINED);
    }
}
}
