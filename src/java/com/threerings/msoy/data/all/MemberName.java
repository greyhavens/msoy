//
// $Id$

package com.threerings.msoy.data.all;

import java.util.Comparator;

import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

/**
 * Extends {@link Name} with persistent member information.
 */
public class MemberName extends Name
    implements DSet.Entry
{
    /** A comparator for sorting Names by their display portion, case insensitively. */
    public static final Comparator BY_DISPLAY_NAME = new Comparator() {
        public int compare (Object o1, Object o2) {
            return compare(o1, o2);
        }
    };

    /** Used to reprepsent a member that has been deleted but is still referenced as an item
     * creator or mail message sender, etc. */
    public static final MemberName DELETED_MEMBER = new MemberName("", -1);

    /** The minimum allowable length of a permaname. */
    public static final int MINIMUM_PERMANAME_LENGTH = 4;

    /** The maximum allowable length of a permaname. */
    public static final int MAXIMUM_PERMANAME_LENGTH = 12;

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

    // from DSet.Entry
    public Comparable getKey ()
    {
        return new Integer(_memberId); // TODO: make this not suck when GWT learns not to suck
    }

    // @Override // from Name
    public int hashCode ()
    {
        // we return a different hash for guests so that they don't end up all in the same bucket
        // in a Map.
        return (_memberId != GUEST_ID) ? _memberId : super.hashCode();
    }

    // @Override // from Name
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

    // @Override // from Name
    public int compareTo (Name o)
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

    // @Override // from Name
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
