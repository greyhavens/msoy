//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.SQLExpression;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;
import com.threerings.util.Name;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity(indices={ @Index(name="ixLowerName") },
        fullTextIndices={ @FullTextIndex(name=MemberRecord.FTS_NAME, fields={ "name" }) })
public class MemberRecord extends PersistentRecord
{
    /** Flags used in the {@link #flags} field. */
    public static enum Flag
    {
        /** A flag denoting this user as having support privileges. */
        SUPPORT(1 << 0),

        /** A flag denoting this user as having admin privileges. */
        ADMIN(1 << 1),

        /** A flag denoting this user as having maintainer privileges. */
        MAINTAINER(1 << 5),

        /** A flag denoting this user has having elected to see mature content. */
        SHOW_MATURE(1 << 2),

        /** A flag denoting this user does not want to receive real email for Whirled mail. */
        NO_WHIRLED_MAIL_TO_EMAIL(1 << 3),

        /** A flag denoting this user does not want to receive announcement mail. */
        NO_ANNOUNCE_EMAIL(1 << 4),

        /** A flag denoting that this user has opted to be a whirled greeter. */
        GREETER(1 << 6),

        /** The next unused flag. Copy this and update the bit mask when making a new flag. */
        TROUBLEMAKER(1 << 7),

        /** The next unused flag. Copy this and update the bit mask when making a new flag. */
        UNUSED(1 << 8);

        public int getBit () {
            return _bit;
        }

        Flag (final int bit) {
            _bit = bit;
        }

        protected int _bit;
    }

    /** Experiences used in the {@link #experiences} field. */
    public static enum Experience
    {
        /** Indicates whether this user has played a game. */
        PLAYED_GAME(1 << 0),

        /** Indicates whether this user has entered a Whirled. */
        EXPLORED_WHIRLED(1 << 1),

        /** Indicates whether this user has decorated their home. */
        DECORATED_HOME(1 << 2),

        /** A place holder experience that is not used and reminds us that we can't have more than
         * 32 experiences. */
        NOT_USED(1 << 32);

        public int getBit () {
            return _bit;
        }

        Experience (final int bit) {
            _bit = bit;
        }

        protected int _bit;
    }

    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberRecord> _R = MemberRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp ACCOUNT_NAME = colexp(_R, "accountName");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp PERMA_NAME = colexp(_R, "permaName");
    public static final ColumnExp HOME_SCENE_ID = colexp(_R, "homeSceneId");
    public static final ColumnExp AVATAR_ID = colexp(_R, "avatarId");
    public static final ColumnExp CREATED = colexp(_R, "created");
    public static final ColumnExp SESSIONS = colexp(_R, "sessions");
    public static final ColumnExp SESSION_MINUTES = colexp(_R, "sessionMinutes");
    public static final ColumnExp LAST_SESSION = colexp(_R, "lastSession");
    public static final ColumnExp HUMANITY = colexp(_R, "humanity");
    public static final ColumnExp LAST_HUMANITY_ASSESSMENT = colexp(_R, "lastHumanityAssessment");
    public static final ColumnExp EXPERIENCES = colexp(_R, "experiences");
    public static final ColumnExp FLAGS = colexp(_R, "flags");
    public static final ColumnExp AFFILIATE_MEMBER_ID = colexp(_R, "affiliateMemberId");
    public static final ColumnExp LEVEL = colexp(_R, "level");
    public static final ColumnExp BADGES_VERSION = colexp(_R, "badgesVersion");
    public static final ColumnExp VISITOR_ID = colexp(_R, "visitorId");
    public static final ColumnExp CHARITY_MEMBER_ID = colexp(_R, "charityMemberId");
    // AUTO-GENERATED: FIELDS END

    /** The identifer for the full text index on the display name. */
    public static final String FTS_NAME = "ftixName";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 29;

    /** Defnies the index on {@link #name} converted to lower case. */
    public static Tuple<SQLExpression, Order> ixLowerName ()
    {
        return new Tuple<SQLExpression, Order>(
            new FunctionExp("LOWER", MemberRecord.NAME), Order.ASC);
    }

    /** This member's unique id. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int memberId;

    /** The authentication account associated with this member. */
    @Column(unique=true) // note: this implies @Index
    public String accountName;

    /** This member's display name. Is shown in the Whirled, can be changed any time. */
    @Index(name="ixName")
    public String name;

    /** This member's permanent name. Must be URL-safe; used to logon to wiki and forums. */
    @Column(nullable=true, unique=true) // note: this implies @Index
    public String permaName;

    /** The home scene for this member. */
    public int homeSceneId;

    /** The avatar of this user, or 0. */
    public int avatarId;

    /** The date on which this member record was created. */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    @Index(name="ixLastSession") 
    public Timestamp lastSession;

    /** This member's current humanity rating, between 0 and {@link MsoyCodes#MAX_HUMANITY}. */
    public int humanity;

    /** The time at which we last assessed this member's humanity. */
    public Timestamp lastHumanityAssessment;

    /** Bits tracking whether the user has had any of a set of "one time" experiences. */
    public int experiences;

    /** Various one bit data. */
    public int flags;

    /** The memberId of this user's affiliate, or 0 if none. */
    @Column(defaultValue="0") @Index(name="ixAffiliate")
    public int affiliateMemberId;

    /** The currently reported level of this user. */
    @Column(defaultValue="1")
    public int level = 1;

    /** The version of the "Badges set" that this user has seen. If this is behind
     * BadgeType.VERSION, then the member's InProgressBadges will be recomputed. */
    @Column(defaultValue="0")
    public short badgesVersion;

    /** Visitor ID of this player, used to correlate their actions before they registered. */
    @Column(nullable=true, defaultValue="")
    public String visitorId;

    /** The ID of the charity member that this member has selected to receive donations. */
    @Column(defaultValue="0")
    public int charityMemberId = 0;

    /** A blank constructor used when loading records from the database. */
    public MemberRecord ()
    {
    }

    /** Constructs a blank member record for the supplied account. */
    public MemberRecord (final String accountName)
    {
        this.accountName = accountName;
    }

    /**
     * Creates web credentials for this member record.
     */
    public WebCreds toCreds (final String authtok)
    {
        WebCreds.Role role;
        if (isMaintainer()) {
            role = WebCreds.Role.MAINTAINER;
        } else if (isSet(Flag.ADMIN)) {
            role = WebCreds.Role.ADMIN;
        } else if (isSet(Flag.SUPPORT)) {
            role = WebCreds.Role.SUPPORT;
        } else {
            role = WebCreds.Role.USER;
        }
        return new WebCreds(authtok, accountName, getName(), permaName, role);
    }

    /**
     * Creates a token ring for this member record.
     */
    public MsoyTokenRing toTokenRing ()
    {
        int tokens = 0;
        if (isMaintainer()) {
            tokens |= MsoyTokenRing.MAINTAINER;
        } else if (isSet(Flag.ADMIN)) {
            tokens |= MsoyTokenRing.ADMIN;
        } else if (isSet(Flag.SUPPORT)) {
            tokens |= MsoyTokenRing.SUPPORT;
        }

        // This can be set independently of the other flags
        if (isSet(Flag.GREETER)) {
            tokens |= MsoyTokenRing.GREETER;
        }

        return new MsoyTokenRing(tokens);
    }

    /**
     * Returns true if this member has support or higher privileges.
     */
    public boolean isSupport ()
    {
        return isSet(Flag.SUPPORT) || isAdmin();
    }

    /**
     * Returns true if this member has admin or higher privileges.
     */
    public boolean isAdmin ()
    {
        return isSet(Flag.ADMIN) || isMaintainer();
    }

    /**
     * Returns true if this member has maintainer privileges.
     */
    public boolean isMaintainer ()
    {
        return isSet(Flag.MAINTAINER) || isRoot();
    }

    /**
     * Returns true if this member has opted to be a whirled greeter.
     */
    public boolean isGreeter ()
    {
        return isSet(Flag.GREETER);
    }

    /**
     * Returns true if this member is known to make trouble.
     */
    public boolean isTroublemaker ()
    {
        return isSet(Flag.TROUBLEMAKER);
    }

    /**
     * Returns true if this member has "root" privileges. The first member in the database has
     * these privileges and this status is used to allow all other privileges to be assigned
     * without resorting to database hackery.
     */
    public boolean isRoot ()
    {
        return memberId == 1;
    }

    /**
     * Tests whether a given flag is set on this member.
     */
    public boolean isSet (final Flag flag)
    {
        return (flags & flag.getBit()) != 0;
    }

    /**
     * Sets a given flag to on or off.
     */
    public void setFlag (final Flag flag, final boolean value)
    {
        flags = (value ? (flags | flag.getBit()) : (flags & ~flag.getBit()));
    }

    /**
     * Returns true if this member has had the specified experience.
     */
    public boolean isSet (final Experience experience)
    {
        return (experiences & experience.getBit()) != 0;
    }

    /**
     * Sets a given experience to on or off.
     */
    public void setExperience (final Experience exp, final boolean value)
    {
        experiences = (value ? (experiences | exp.getBit()) : (experiences & ~exp.getBit()));
    }

    /** Returns this member's name as a proper {@link Name} instance. */
    public MemberName getName ()
    {
        return new MemberName(name, memberId);
    }

    /**
     * Returns a brief string containing our account name, member id and display name.
     */
    public String who ()
    {
        return accountName + " (" + memberId + ", " + name + ")";
    }

    /** Generates a string representation of this instance. */
    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberRecord}
     * with the supplied key values.
     */
    public static Key<MemberRecord> getKey (int memberId)
    {
        return new Key<MemberRecord>(
                MemberRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
