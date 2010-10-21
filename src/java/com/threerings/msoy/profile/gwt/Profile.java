//
// $Id$

package com.threerings.msoy.profile.gwt;

import com.threerings.orth.data.MediaDesc;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebCreds;

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

    /** The member's selected profile picture. */
    public MediaDesc photo = MemberCard.DEFAULT_PHOTO;

    /** The member's role. */
    public WebCreds.Role role;

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

    /** This member's birthday (year, month, day). Only provided for yourself & for support. */
    public int[] birthday;

    /** The member's professed age. A/S/L's A. */
    public int age;

    /** The member's professed sex. A/S/L's S. */
    public byte sex;

    /** The member's professed location.  A/S/L's L. */
    public String location;

    /** The user's permaName */
    public String permaName;

    /** The award to be displayed on this player's profile. */
    public Award award;
}
