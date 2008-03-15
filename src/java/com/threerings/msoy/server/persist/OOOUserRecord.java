//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.UniqueConstraint;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.user.OOOUser;

/**
 * Emulates {@link OOOUser} for the Depot.
 */
@Entity(name="users", indices={
    @Index(name="ixEmail", fields={ OOOUserRecord.EMAIL })
})
@Table(uniqueConstraints={ @UniqueConstraint(fieldNames={ OOOUserRecord.USERNAME })})
public class OOOUserRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #userId} field. */
    public static final String USER_ID = "userId";

    /** The qualified column identifier for the {@link #userId} field. */
    public static final ColumnExp USER_ID_C =
        new ColumnExp(OOOUserRecord.class, USER_ID);

    /** The column identifier for the {@link #username} field. */
    public static final String USERNAME = "username";

    /** The qualified column identifier for the {@link #username} field. */
    public static final ColumnExp USERNAME_C =
        new ColumnExp(OOOUserRecord.class, USERNAME);

    /** The column identifier for the {@link #password} field. */
    public static final String PASSWORD = "password";

    /** The qualified column identifier for the {@link #password} field. */
    public static final ColumnExp PASSWORD_C =
        new ColumnExp(OOOUserRecord.class, PASSWORD);

    /** The column identifier for the {@link #email} field. */
    public static final String EMAIL = "email";

    /** The qualified column identifier for the {@link #email} field. */
    public static final ColumnExp EMAIL_C =
        new ColumnExp(OOOUserRecord.class, EMAIL);

    /** The column identifier for the {@link #realname} field. */
    public static final String REALNAME = "realname";

    /** The qualified column identifier for the {@link #realname} field. */
    public static final ColumnExp REALNAME_C =
        new ColumnExp(OOOUserRecord.class, REALNAME);

    /** The column identifier for the {@link #created} field. */
    public static final String CREATED = "created";

    /** The qualified column identifier for the {@link #created} field. */
    public static final ColumnExp CREATED_C =
        new ColumnExp(OOOUserRecord.class, CREATED);

    /** The column identifier for the {@link #siteId} field. */
    public static final String SITE_ID = "siteId";

    /** The qualified column identifier for the {@link #siteId} field. */
    public static final ColumnExp SITE_ID_C =
        new ColumnExp(OOOUserRecord.class, SITE_ID);

    /** The column identifier for the {@link #affiliateTagId} field. */
    public static final String AFFILIATE_TAG_ID = "affiliateTagId";

    /** The qualified column identifier for the {@link #affiliateTagId} field. */
    public static final ColumnExp AFFILIATE_TAG_ID_C =
        new ColumnExp(OOOUserRecord.class, AFFILIATE_TAG_ID);

    /** The column identifier for the {@link #flags} field. */
    public static final String FLAGS = "flags";

    /** The qualified column identifier for the {@link #flags} field. */
    public static final ColumnExp FLAGS_C =
        new ColumnExp(OOOUserRecord.class, FLAGS);

    /** The column identifier for the {@link #tokens} field. */
    public static final String TOKENS = "tokens";

    /** The qualified column identifier for the {@link #tokens} field. */
    public static final ColumnExp TOKENS_C =
        new ColumnExp(OOOUserRecord.class, TOKENS);

    /** The column identifier for the {@link #yohoho} field. */
    public static final String YOHOHO = "yohoho";

    /** The qualified column identifier for the {@link #yohoho} field. */
    public static final ColumnExp YOHOHO_C =
        new ColumnExp(OOOUserRecord.class, YOHOHO);

    /** The column identifier for the {@link #spots} field. */
    public static final String SPOTS = "spots";

    /** The qualified column identifier for the {@link #spots} field. */
    public static final ColumnExp SPOTS_C =
        new ColumnExp(OOOUserRecord.class, SPOTS);

    /** The column identifier for the {@link #shunLeft} field. */
    public static final String SHUN_LEFT = "shunLeft";

    /** The qualified column identifier for the {@link #shunLeft} field. */
    public static final ColumnExp SHUN_LEFT_C =
        new ColumnExp(OOOUserRecord.class, SHUN_LEFT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The user's assigned integer userid. */
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    public int userId;

    /** The user's chosen username. */
    @Column(length=128)
    public String username;

    /** The user's chosen password (encrypted). */
    @Column(length=32)
    public String password;

    /** The user's email address. */
    @Column(length=128)
    public String email;

    /** The user's real name (first, last and whatever else they opt to provide). */
    @Column(length=128)
    public String realname;

    /** The date this record was created. */
    public Date created;

    /** The site identifier of the site through which the user created
     * their account. (Their affiliation, if you will.) */
    public int siteId;

    /** The id of any opaque tag provided by the affiliate to tag this user for their purposes. */
    public int affiliateTagId;

    /** The flags detailing the user's various bits of status. (VALIDATED_FLAG, etc) */
    public int flags;

    /** The tokens detailing the user's site access permissions. (ADMIN, TESTER, etc) */
    public byte[] tokens;

    /** The user's account status for Yohoho! Puzzle Pirates. (TRIAL_STATE, SUBSCRIBER_STATE,
     * etc) */
    public byte yohoho;

    /** The spots that have been given to the user by various crews. */
    @Column(length=128)
    public String spots;

    /** The amount of time remaining on the users shun, in minutes. */
    public int shunLeft;

    /**
     * Creates a OOOUserRecord from a OOOUser.
     */
    public static OOOUserRecord fromUser (OOOUser user)
    {
        OOOUserRecord record = new OOOUserRecord();
        record.userId = user.userId;
        record.username = user.username;
        record.created = user.created;
        record.realname = user.realname;
        record.password = user.password;
        record.email = user.email;
        record.siteId = user.siteId;
        record.flags = user.flags;
        record.tokens = user.tokens;
        record.yohoho = user.yohoho;
        record.spots = user.spots;
        record.shunLeft = user.shunLeft;
        record.affiliateTagId = user.affiliateTagId;
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #OOOUserRecord}
     * with the supplied key values.
     */
    public static Key<OOOUserRecord> getKey (int userId)
    {
        return new Key<OOOUserRecord>(
                OOOUserRecord.class,
                new String[] { USER_ID },
                new Comparable[] { userId });
    }
    // AUTO-GENERATED: METHODS END

    /**
     * Returns true if this user holds the specified token.
     */
    public boolean holdsToken (byte token)
    {
        if (tokens == null) {
            return false;
        }
        for (byte heldTok : tokens) {
            if (heldTok == token) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a OOOUser version of this record.
     */
    public OOOUser toUser ()
    {
        OOOUser user = new OOOUser();
        user.userId = userId;
        user.username = username;
        user.created = created;
        user.realname = realname;
        user.password = password;
        user.email = email;
        user.siteId = siteId;
        user.flags = flags;
        user.tokens = tokens;
        user.yohoho = yohoho;
        user.spots = spots;
        user.shunLeft = shunLeft;
        user.affiliateTagId = affiliateTagId;
        return user;
    }
}
