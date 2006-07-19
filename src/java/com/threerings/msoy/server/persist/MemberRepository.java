//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

import com.threerings.util.Name;

import com.threerings.msoy.data.FriendEntry;

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
        _byNameMask = _ptable.getFieldMask();
        _byNameMask.setModified("accountName");

        // tune our table keys on every startup
        maintenance("analyze", "FRIENDS");
    }

    /**
     * Loads up the member record associated with the specified account.
     * Returns null if no matching record could be found.
     */
    public Member loadMember (String accountName)
        throws PersistenceException
    {
        return (Member)loadByExample(
            _ptable, new Member(accountName), _byNameMask);
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
        member.memberId = insert(_ptable, member);
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
        delete(_ptable, member);
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
                        "select NAME, INVITER_ID, STATUS from FRIENDS " +
                        "straight join MEMBERS where (INVITER_ID=" + memberId +
                        " and MEMBER_ID=INVITEE_ID) " +
                        "union select NAME, INVITER_ID, STATUS from FRIENDS " +
                        "straight join MEMBERS where (INVITEE_ID=" + memberId +
                        " and MEMBER_ID=INVITER_ID)");
                    while (rs.next()) {
                        Name name = new Name(
                            JDBCUtil.unjigger(rs.getString(1)));
                        boolean established = rs.getBoolean(3);
                        byte status = established ? FriendEntry.FRIEND
                            : ((memberId == rs.getInt(2))
                                ? FriendEntry.PENDING_THEIR_APPROVAL
                                : FriendEntry.PENDING_MY_APPROVAL);
                        list.add(new FriendEntry(name, false, status));
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
     * @return the FriendEntry status code, or -1 on error.
     *
     * If there is a pending invite from the other member,
     * the friendship will be established and FriendEntry.FRIEND is returned.
     * If no pending friendship exists, one is established and
     * FriendEntry.PENDING_THEIR_APPROVAL is returned.
     * Otherwise, no change is made and FriendEntry.FRIEND or
     * FriendEntry.PENDING_THEIR_APPROVAL is returned, depending.
     */
    public byte inviteOrApproveFriend (final int memberId, final Name other)
        throws PersistenceException
    {
        return executeUpdate(new Operation<Byte>() {
            public Byte invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the userid for the other user
                    int otherId = nameToId(stmt, other);
                    if (otherId == -1) {
                        log.warning("Failed to establish friends: member no " +
                            "longer exists [missing=" + other +
                            ", initiatingMemberId=" + memberId + "].");
                        return (byte) -1;
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
                        return FriendEntry.PENDING_THEIR_APPROVAL;

                    } else if (inviterId == otherId) {
                        // we're responding to an invite
                        String sql = "update FRIENDS " +
                            "set STATUS=true where INVITER_ID=" + otherId +
                            " and INVITEE_ID=" + memberId;
                        JDBCUtil.checkedUpdate(stmt, sql, 1);
                        return FriendEntry.FRIEND;

                    } else {
                        // we've already done all we can
                        return status ? FriendEntry.FRIEND
                                      : FriendEntry.PENDING_THEIR_APPROVAL;
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
    public void removeFriends (final int memberId1, final Name name2)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the userid for user2
                    int memberId2 = nameToId(stmt, name2);
                    if (memberId2 == -1) {
                        log.warning("Failed to delete friends " +
                            "[mid=" + memberId1 + ", friend=" + name2 +
                            "]. Friend no longer exists.");
                        return null;
                    }

                    // now delete any friend relation between these two
                    stmt.executeUpdate("delete from FRIENDS where " +
                        "(INVITER_ID = " + memberId1 +
                        " and INVITEE_ID = " + memberId2 + ") or " +
                        "(INVITER_ID = " + memberId2 +
                        " and INVITEE_ID = " + memberId1 + ")");
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
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
    protected int nameToId (Statement stmt, Name name)
        throws SQLException, PersistenceException
    {
        ResultSet rs = stmt.executeQuery(
            "select MEMBER_ID from MEMBERS where NAME = " +
            JDBCUtil.escape(JDBCUtil.jigger(name.toString())));
        try {
            return rs.next() ? rs.getInt(1) : -1;

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
	_ptable = new Table<Member>(Member.class, "MEMBERS", "MEMBER_ID", true);
    }

    protected Table<Member> _ptable;
    protected FieldMask _byNameMask;
}
