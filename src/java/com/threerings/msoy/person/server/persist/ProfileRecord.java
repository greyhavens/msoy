//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Date;
import java.util.Calendar;

import com.google.common.base.Preconditions;

import com.samskivert.util.Calendars;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.Award.AwardType;
import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.server.MediaDescFactory;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.gwt.MemberCard;

import static com.threerings.msoy.Log.log;

/**
 * Contains a member's profile data.
 */
@Entity(fullTextIndices={
    @FullTextIndex(name=ProfileRecord.FTS_REAL_NAME, fields={ "realName" })
})
public class ProfileRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ProfileRecord> _R = ProfileRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Integer> MODIFICATIONS = colexp(_R, "modifications");
    public static final ColumnExp<byte[]> PHOTO_HASH = colexp(_R, "photoHash");
    public static final ColumnExp<Byte> PHOTO_MIME_TYPE = colexp(_R, "photoMimeType");
    public static final ColumnExp<Byte> PHOTO_CONSTRAINT = colexp(_R, "photoConstraint");
    public static final ColumnExp<String> HOME_PAGE_URL = colexp(_R, "homePageURL");
    public static final ColumnExp<String> HEADLINE = colexp(_R, "headline");
    public static final ColumnExp<Byte> SEX = colexp(_R, "sex");
    public static final ColumnExp<Date> BIRTHDAY = colexp(_R, "birthday");
    public static final ColumnExp<Boolean> SHOW_AGE = colexp(_R, "showAge");
    public static final ColumnExp<String> LOCATION = colexp(_R, "location");
    public static final ColumnExp<String> REAL_NAME = colexp(_R, "realName");
    public static final ColumnExp<Integer> PROFILE_BADGE_CODE = colexp(_R, "profileBadgeCode");
    public static final ColumnExp<Integer> PROFILE_MEDAL_ID = colexp(_R, "profileMedalId");
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on Real Name */
    public static final String FTS_REAL_NAME = "RN";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 9;

    /** The unique id of the member with whom this profile is associated. */
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

    /** The member's home page URL. */
    @Column(length=Profile.MAX_HOMEPAGE_LENGTH)
    public String homePageURL = "";

    /** A short bit of text provided by the member. */
    @Column(length=Profile.MAX_STATUS_LENGTH)
    public String headline = "";

    /** Whether the member identifies as {@link Profile#SEX_MALE}, {@link Profile#SEX_FEMALE} or
     * {@link Profile#SEX_UNSPECIFIED}. */
    public byte sex;

    /** The date on which the member claims to be born. */
    @Column(nullable=true)
    public Date birthday;

    /** Whether or not to show their age on their profile. */
    public boolean showAge;

    /** The locale from which the member claims to hail (maxlen: 255). */
    public String location = "";

    /** The user's real name.  Used for searching only. */
    @Column(length=MemberName.MAX_REALNAME_LENGTH)
    public String realName = "";

    /** The user's profile badge.  Only one or none of profileBadgeCode and profileMedalId should
     * be non-zero. */
    public int profileBadgeCode;

    /** The user's profile medal.  Only one or none of profileBadgeCode and profileMedalId should
     * be non-zero. */
    public int profileMedalId;

    /**
     * Converts (year, month, day) to a {@link Date}. Month is 0-11, the other values are as a
     * human would expect to read them.
     */
    public static Date fromDateVec (int[] datevec)
    {
        return Calendars.at(datevec[0], datevec[1], datevec[2]).toSQLDate();
    }

    /**
     * Converts a {@link Date} to a (year, month, day) vector.
     */
    public static int[] toDateVec (Date date)
    {
        Calendar cal = Calendars.at(date).asCalendar();
        return new int[] {
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)
        };
    }

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
        sex = profile.sex;
        location = StringUtil.deNull(profile.location);
        if (profile.birthday != null) {
            birthday = fromDateVec(profile.birthday);
        }
        showAge = (profile.age != 0);
        setPhoto(profile.photo);
        setAward(profile.award);
    }

    /**
     * Creates a runtime record from this persistent record.
     *
     * The award added by this method will only contain the type and awardId.  Some award types
     * may require further information before sending the Profile on to the client.
     *
     * @param member the member record of the member that owns this profile.
     * @param forMemberId the member id of the member that will be *seeing* this profile.
     */
    public Profile toProfile (MemberRecord member, boolean includeBirthday)
    {
        Preconditions.checkArgument(
            member.memberId == memberId, "toProfile() passed invalid member record.");

        Profile profile = new Profile();
        profile.homePageURL = homePageURL;
        profile.headline = headline;
        profile.sex = sex;
        if (birthday != null) {
            if (includeBirthday) {
                profile.birthday = toDateVec(birthday);
            }
            if (showAge) {
                profile.age = toAge(birthday);
            }
        }
        profile.location = location;
        profile.photo = getPhoto();
        profile.role = member.toRole();
        profile.permaName = member.permaName;

        profile.level = member.level;
        profile.memberSince = member.created.getTime();
        profile.lastLogon = (member.lastSession != null) ? member.lastSession.getTime() : 0L;

        profile.award = getAward();

        return profile;
    }

    /**
     * Configures our photo data with the supplied media descriptor, which may be null (in which
     * case this method is a NOOP).
     */
    public void setPhoto (MediaDesc photo)
    {
        // encode the default as null
        if (photo == null || photo.equals(MemberCard.DEFAULT_PHOTO)) {
            return;
        }

        photoHash = HashMediaDesc.unmakeHash(photo);
        photoMimeType = photo.getMimeType();
        photoConstraint = photo.getConstraint();
    }

    /**
     * Returns the photo associated with this profile, or the default record.
     */
    public MediaDesc getPhoto ()
    {
        return (photoHash != null) ?
            MediaDescFactory.createMediaDesc(photoHash, photoMimeType, photoConstraint) : MemberCard.DEFAULT_PHOTO;
    }

    public void setAward (Award award)
    {
        if (award == null) {
            return;
        }

        profileBadgeCode = award.type == AwardType.BADGE ? award.awardId : 0;
        profileMedalId = award.type == AwardType.MEDAL ? award.awardId : 0;
    }

    /**
     * Gets the Award for this profile.  If none has been requested, it will return null.  The
     * Award returned will only have the type and awardId fields filled in.  More information may
     * be required for some award types.
     */
    public Award getAward ()
    {
        Award award = null;

        if (profileBadgeCode != 0) {
            award = new Award();
            award.type = Award.AwardType.BADGE;
            award.awardId = profileBadgeCode;

        } else if (profileMedalId != 0) {
            award = new Award();
            award.type = Award.AwardType.MEDAL;
            award.awardId = profileMedalId;
        }

        return award;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ProfileRecord}
     * with the supplied key values.
     */
    public static Key<ProfileRecord> getKey (int memberId)
    {
        return newKey(_R, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID); }
    // AUTO-GENERATED: METHODS END

    /**
     * Converts a date into years.
     */
    protected static int toAge (Date birthday)
    {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTime(birthday);
        int age = 0;
        do {
            cal.add(Calendar.YEAR, 1);
            if (cal.getTimeInMillis() > now) {
                break;
            }
        } while (++age < 999); // sorry methuselah
        return age;
    }
}
