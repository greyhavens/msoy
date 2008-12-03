//
// $Id$

package com.threerings.msoy.data.all;


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

    public VizMemberName (MemberName name, MediaDesc photo)
    {
        super(name.toString(), name.getMemberId());
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
