//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Date;

import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.msoy.web.data.Profile;

/**
 * Contains a single row from the PROFILES table.
 */
public class ProfileRecord extends PersistentRecord
{
    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The unique id of the memory with whom this profile is associated. */
    @Id public int memberId;

    /** The member's home page URL (maxlen: 255). */
    public String homePageURL;

    /** A short bit of text provided by the member (maxlen: 255). */
    public String headline;

    /** Whether the member identifies as male or female. */
    public boolean isMale;

    /** The date on which the member claims to be born. */
    @Column(nullable=true)
    public Date birthday;

    /** The locale from which the member claims to hail (maxlen: 255). */
    public String location;

    public ProfileRecord ()
    {
    }

    /**
     * Creates a profile record from the supplied (user provided) profile data.
     */
    public ProfileRecord (int memberId, Profile profile)
    {
        this.memberId = memberId;
        homePageURL = StringUtil.deNull(profile.homePageURL);
        headline = StringUtil.deNull(profile.headline);
        isMale = profile.isMale;
        // birthday = profile.birthday; // TODO
        location = StringUtil.deNull(profile.location);
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public Profile toProfile ()
    {
        Profile profile = new Profile();
        profile.homePageURL = homePageURL;
        profile.headline = headline;
        profile.isMale = isMale;
        // profile.birthday = birthday;
        profile.location = location;
        return profile;
    }
}
