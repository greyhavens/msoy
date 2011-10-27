//
// $Id$

package com.threerings.msoy.data.all;

import com.google.common.base.Predicate;

import com.threerings.util.Name;

import com.threerings.orth.data.OrthName;

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
public class MemberName extends OrthName
{
    /** The minimum allowable length of a permaname. */
    public static final int MINIMUM_PERMANAME_LENGTH = 4;

    /** The maximum allowable length of a permaname. */
    public static final int MAXIMUM_PERMANAME_LENGTH = 12;

    /** The minimum length for a display name. */
    public static final int MIN_DISPLAY_NAME_LENGTH = 3;

    /** The maximum length for a display name. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 30;

    /** Characters (above ' ') that are illegal in display names.
     */
    public static final String ILLEGAL_DISPLAY_NAME_CHARS = "\u007F" + // renders invisible in flash
        "\uFFFC\uFFFD" + // used by whirled for delimiting links in chat
        "\u202A\u202B\u202C\u202D\u202E\u200C\u200D\u200E\u200F" + // control text direction
        "\u534D\u5350\u0FCC"; // swastikas

    /** The maximum length for an account name (email address). */
    public static final int MAX_EMAIL_LENGTH = 128;

    /** The maximum length for a user's real name. */
    public static final int MAX_REALNAME_LENGTH = 128;

    /** Used to reprepsent a member that has been deleted but is still referenced as an item
     * creator or mail message sender, etc. */
    public static final MemberName DELETED_MEMBER = new MemberName("", -1);

    /** A Predicate, assigned only on the server, that tests whether a char is whitespace. */
    public static Predicate<Character> isSpacePred;

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
        return (name != null && name.trim().equals(name) &&
                name.length() >= MIN_DISPLAY_NAME_LENGTH &&
                name.length() <= MAX_DISPLAY_NAME_LENGTH &&
                containsOnlyLegalChars(name));
    }

    /**
     * Called after {@link #isValidDisplayName}, if the user is not support.
     */
    public static boolean isValidNonSupportName (String name)
    {
        name = name.toLowerCase();

        // There have been tons of phishing attacks lately posing as Aduros, nip that in the bud
        if (((isPossibleA(name.charAt(0)) && name.startsWith("duros", 1)))
                || name.startsWith("cleaver")) {
            return false;
        }

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
     * Create a new MemberName.
     */
    public MemberName (String displayName, int memberId)
    {
        super(displayName, memberId);
    }

    /**
     * Returns true if this name represents an anonymous viewer.
     */
    public boolean isViewer ()
    {
        return isViewer(_id);
    }

    /**
     * Ensure that this object is just a base MemberName, and not an extended class with
     * additional information. This is sometimes desired when sending names "over the wire".
     */
    public MemberName toMemberName ()
    {
        return this;
    }

    // from DSet.Entry

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
        if (isSpacePred == null) {
            // we do this in GWT
            return Character.isSpace(c);
        }
        // we do this on the server
        return isSpacePred.apply(c);
    }

    /** Helper for {@link #isValidDisplayName}. */
    protected static boolean containsOnlyLegalChars (String name)
    {
        boolean visible = false;
        for (int ii = 0, nn = name.length(); ii < nn; ii++) {
            char c = name.charAt(ii);
            if ((c < ' ') || (-1 != ILLEGAL_DISPLAY_NAME_CHARS.indexOf(c))) {
                return false;
            }
            visible = visible || !isWhitespace(c);
        }
        return visible;
    }

    /** Helper for {@link #isValidNonSupportName}. */
    protected static boolean isPossibleA (char c)
    {
        // it's an A, or it's something unicodey: kill; I don't want to enumerate all possible
        // unicode A characters
        return (c == 'a') || (c > 127);
    }
}
