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
     * Get the names of all the friends of the specified memberId.
     */
    public ArrayList<Name> getFriends (final int memberId)
        throws PersistenceException
    {
        return execute(new Operation<ArrayList<Name>>() {
            public ArrayList<Name> invoke (
                    Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                ArrayList<Name> list = new ArrayList<Name>();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(
                        "select NAME from FRIENDS straight join MEMBERS " +
                        "where (MEMBER_ID1=" + memberId +
                        " and MEMBER_ID=MEMBER_ID2) " +
                        "union select NAME from FRIENDS " +
                        "straight join MEMBERS where (MEMBER_ID2=" + memberId +
                        " and MEMBER_ID=MEMBER_ID1)");
                    while (rs.next()) {
                        list.add(new Name(JDBCUtil.unjigger(rs.getString(1))));
                    }
                    return list;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Add a new friend relation to the database. The user ids may be
     * specified in any order.
     */
    public void addFriends (int memberId1, int memberId2)
        throws PersistenceException
    {
        // we use a convention where the first memberId is the smaller one
        if (memberId1 > memberId2) {
            int temp = memberId2;
            memberId2 = memberId1;
            memberId1 = temp;
        }
        update("insert into FRIENDS values(" + memberId1 + ", " +
            memberId2 + ")");
    }

    /**
     * Remove a friend mapping from the database where the memberId for one
     * is known and only the name for the other is known.
     */
    public void removeFriends (final int memberId1, final Name username2)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the userid for user2
                    int memId2 = -1;
                    ResultSet rs = stmt.executeQuery(
                        "select MEMBER_ID from MEMBERS where NAME = " +
                        JDBCUtil.escape(JDBCUtil.jigger(
                            username2.toString())));
                    while (rs.next()) {
                        memId2 = rs.getInt(1);
                    }
                    rs.close();

                    if (memId2 == -1) {
                        log.warning("Failed to delete friends " +
                            "[mid=" + memberId1 + ", friend=" + username2 +
                            "]. Friend no longer exists.");
                        return null;
                    }

                    int memId1 = memberId1;
                    if (memId1 > memId2) {
                        int temp = memId2;
                        memId2 = memId1;
                        memId1 = temp;
                    }

                    // now delete any friend relation between these two
                    stmt.executeUpdate("delete from FRIENDS where " +
                        "(MEMBER_ID1 = " + memId1 +
                        " and MEMBER_ID2 = " + memId2 + ")");
                    return null;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Delete all the friend relations involving the specified userid
     * because they're being deleted.
     */
    public void deleteAllFriends (int memberId)
        throws PersistenceException
    {
        update("delete from FRIENDS where MEMBER_ID1 = " + memberId +
           " or MEMBER_ID2 = " + memberId);
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
            "MEMBER_ID1 integer not null",
            "MEMBER_ID2 integer not null",
            "STATUS tinyint not null",
            "unique (MEMBER_ID1, MEMBER_ID2)",
            "index (MEMBER_ID2)",
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
