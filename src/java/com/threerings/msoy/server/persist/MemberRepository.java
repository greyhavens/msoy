//
// $Id$

package com.threerings.msoy.server.persist;

import java.lang.ref.WeakReference;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.LRUHashMap;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

import com.threerings.util.Name;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.server.CacheConfig;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
public class MemberRepository extends JORARepository
{
    /** The database identifier used when establishing a database
     * connection. This value being <code>memberdb</code>. */
    public static final String MEMBER_DB_IDENT = "memberdb";

    /**
     * Constructs a new member repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public MemberRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, MEMBER_DB_IDENT);
        _byNameMask = _mtable.getFieldMask();
        _byNameMask.setModified("accountName");

        // tune our table keys on every startup
        maintenance("analyze", "FRIENDS");
    }

    /**
     * Loads up the member record associated with the specified account.
     * Returns null if no matching record could be found. The record will be
     * fetched from the cache if possible and cached if not.
     */
    public Member loadMember (String accountName)
        throws PersistenceException
    {
        Member member;

        // allow access to the cache from multiple threads but don't try to
        // prevent multiple simultaneous lookups; we don't want to keep the
        // member cache locked during the database load
        synchronized (_memberCache) {
            member = _memberCache.get(accountName);
            if (member != null) {
                return member;
            }
        }

        // load up and cache the member
        member = loadByExample(_mtable, new Member(accountName), _byNameMask);
        cacheMember(member);

        return member;
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with
     * the specified id. The record will be fetched from the cache if possible
     * and cached if not.
     */
    public Member loadMember (int memberId)
        throws PersistenceException
    {
        Member member;

        // allow access to the cache from multiple threads but don't try to
        // prevent multiple simultaneous lookups; we don't want to keep the
        // member cache locked during the database load
        synchronized (_memberCache) {
            WeakReference<Member> ref = _memberIdCache.get(memberId);
            if (ref != null) {
                member = ref.get();
                // make sure this record hasn't expired from the primary cache
                if (member != null &&
                    _memberCache.containsKey(member.accountName)) {
                    return member;
                }
            }
        }

        // load and cache up the member
        member = load(_mtable, "where MEMBER_ID = " + memberId);
        cacheMember(member);

        return member;
    }

    /**
     * Insert a new member record into the repository and assigns them a
     * unique member id in the process. The {@link Member#created} field
     * will be filled in by this method if it is not already.
     */
    public void insertMember (final Member member)
        throws PersistenceException
    {
        if (member.created == null) {
            member.created = new Date(System.currentTimeMillis());
            member.lastSession = member.created;
        }
        member.memberId = insert(_mtable, member);
    }

    /**
     * Configures a member's name.
     *
     * @return true if the member was properly configured, false if the
     * requested name is a duplicate of an existing name.
     */
    public boolean configureMember (int memberId, Name name)
        throws PersistenceException
    {
        final String query = "update MEMBERS set NAME = " +
            JDBCUtil.escape(name.toString()) + " where MEMBER_ID = " + memberId;
        return executeUpdate(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    int mods = stmt.executeUpdate(query);
                    if (mods != 1) {
                        log.warning("Failed to config member [query=" + query +
                                    ", mods=" + mods + "].");
                        return Boolean.FALSE;
                    }

                } catch (SQLException sqe) {
                    if (liaison.isDuplicateRowException(sqe)) {
                        return Boolean.FALSE;
                    } else {
                        throw sqe;
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Deletes the specified member from the repository.
     */
    public void deleteMember (final Member member)
        throws PersistenceException
    {
        delete(_mtable, member);
    }

    /**
     * Deducts the specified amount of flow from the specified member's
     * account.
     */
    public void spendFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow("MEMBER_ID = " + memberId, amount, "spend");
    }

    /**
     * Adds the specified amount of flow to the specified member's account.
     */
    public void grantFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow("MEMBER_ID = " + memberId, amount, "grant");
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work
     * with the coin system which tracks members by username rather than id.
     */
    public void grantFlow (String accountName, int amount)
        throws PersistenceException
    {
        updateFlow("ACCOUNT_NAME = " + JDBCUtil.escape(accountName),
                    amount, "grant");
    }

    /**
     * Mimics the disabling of deleted members by renaming them to an
     * invalid value that we do in our member management system. This is
     * triggered by us receiving a member action indicating that the
     * member was deleted.
     */
    public void disableMember (String accountName, String disabledName)
        throws PersistenceException
    {
        int mods = update(
            "update MEMBERS set ACCOUNT_NAME = " +
            JDBCUtil.escape(disabledName) + " where ACCOUNT_NAME = " +
            JDBCUtil.escape(accountName));
        switch (mods) {
        case 0:
            // they never played our game, no problem
            break;

        case 1:
            log.info("Disabled deleted member [oname=" + accountName +
                     ", dname=" + disabledName + "].");
            break;

        default:
            log.warning("Attempt to disable member account resulted in " +
                        "weirdness [aname=" + accountName +
                        ", dname=" + disabledName + ", mods=" + mods + "].");
            break;
        }
    }

    /**
     * Note that a member's session has ended: increment their sessions, add in
     * the number of minutes spent online, and set their last session time to
     * now.
     */
    public void noteSessionEnded (int memberId, int minutes)
        throws PersistenceException
    {
        String sql = "update MEMBERS set SESSIONS = SESSIONS + 1, " +
            "SESSION_MINUTES = SESSION_MINUTES + " + minutes + ", " +
            "LAST_SESSION = NOW() where MEMBER_ID=" + memberId;
        checkedUpdate(sql, 1);
    }

    /**
     * Get the FriendEntry record for all friends (pending, too) of the
     * specified memberId. The online status of each friend will be false.
     */
    public ArrayList<FriendEntry> getFriends (final int memberId)
        throws PersistenceException
    {
        return execute(new Operation<ArrayList<FriendEntry>>() {
            public ArrayList<FriendEntry> invoke (
                    Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                ArrayList<FriendEntry> list = new ArrayList<FriendEntry>();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(
                        "select NAME, MEMBER_ID, INVITER_ID, STATUS " +
                        "from FRIENDS straight join MEMBERS " +
                        "where (INVITER_ID=" + memberId +
                        " and MEMBER_ID=INVITEE_ID) " +
                        "union select NAME, MEMBER_ID, INVITER_ID, STATUS " +
                        "from FRIENDS straight join MEMBERS " +
                        "where (INVITEE_ID=" + memberId +
                        " and MEMBER_ID=INVITER_ID)");
                    while (rs.next()) {
                        Name name = new Name(
                            JDBCUtil.unjigger(rs.getString(1)));
                        int memberId = rs.getInt(2);
                        boolean established = rs.getBoolean(4);
                        byte status = established ? FriendEntry.FRIEND
                            : ((memberId == rs.getInt(3))
                                ? FriendEntry.PENDING_THEIR_APPROVAL
                                : FriendEntry.PENDING_MY_APPROVAL);
                        list.add(
                            new FriendEntry(memberId, name, false, status));
                    }
                    return list;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Invite or approve the specified member name as our friend.
     *
     * @param memberId The id of the member performing this action.
     * @param otherId The id of the other member.
     *
     * @return the new FriendEntry record for this friendship (modulo online
     * information), or null if there was an error finding the friend.
     */
    public FriendEntry inviteOrApproveFriend (
            final int memberId, final int otherId)
        throws PersistenceException
    {
        return executeUpdate(new Operation<FriendEntry>() {
            public FriendEntry invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    Name otherName = idToName(stmt, otherId);
                    if (otherName == null) {
                        log.warning("Failed to establish friends: " +
                            "member no longer exists " +
                            "[missingId=" + otherId +
                            ", requestorId=" + memberId + "].");
                        return null;
                    }

                    // see if there is already a connection, either way
                    ResultSet rs = stmt.executeQuery(
                        "select INVITER_ID, STATUS from FRIENDS " +
                        "where (INVITER_ID=" + memberId +
                        " and INVITEE_ID=" + otherId + ") " +
                        "union select INVITER_ID, STATUS from FRIENDS " +
                        "where INVITER_ID=" + otherId +
                        " and INVITEE_ID=" + memberId + ")");
                    int inviterId = -1;
                    boolean status = false;
                    if (rs.next()) {
                        inviterId = rs.getInt(1);
                        status = rs.getBoolean(2);
                    }
                    rs.close();

                    if (inviterId == -1) {
                        // there is no connection yet: invite the other
                        String sql = "insert into FRIENDS " +
                            "(INVITER_ID, INVITEE_ID, STATUS) values (" +
                            memberId + ", " + otherId + ", false)";
                        JDBCUtil.checkedUpdate(stmt, sql, 1);
                        return new FriendEntry(otherId, otherName,
                            false, FriendEntry.PENDING_THEIR_APPROVAL);

                    } else if (inviterId == otherId) {
                        // we're responding to an invite
                        String sql = "update FRIENDS " +
                            "set STATUS=true where INVITER_ID=" + otherId +
                            " and INVITEE_ID=" + memberId;
                        JDBCUtil.checkedUpdate(stmt, sql, 1);
                        return new FriendEntry(otherId, otherName,
                            false, FriendEntry.FRIEND);

                    } else {
                        // we've already done all we can
                        return new FriendEntry(otherId, otherName, false,
                            status ? FriendEntry.FRIEND
                                   : FriendEntry.PENDING_THEIR_APPROVAL);
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Remove a friend mapping from the database where the memberId for one
     * is known and only the name for the other is known.
     */
    public void removeFriends (final int memberId, final int otherId)
        throws PersistenceException
    {
        update("delete from FRIENDS where " +
            "(INVITER_ID=" + memberId + " and INVITEE_ID=" + otherId + ") " +
            "or (INVITER_ID=" + otherId + " and INVITEE_ID=" + memberId + ")");
    }

    /**
     * Delete all the friend relations involving the specified userid,
     * usually because they're being deleted.
     */
    public void deleteAllFriends (int memberId)
        throws PersistenceException
    {
        update("delete from FRIENDS where INVITER_ID = " + memberId +
           " or INVITEE_ID = " + memberId);
    }

    /**
     * A convenience method to look up the member's id, given their name,
     * or -1 if unknown.
     */
    protected Name idToName (Statement stmt, int memberId)
        throws SQLException, PersistenceException
    {
        ResultSet rs = stmt.executeQuery(
            "select NAME from MEMBERS where MEMBER_ID = " + memberId);
        try {
            return rs.next() ? new Name(JDBCUtil.unjigger(rs.getString(1)))
                             : null;

        } finally {
            rs.close();
        }
    }


    /** Helper function for {@link #spendFlow} and {@link #grantFlow}. */
    protected void updateFlow (String where, int amount, String type)
        throws PersistenceException
    {
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal flow " + type + " [where=" + where +
                ", amount=" + amount + "]");
        }

        String action = type.equals("grant") ? "+" : "-";
        String query = "update MEMBERS set FLOW = FLOW " + action + " " +
            amount + " where " + where;
        int mods = update(query);
        if (mods == 0) {
            throw new PersistenceException(
                "Flow " + type + " modified zero rows [where=" + where +
                ", amount=" + amount + "]");
        } else if (mods > 1) {
            log.warning("Flow " + type + " modified multiple rows " +
                        "[where=" + where + ", amount=" + amount +
                        ", mods=" + mods + "].");
        }
    }

    /**
     * Caches the supplied member record which was presumably freshly loaded
     * from the database.
     */
    protected void cacheMember (Member member)
    {
        if (member != null) {
            synchronized (_memberCache) {
                _memberCache.put(member.accountName, member);
                _memberIdCache.put(
                    member.memberId, new WeakReference<Member>(member));
            }
        }
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "MEMBERS", new String[] {
            "MEMBER_ID integer not null auto_increment",
            "ACCOUNT_NAME varchar(64) not null",
            "NAME varchar(64) unique",
            "FLOW integer not null",
            "CREATED datetime not null",
            "SESSIONS integer not null",
            "SESSION_MINUTES integer not null",
            "LAST_SESSION datetime not null",
            "FLAGS integer not null",
            "primary key (MEMBER_ID)",
            "unique (ACCOUNT_NAME)",
        }, "");

        JDBCUtil.createTableIfMissing(conn, "FRIENDS", new String[] {
            "INVITER_ID integer not null",
            "INVITEE_ID integer not null",
            "STATUS boolean not null",
            "unique (INVITER_ID, INVITEE_ID)",
            "index (INVITEE_ID)",
        }, "");
    }

    @Override // documentation inherited
    protected void createTables ()
    {
	_mtable = new Table<Member>(Member.class, "MEMBERS", "MEMBER_ID", true);
    }

    protected Table<Member> _mtable;
    protected FieldMask _byNameMask;

    /** Contains a mapping from account name to {@link Member} records.
     * TODO: create a fancier cache system that expires records after some time
     * period. */
    protected LRUHashMap<String,Member> _memberCache =
        new LRUHashMap<String,Member>(CacheConfig.MEMBER_CACHE_SIZE);

    /** Contains a mapping from memberId to {@link Member} records. */
    protected HashMap<Integer,WeakReference<Member>> _memberIdCache =
        new HashMap<Integer,WeakReference<Member>>();
}
