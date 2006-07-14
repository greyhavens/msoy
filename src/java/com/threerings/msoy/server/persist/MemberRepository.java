//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;

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
     * Updates the specified member's record to reflect that they now have
     * access to the specified town (and all towns up to that point).
     */
    public void grantTownAccess (int memberId, String townId)
        throws PersistenceException
    {
        checkedUpdate("update MEMBERS set TOWN_ID = '" + townId + "' " +
                      "where MEMBER_ID = " + memberId, 1);
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
        StringBuffer update = new StringBuffer();
        update.append("update MEMBERS set SESSIONS = SESSIONS + 1, ");
        update.append("SESSION_MINUTES = SESSION_MINUTES + ");
        update.append(minutes).append(", ");
        update.append("LAST_SESSION = NOW() where MEMBER_ID=").append(memberId);
        checkedUpdate(update.toString(), 1);
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
            "MEMBER_ID INTEGER NOT NULL AUTO_INCREMENT",
            "ACCOUNT_NAME VARCHAR(64) NOT NULL",
            "NAME VARCHAR(64) UNIQUE",
            "FLOW INTEGER NOT NULL",
            "CREATED DATETIME NOT NULL",
            "SESSIONS INTEGER NOT NULL",
            "SESSION_MINUTES INTEGER NOT NULL",
            "LAST_SESSION DATETIME NOT NULL",
            "FLAGS INTEGER NOT NULL",
            "PRIMARY KEY (MEMBER_ID)",
            "UNIQUE (ACCOUNT_NAME)",
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
