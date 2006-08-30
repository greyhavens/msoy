//
// $Id$

package com.threerings.msoy.data;

import java.util.Comparator;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Extends Name with persistent member information.
 */
public class MemberName extends Name
    implements IsSerializable
{
    /** A comparator for sorting Names by their display portion, case
     * insensitively. */
    public static final Comparator<Name> BY_DISPLAY_NAME =
        new Comparator<Name>() {
            public int compare (Name o1, Name o2)
            {
                return String.CASE_INSENSITIVE_ORDER.compare(
                    o1.toString(), o2.toString());
            }
        };

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
        // we return a different hash for guests so that they
        // don't end up all in the same bucket in a Map.
        return (_memberId != -1) ? _memberId : super.hashCode();
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
    public int compareTo (Object o)
    {
        MemberName that = (MemberName) o;
        int diff = this._memberId - that._memberId;
        // memberId is the primary sorting key
        if (diff != 0) {
            return diff;
        }

        // return 0 if diff is the same (they have the same memberId)
        // UNLESS the memberId is -1, in which case they're a guest
        // and we compare by name
        return (_memberId != -1) ? 0 : BY_DISPLAY_NAME.compare(this, that);
    }

    @Override
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    /** The member id of the member we represent. */
    protected int _memberId;
}
