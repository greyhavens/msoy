//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.annotation.*;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.util.Name;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.MemberMailUtil;
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

        /** Indicates that this user has been known to make trouble. */
        TROUBLEMAKER(1 << 7),

        /** Indicates that the user has validated their email address. */
        VALIDATED(1 << 8),

        /** Indicates that the user is currently banned, temp banned or has an unacknowledged
         * warning. This should be true if and only if this user's tokens contain the msoy ban code
         * or there is a warning for this user. The flag is recorded here so that batch processes
         * like announcement and retention mailings do not have to check 3 records per user.
         * Instead we do one extra write when bans or warnings are updated, a relatively rare
         * activity.
         * @see com.threerings.user.OOOUser#tokens
         * @see com.threerings.user.OOOUser#MSOY_BANNED
         * @see MemberWarningRecord
         */
        SPANKED(1 << 9),

        /** If this user should automatically send a friend request to their affiliate when they
         * register. */
        FRIEND_AFFILIATE(1 << 10),

        /** Is this user a subscriber? */
        SUBSCRIBER(1 << 11),

        /** Is this user a permanent subscriber? */
        SUBSCRIBER_PERMANENT(1 << 12),

        /** Whether the user wants to suppress loading the Flash client when they visit Whirled. */
        NO_AUTO_FLASH(1 << 13),

        /** The next unused flag. Copy this and update the bit mask when making a new flag. */
        UNUSED(1 << 14);

        public int getBit () {
            return _bit;
        }

        Flag (int bit) {
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

        Experience (int bit) {
            _bit = bit;
        }

        protected int _bit;
    }

    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberRecord> _R = MemberRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<String> ACCOUNT_NAME = colexp(_R, "accountName");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<String> PERMA_NAME = colexp(_R, "permaName");
    public static final ColumnExp<Integer> HOME_SCENE_ID = colexp(_R, "homeSceneId");
    public static final ColumnExp<Integer> AVATAR_ID = colexp(_R, "avatarId");
    public static final ColumnExp<Integer> THEME_GROUP_ID = colexp(_R, "themeGroupId");
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<Integer> SESSIONS = colexp(_R, "sessions");
    public static final ColumnExp<Integer> SESSION_MINUTES = colexp(_R, "sessionMinutes");
    public static final ColumnExp<Timestamp> LAST_SESSION = colexp(_R, "lastSession");
    public static final ColumnExp<Integer> EXPERIENCES = colexp(_R, "experiences");
    public static final ColumnExp<Integer> FLAGS = colexp(_R, "flags");
    public static final ColumnExp<Integer> AFFILIATE_MEMBER_ID = colexp(_R, "affiliateMemberId");
    public static final ColumnExp<Integer> LEVEL = colexp(_R, "level");
    public static final ColumnExp<Short> BADGES_VERSION = colexp(_R, "badgesVersion");
    public static final ColumnExp<String> VISITOR_ID = colexp(_R, "visitorId");
    public static final ColumnExp<Integer> CHARITY_MEMBER_ID = colexp(_R, "charityMemberId");
    // AUTO-GENERATED: FIELDS END

    /** The identifer for the full text index on the display name. */
    public static final String FTS_NAME = "ftixName";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 31;

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

    /** The themed environment this user is currently in, or 0. */
    public int themeGroupId;

    /** The date on which this member record was created. */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    @Index(name="ixLastSession")
    public Timestamp lastSession;

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

    /** Defnies the index on {@link #name} converted to lower case. */
    public static Tuple<SQLExpression<?>, Order> ixLowerName ()
    {
        return new Tuple<SQLExpression<?>, Order>(StringFuncs.lower(MemberRecord.NAME), Order.ASC);
    }

    /**
     * Deduces the role given a memberId, flags and account name.
     */
    public static WebCreds.Role toRole (int memberId, int flags, String accountName)
    {
        if (isMaintainer(memberId, flags)) {
            return WebCreds.Role.MAINTAINER;
        } else if (isSet(flags, Flag.ADMIN)) {
            return WebCreds.Role.ADMIN;
        } else if (isSet(flags, Flag.SUPPORT)) {
            return WebCreds.Role.SUPPORT;
        } else if (isSet(flags, Flag.SUBSCRIBER) || isSet(flags, Flag.SUBSCRIBER_PERMANENT)) {
            return WebCreds.Role.SUBSCRIBER;
        } else if (!MemberMailUtil.isPermaguest(accountName)) {
            return WebCreds.Role.REGISTERED;
        } else {
            return WebCreds.Role.PERMAGUEST;
        }
    }

    /**
     * Returns true if the supplied member id and account name combination represents a deleted
     * member.
     */
    public static boolean isDeleted (int memberId, String accountName)
    {
        return accountName.equals(memberId + DELETED_SUFFIX);
    }

    /**
     * Returns true if a member with the given id and flags has maintainer privileges.
     */
    public static boolean isMaintainer (int memberId, int flags)
    {
        return isSet(flags, Flag.MAINTAINER) || isRoot(memberId);
    }

    /**
     * Returns true if a given member id has "root" privileges. The first member in the database
     * has these privileges and this status is used to allow all other privileges to be assigned
     * without resorting to database hackery.
     */
    public static boolean isRoot (int memberId)
    {
        return memberId == 1;
    }

    /**
     * Tests whether a given integer has a given flag set.
     */
    public static boolean isSet (int flags, Flag flag)
    {
        return (flags & flag.getBit()) != 0;
    }

    /** A blank constructor used when loading records from the database. */
    public MemberRecord ()
    {
    }

    /** Constructs a blank member record for the supplied account. */
    public MemberRecord (String accountName)
    {
        this.accountName = accountName;
    }

    /**
     * Get the Role of this member record.
     */
    public WebCreds.Role toRole ()
    {
        return toRole(memberId, flags, accountName);
    }

    /**
     * Creates web credentials for this member record.
     */
    public WebCreds toCreds (String authtok)
    {
        return new WebCreds(
            authtok, accountName, isValidated(), isNewbie(), getName(), permaName, toRole(),
            !isSet(Flag.NO_AUTO_FLASH));
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
        } else if (isSet(Flag.SUBSCRIBER) || isSet(Flag.SUBSCRIBER_PERMANENT)) {
            tokens |= MsoyTokenRing.SUBSCRIBER;
        }

        // This can be set independently of the other flags
        if (isSet(Flag.GREETER)) {
            tokens |= MsoyTokenRing.GREETER;
        }

        return new MsoyTokenRing(tokens);
    }

    /**
     * Returns true if this member has subscriber or higher privileges.
     */
    public boolean isSubscriber ()
    {
        return isSet(Flag.SUBSCRIBER) || isSet(Flag.SUBSCRIBER_PERMANENT) || isSupport();
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
     * Returns true if this member has validated their email address.
     */
    public boolean isValidated ()
    {
        // annoying hack: if this member has a placeholder address, pretend like they are validated
        // because they are using an account created via an external authentication source (like
        // Facebook) and we can't require validation, at some point we'll have the client upsell
        // them to giving us an email address and validating it, but for now this is the path of
        // least PITA
        return isSet(Flag.VALIDATED) || (!MemberMailUtil.isPermaguest(accountName) &&
                                         MemberMailUtil.isPlaceholderAddress(accountName));
    }

    /**
     * Returns true if this member is "new": registered less than a week ago or has had fewer than
     * 7 (Flash) sessions.
     */
    public boolean isNewbie ()
    {
        return (sessions < NEWBIE_SESSIONS) ||
            (System.currentTimeMillis() - created.getTime() < NEWBIE_PERIOD);
    }

    /**
     * Returns true if this member is an anonymous guest.
     */
    public boolean isPermaguest ()
    {
        return MemberMailUtil.isPermaguest(accountName);
    }

    /**
     * Returns true if this member has "root" privileges. The first member in the database has
     * these privileges and this status is used to allow all other privileges to be assigned
     * without resorting to database hackery.
     */
    public boolean isRoot ()
    {
        return isRoot(memberId);
    }

    /**
     * Returns true if this member record represents a deleted account.
     */
    public boolean isDeleted ()
    {
        return isDeleted(memberId, accountName);
    }

    /**
     * Tests whether a given flag is set on this member.
     */
    public boolean isSet (Flag flag)
    {
        return isSet(flags, flag);
    }

    /**
     * Sets a given flag to on or off.
     */
    public void setFlag (Flag flag, boolean value)
    {
        flags = (value ? (flags | flag.getBit()) : (flags & ~flag.getBit()));
    }

    /**
     * Updates a given flag to on or off and returns true if the value changed.
     */
    public boolean updateFlag (Flag flag, boolean value)
    {
        if (isSet(flag) == value) {
            return false;
        }
        setFlag(flag, value);
        return true;
    }

    /**
     * Returns true if this member has had the specified experience.
     */
    public boolean isSet (Experience experience)
    {
        return (experiences & experience.getBit()) != 0;
    }

    /**
     * Sets a given experience to on or off.
     */
    public void setExperience (Experience exp, boolean value)
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
        return newKey(_R, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID); }
    // AUTO-GENERATED: METHODS END

    protected static final String DELETED_SUFFIX = ":deleted";
    protected static final int NEWBIE_SESSIONS = 7;
    protected static final long NEWBIE_PERIOD = 7*24*60*60*1000L;
}
