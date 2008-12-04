//
// $Id$

package com.threerings.msoy.data.all;

import java.util.Comparator;

import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

/**
 * Extends {@link Name} with persistent member information.
 *
 * <p> NOTE: this class (and all {@link Name} derivatives} must use custom field serializers (in
 * this case {@link MemberName_CustomFieldSerializer}) because IsSerializable only serializes the
 * fields in the class that declares that interface and all subclasses, it does not serialize
 * fields from the superclass. In this case, we have fields from our superclass that need to be
 * serialized, but we can't make {@link Name} implement IsSerializable without introducing an
 * otherwise unwanted dependency on GWT in Narya.
 *
 * <p> If you extend this class (or if you extend {@link Name}) you will have to implement a custom
 * field serializer for your derived class.
 */
public class MemberName extends Name
    implements DSet.Entry
{
    /** The minimum allowable length of a permaname. */
    public static final int MINIMUM_PERMANAME_LENGTH = 4;

    /** The maximum allowable length of a permaname. */
    public static final int MAXIMUM_PERMANAME_LENGTH = 12;

    /** The minimum length for a display name. */
    public static final int MIN_DISPLAY_NAME_LENGTH = 3;

    /** The maximum length for a display name. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 30;

    /** The maximum length for an account name (email address). */
    public static final int MAX_EMAIL_LENGTH = 128;

    /** The maximum length for a user's real name. */
    public static final int MAX_REALNAME_LENGTH = 128;

    /** Used to reprepsent a member that has been deleted but is still referenced as an item
     * creator or mail message sender, etc. */
    public static final MemberName DELETED_MEMBER = new MemberName("", -1);

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

    /**
     * Returns true if the supplied display name meets our length requirements. If we add other
     * requirements in the future, we can enforce those here as well.
     */
    public static boolean isValidDisplayName (String name)
    {
        return (name != null && name.length() >= MIN_DISPLAY_NAME_LENGTH &&
                name.length() <= MAX_DISPLAY_NAME_LENGTH);
    }

    /**
     * Called after {@link #isValidDisplayName}, if the user is not support.
     */
    public static boolean isValidNonSupportName (String name)
    {
        name = name.toLowerCase();
        if (isPossibleA(name.charAt(0)) && (
                name.startsWith("gent", 1) || name.startsWith("genl ", 1) ||
                name.startsWith("gant ", 1) || name.startsWith("gint ", 1))) {
            return false;
        }

        int lastDex = 2;
        do {
            int dex = name.indexOf("gent ", lastDex);
            if (dex != -1 && isWhitespace(name.charAt(dex - 2)) &&
                    isPossibleA(name.charAt(dex - 1))) {
                return false;
            }
            lastDex = dex + 1;
        } while (lastDex > 0);
        return true;
    }

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
    public Comparable<?> getKey ()
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
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    /**
     * GWT needs isSpace(), but it's deprecated in favor of isWhitespace() in the Java library.
     */
    @SuppressWarnings("deprecation")
    protected static boolean isWhitespace (char c)
    {
        return Character.isSpace(c);
    }

    /** Helper for {@link #isValidNonSupportName}. */
    protected static boolean isPossibleA (char c)
    {
        // it's an A, or it's something unicodey: kill; I don't want to enumerate all possible
        // unicode A characters
        return (c == 'a') || (c > 127);
    }

    /** The member id of the member we represent. */
    protected int _memberId;
}
