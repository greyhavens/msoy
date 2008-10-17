//
// $Id$

package com.threerings.msoy.profile.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.MemberCard;

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
}
