//
// $Id$

package com.threerings.msoy.profile.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.MemberCard;

/**
 * Contains all member profile data for display on the profile page.
 */
public class Profile implements IsSerializable
{
    /** The maximum length for a member's status. */
    public static final int MAX_STATUS_LENGTH = 100;

    /** The maximum length for a member's homepage. */
    public static final int MAX_HOMEPAGE_LENGTH = 100;

    /** A {@link #sex} constant. */
    public static final byte SEX_UNSPECIFIED = 0;

    /** A {@link #sex} constant. */
    public static final byte SEX_MALE = 1;

    /** A {@link #sex} constant. */
    public static final byte SEX_FEMALE = 2;

    /** The the member's selected profile picture. */
    public MediaDesc photo = MemberCard.DEFAULT_PHOTO;

    /** A member provided profile status. */
    public String headline;

    /** A member provided homepage URL. */
    public String homePageURL;

    /** This member's level. */
    public int level;

    /** The time at which this member registered. */
    public long memberSince;

    /** The time at which the member last logged on. 0L means online now, -1L means they have never
     * logged on. */
    public long lastLogon;

    /** This member's birthday (year, month, day). Only provided for the member themselves. */
    public int[] birthday;

    /** The member's professed age. A/S/L's A. */
    public int age;

    /** The member's professed sex. A/S/L's S. */
    public byte sex;

    /** The member's professed location.  A/S/L's L. */
    public String location;

    /** True if this member has a blog. */
    public boolean hasBlog;

    /** True if this member has a photo gallery. */
    public boolean hasGallery;

    /** The user's permaName */
    public String permaName;

    /**
     * Returns true if the supplied display name meets our length requirements. If we add other
     * requirements in the future, we can enforce those here as well.
     */
    public static boolean isValidDisplayName (String name)
    {
        return (name != null && name.length() >= MemberName.MIN_DISPLAY_NAME_LENGTH &&
                name.length() <= MemberName.MAX_DISPLAY_NAME_LENGTH);
    }

    /**
     * Called after isValidDisplayName, if the user is not support.
     */
    @SuppressWarnings("deprecation") // GWT needs isSpace(), but it's deprecated in favor of
    // isWhitespace() in the Java library.
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
            if (dex != -1 && Character.isSpace(name.charAt(dex - 2)) &&
                    isPossibleA(name.charAt(dex - 1))) {
                return false;
            }
            lastDex = dex + 1;
        } while (lastDex > 0);
        return true;
    }

    /** Helper for isValidNonSupportName. */
    private static boolean isPossibleA (char c)
    {
        // it's an A, or it's something unicodey: kill.
        // I don't want to enumerate all possible unicode A characters.
        return (c == 'a') || (c > 127);
    }
}
