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
            return compareNames((MemberName)o1, (MemberName)o2);
        }
    };

    /**
     * Compares two member name records case insensitively.
     */
    public static int compareNames (MemberName m1, MemberName m2)
    {
        return m1.toString().toLowerCase().compareTo(m2.toString().toLowerCase());
    }

    /** Used to reprepsent a member that has been deleted but is still referenced as an item
     * creator or mail message sender, etc. */
    public static final MemberName DELETED_MEMBER = new MemberName("", -1);

    /** The minimum allowable length of a permaname. */
    public static final int MINIMUM_PERMANAME_LENGTH = 4;

    /** The maximum allowable length of a permaname. */
    public static final int MAXIMUM_PERMANAME_LENGTH = 12;

    /**
     * Returns true if the supplied member id represents a guest rather than a registered user.
     */
    public static boolean isGuest (int memberId)
    {
        return memberId <= 0;
    }

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
     * Return the memberId of this user, positive if they are a registered member, negative if they
     * are a guest.
     */
    public int getMemberId ()
    {
        return _memberId;
    }

    /**
     * Returns true if this name represents a guest member.
     */
    public boolean isGuest ()
    {
        return isGuest(_memberId);
    }

    // from DSet.Entry
    public Comparable getKey ()
    {
        return new Integer(_memberId); // TODO: make this not suck when GWT learns not to suck
    }

    // @Override // from Name
    public int hashCode ()
    {
        return _memberId;
    }

    // @Override // from Name
    public boolean equals (Object other)
    {
        if (other instanceof MemberName) {
            return ((MemberName) other).getMemberId() == _memberId;
        } else {
            return false;
        }
    }

    // @Override // from Name
    public int compareTo (Name o)
    {
        return _memberId - ((MemberName) o)._memberId;
    }

    // @Override // from Name
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    /** The member id of the member we represent. */
    protected int _memberId;
}
