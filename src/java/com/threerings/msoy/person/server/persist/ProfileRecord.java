//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Date;

import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.FullTextIndex;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.Profile;

/**
 * Contains a member's profile data.
 */
@Table(fullTextIndexes={
    @FullTextIndex(name=ProfileRecord.FTS_REAL_NAME, fieldNames={ ProfileRecord.REAL_NAME })
})
public class ProfileRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(ProfileRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #modifications} field. */
    public static final String MODIFICATIONS = "modifications";

    /** The qualified column identifier for the {@link #modifications} field. */
    public static final ColumnExp MODIFICATIONS_C =
        new ColumnExp(ProfileRecord.class, MODIFICATIONS);

    /** The column identifier for the {@link #photoHash} field. */
    public static final String PHOTO_HASH = "photoHash";

    /** The qualified column identifier for the {@link #photoHash} field. */
    public static final ColumnExp PHOTO_HASH_C =
        new ColumnExp(ProfileRecord.class, PHOTO_HASH);

    /** The column identifier for the {@link #photoMimeType} field. */
    public static final String PHOTO_MIME_TYPE = "photoMimeType";

    /** The qualified column identifier for the {@link #photoMimeType} field. */
    public static final ColumnExp PHOTO_MIME_TYPE_C =
        new ColumnExp(ProfileRecord.class, PHOTO_MIME_TYPE);

    /** The column identifier for the {@link #photoConstraint} field. */
    public static final String PHOTO_CONSTRAINT = "photoConstraint";

    /** The qualified column identifier for the {@link #photoConstraint} field. */
    public static final ColumnExp PHOTO_CONSTRAINT_C =
        new ColumnExp(ProfileRecord.class, PHOTO_CONSTRAINT);

    /** The column identifier for the {@link #homePageURL} field. */
    public static final String HOME_PAGE_URL = "homePageURL";

    /** The qualified column identifier for the {@link #homePageURL} field. */
    public static final ColumnExp HOME_PAGE_URL_C =
        new ColumnExp(ProfileRecord.class, HOME_PAGE_URL);

    /** The column identifier for the {@link #headline} field. */
    public static final String HEADLINE = "headline";

    /** The qualified column identifier for the {@link #headline} field. */
    public static final ColumnExp HEADLINE_C =
        new ColumnExp(ProfileRecord.class, HEADLINE);

    /** The column identifier for the {@link #isMale} field. */
    public static final String IS_MALE = "isMale";

    /** The qualified column identifier for the {@link #isMale} field. */
    public static final ColumnExp IS_MALE_C =
        new ColumnExp(ProfileRecord.class, IS_MALE);

    /** The column identifier for the {@link #birthday} field. */
    public static final String BIRTHDAY = "birthday";

    /** The qualified column identifier for the {@link #birthday} field. */
    public static final ColumnExp BIRTHDAY_C =
        new ColumnExp(ProfileRecord.class, BIRTHDAY);

    /** The column identifier for the {@link #location} field. */
    public static final String LOCATION = "location";

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(ProfileRecord.class, LOCATION);

    /** The column identifier for the {@link #realName} field. */
    public static final String REAL_NAME = "realName";

    /** The qualified column identifier for the {@link #realName} field. */
    public static final ColumnExp REAL_NAME_C =
        new ColumnExp(ProfileRecord.class, REAL_NAME);
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on Real Name */
    public static final String FTS_REAL_NAME = "RN";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 6;

    /** The unique id of the memory with whom this profile is associated. */
    @Id public int memberId;

    /** The number of times the user has modified their profile. We need this for first time
     * profile creation purposes, so we'll have some statistical fun while we're at it. */
    public int modifications;

    /** The hash code of the user's profile photo. */
    @Column(nullable=true)
    public byte[] photoHash;

    /** The MIME type of photo image. */
    public byte photoMimeType;

    /** The constraint for the photo image. */
    public byte photoConstraint;

    /** The member's home page URL (maxlen: 255). */
    public String homePageURL = "";

    /** A short bit of text provided by the member (maxlen: 255). */
    public String headline = "";

    /** Whether the member identifies as male or female. */
    public boolean isMale;

    /** The date on which the member claims to be born. */
    @Column(nullable=true)
    public Date birthday;

    /** The locale from which the member claims to hail (maxlen: 255). */
    public String location = "";

    /** The user's real name.  Used for searching only. */
    public String realName = "";

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
        location = StringUtil.deNull(profile.location);
        if (profile.photo != null) {
            photoHash = profile.photo.hash;
            photoMimeType = profile.photo.mimeType;
            photoConstraint = profile.photo.constraint;
        }
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public Profile toProfile (String permaName)
    {
        Profile profile = new Profile();
        profile.homePageURL = homePageURL;
        profile.headline = headline;
        profile.isMale = isMale;
        // profile.age = toAge(birthday);
        profile.location = location;
        profile.photo = getPhoto();
        profile.permaName = permaName;
        return profile;
    }

    /**
     * Returns the photo associated with this profile, or the default record.
     */
    public MediaDesc getPhoto ()
    {
        return (photoHash != null) ?
            new MediaDesc(photoHash, photoMimeType, photoConstraint) : Profile.DEFAULT_PHOTO;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ProfileRecord}
     * with the supplied key values.
     */
    public static Key<ProfileRecord> getKey (int memberId)
    {
        return new Key<ProfileRecord>(
                ProfileRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
