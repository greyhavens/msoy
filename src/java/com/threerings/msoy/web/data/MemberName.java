//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Comparator;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Extends {@link Name} with persistent member information.
 */
public class MemberName extends Name
    implements IsSerializable
{
    /** A comparator for sorting Names by their display portion, case insensitively. */
    public static final Comparator BY_DISPLAY_NAME = new Comparator() {
        public int compare (Object o1, Object o2) {
            return compare((MemberName)o1, (MemberName)o2);
        }
    };

    /** The "member id" used for guests. */
    public static final int GUEST_ID = 0;

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
     * Return the memberId of this user, or 0 if they're a guest.
     */
    public int getMemberId ()
    {
        return _memberId;
    }

    // @Override
    public int hashCode ()
    {
        // we return a different hash for guests so that they don't end up all in the same bucket
        // in a Map.
        return (_memberId != GUEST_ID) ? _memberId : super.hashCode();
    }

    // @Override
    public boolean equals (Object other)
    {
        if (other instanceof MemberName) {
            int otherId = ((MemberName) other).getMemberId();
            // if we have the same memberId then we're equals, unless we're a guest, in which case
            // we fall back to names
            return (otherId == _memberId) && ((_memberId != GUEST_ID) || super.equals(other));
        } else {
            return false;
        }
    }

    // @Override
    public int compareTo (Object o)
    {
        MemberName that = (MemberName) o;
        int diff = this._memberId - that._memberId;
        // memberId is the primary sorting key
        if (diff != 0) {
            return diff;
        }

        // return 0 if diff is the same (they have the same memberId) UNLESS the memberId is 0, in
        // which case they're a guest and we compare by name
        return (_memberId != GUEST_ID) ? 0 : compare(this, that);
    }

    // @Override
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    /**
     * Compares two member name records case insensitively.
     */
    protected static int compare (MemberName m1, MemberName m2)
    {
        return m1.toString().toLowerCase().compareTo(m2.toString().toLowerCase());
    }

    /** The member id of the member we represent. */
    protected int _memberId;
}
