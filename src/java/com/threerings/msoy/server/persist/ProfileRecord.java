//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import com.threerings.msoy.web.data.Profile;

/**
 * Contains a single row from the PROFILES table.
 */
public class ProfileRecord
{
    /** The unique id of the memory with whom this profile is associated. */
    public int memberId;

    /** The member's home page URL (maxlen: 255). */
    public String homePageURL;

    /** A short bit of text provided by the member (maxlen: 255). */
    public String headline;

    /** Whether the member identifies as male or female. */
    public boolean isMale;

    /** The date on which the member claims to be born. */
    public Date birthday;

    /** The locale from which the member claims to hail (maxlen: 255). */
    public String location;

    /**
     * Populates the supplied profile object with our data.
     */
    public void populateProfile (Profile profile)
    {
        profile.homePageURL = homePageURL;
        profile.headline = headline;
        profile.isMale = isMale;
        // TODO: profile.age = toAge(birthday);
        profile.location = location;
    }
}
