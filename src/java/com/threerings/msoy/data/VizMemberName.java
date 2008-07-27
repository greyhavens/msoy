//
// $Id$

package com.threerings.msoy.data;


import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

/**
 * A member name and profile photo all rolled into one!
 */
public class VizMemberName extends MemberName
{
    /** For unserialization. */
    public VizMemberName ()
    {
    }

    /**
     * Creates a new name with the supplied data.
     */
    public VizMemberName (String displayName, int memberId, MediaDesc photo)
    {
        super(displayName, memberId);
        _photo = photo;
    }

    /**
     * Returns this member's photo.
     */
    public MediaDesc getPhoto ()
    {
        return _photo;
    }

    /** This member's profile photo. */
    protected MediaDesc _photo;
}
