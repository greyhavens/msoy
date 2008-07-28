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
    public static final Comparator<MemberName> BY_DISPLAY_NAME = new Comparator<MemberName>() {
        public int compare (MemberName name1, MemberName name2) {
            return compareNames(name1, name2);
        }
    };

    /**
     * Compares two member name records case insensitively.
     */
    public static int compareNames (MemberName m1, MemberName m2)
    {
        return m1.toString().toLowerCase().compareTo(m2.toString().toLowerCase());
    }

    /**
     * Creates a member name that can be used as a key for a DSet lookup or whereever else one might
     * need to use a {@link MemberName} instance as a key but do not have the (unneeded) member
     * name.
     */
    public static MemberName makeKey (int memberId)
    {
        return new MemberName(null, memberId);
    }

    /** Used to reprepsent a member that has been deleted but is still referenced as an item
     * creator or mail message sender, etc. */
    public static final MemberName DELETED_MEMBER = new MemberName("", -1);

    /** The minimum allowable length of a permaname. */
    public static final int MINIMUM_PERMANAME_LENGTH = 4;

    /** The maximum allowable length of a permaname. */
    public static final int MAXIMUM_PERMANAME_LENGTH = 12;

    /**
     * Returns true if the supplied member id represents an anonymous viewer.
     */
    public static boolean isViewer (int memberId)
    {
        return memberId == 0;
    }

    /**
     * Returns true if the supplied member id represents a guest or a viewer,
     * rather than a registered user.
     */
    public static boolean isGuest (int memberId)
    {
        // memberId == 0 is not technically a guest, but is used for "viewers"
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
     * Returns true if this name represents a guest member or a viewer.
     */
    public boolean isGuest ()
    {
        return isGuest(_memberId);
    }

    /**
     * Returns true if this name represents an anonymous viewer.
     */
    public boolean isViewer ()
    {
        return isViewer(_memberId);
    }

    // from DSet.Entry
    public Comparable getKey ()
    {
        return new Integer(_memberId); // TODO: make this not suck when GWT learns not to suck
    }

    @Override // from Name
    public int hashCode ()
    {
        return _memberId;
    }

    @Override // from Name
    public boolean equals (Object other)
    {
        return (other instanceof MemberName) && (((MemberName) other).getMemberId() == _memberId);
    }

    @Override // from Name
    public int compareTo (Name o)
    {
        // Note: You may be tempted to have names sort by the String value, but Names are used
        // as DSet keys in various places and so each user's must be unique.
        // Use BY_DISPLAY_NAME to sort names for display.

        // oh right, FFS, we can't use our Comparators.compare() static method for this
        // because this is compiled into GWT
        int otherId = ((MemberName) o)._memberId;
        return (_memberId > otherId) ? 1 : ((_memberId == otherId) ? 0 : -1);
    }
    
    @Override // from Name
    public String toString ()
    {
        return "[name=" + _name + ", memberId=" + _memberId + "]";
    }

    @Override // from Name
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    /** The member id of the member we represent. */
    protected int _memberId;
}
