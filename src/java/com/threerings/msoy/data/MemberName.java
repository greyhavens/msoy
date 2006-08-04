//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.Name;

/**
 * Extends Name with persistent member information.
 */
public class MemberName extends Name
{
    /** For unserialization. */
    public MemberName ()
    {
    }

    /**
     * Create a new MemberName.
     */
    public MemberName (String displayName, int memberId)
    {
        super(displayName);
        _memberId = memberId;
    }

    /**
     * Return the memberId of this user, or -1 if they're a guest.
     */
    public int getMemberId ()
    {
        return _memberId;
    }

    @Override
    public int hashCode ()
    {
        return _memberId;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other != null) {
            Class c = getClass();
            Class oc = other.getClass();
            if (c == oc || c.getName().equals(oc.getName())) {
                int otherId = ((MemberName) other).getMemberId();
                // if we have the same memberId then we're equals, unless
                // we're a guest, in which case we fall back to names
                return (otherId == _memberId) &&
                    ((_memberId != -1) || super.equals(other));
            }
        }
        return false;
    }

    @Override
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    /** The member id of the member we represent. */
    protected int _memberId;
}
