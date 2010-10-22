//
// $Id$

package com.threerings.msoy.group.data.all {

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.MediaDescImpl;
import com.threerings.msoy.data.all.MediaMimeTypes;
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
        return new StaticMediaDesc(MediaMimeTypes.IMAGE_PNG, "photo", "group_logo",
            MediaDescImpl.HALF_VERTICALLY_CONSTRAINED);
    }
}
}
