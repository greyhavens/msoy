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
import java.util.Calendar;
import java.util.HashMap;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.LRUHashMap;
import com.samskivert.util.StringUtil;

// import com.samskivert.jdbc.ConnectionProvider;
// import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
// import com.samskivert.jdbc.JORARepository;
// import com.samskivert.jdbc.jora.FieldMask;
// import com.samskivert.jdbc.jora.Table;

import com.threerings.util.Name;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberName;
import com.threerings.msoy.server.CacheConfig;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
public class HibernateMemberRepository extends HibernateRepository
{
    @Override // from HibernateRepository
    public void configure (AnnotationConfiguration config)
    {
        config.addAnnotatedClass(Member.class);
        config.addAnnotatedClass(AuthSession.class);
    }

    /**
     * Loads up the member record associated with the specified account.
     * Returns null if no matching record could be found. The record will be
     * fetched from the cache if possible and cached if not.
     */
    public Member loadMember (final String accountName)
        throws PersistenceException
    {
        return execute(new Operation<Member>() {
            public Member invoke (Session session) {
                return (Member)session.createCriteria(Member.class).
                    add(Restrictions.eq("accountName", accountName)).
                    setCacheable(true).
                    uniqueResult();
            }
        });
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with
     * the specified id. The record will be fetched from the cache if possible
     * and cached if not.
     */
    public Member loadMember (final int memberId)
        throws PersistenceException
    {
        return execute(new Operation<Member>() {
            public Member invoke (Session session) {
                return (Member)session.get(Member.class, memberId);
            }
        });
    }

    /**
     * Loads up the member associated with the supplied session token. Returns
     * null if the session has expired or is not valid.
     */
    public Member loadMemberForSession (final String sessionToken)
        throws PersistenceException
    {
        return execute(new Operation<Member>() {
            public Member invoke (Session session) {
                // TODO: maybe there's a way to do this with a join that
                // preserves cacheability
                AuthSession authsess = (AuthSession)
                    session.get(AuthSession.class, sessionToken);
                if (authsess == null) {
                    return null;
                }
                return (Member)session.get(Member.class, authsess.memberId);
            }
        });
    }

    /**
     * Creates a mapping from the supplied memberId to a session token (or
     * reusese an existing mapping). The member is assumed to have provided
     * valid credentials and we will allow anyone who presents the returned
     * session token access as the specified member. If an existing session is
     * reused, its expiration date will be adjusted as if the session was newly
     * created as of now (using the supplied <code>persist</code> setting).
     *
     * @param persist if true the session will be set to expire in one month,
     * if false it will be set to expire in one day.
     */
    public String startOrJoinSession (final int memberId, boolean persist)
        throws PersistenceException
    {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, persist ? 30 : 1);

        return execute(new Operation<String>() {
            public String invoke (Session session) {
                // look for an existing session for this member
                AuthSession authsess = (AuthSession)
                    session.createCriteria(AuthSession.class).
                    add(Restrictions.eq("memberId", memberId)).
                    uniqueResult();
                if (authsess == null) {
                    authsess = new AuthSession();
                    authsess.memberId = memberId;
                    authsess.token = StringUtil.md5hex(
                        "" + memberId + cal.getTime().getTime() +
                        Math.random());
                }
                authsess.expires = new Date(cal.getTime().getTime());
                session.saveOrUpdate(authsess);

                return authsess.token;
            }
        });
    }

    /**
     * Clears out a session to member id mapping. This should be called when a
     * user logs off.
     */
    public void clearSession (final String sessionToken)
        throws PersistenceException
    {
        execute(new VoidOperation() {
            public void invokeVoid (Session session) {
                session.delete(new AuthSession(sessionToken));
            }
        });
    }

    /**
     * Insert a new member record into the repository and assigns them a unique
     * member id in the process. The {@link Member#created} field will be
     * filled in by this method if it is not already.
     */
    public void insertMember (final Member member)
        throws PersistenceException
    {
        execute(new VoidOperation() {
            public void invokeVoid (Session session) {
                if (member.created == null) {
                    member.created = new Date(System.currentTimeMillis());
                    member.lastSession = member.created;
                }
                session.save(member);
            }
        });
    }

    /**
     * Configures a member's name.
     *
     * @return true if the member was properly configured, false if the
     * requested name is a duplicate of an existing name.
     */
    public boolean configureMember (final int memberId, final Name name)
        throws PersistenceException
    {
        return execute(new Operation<Boolean>() {
            public Boolean invoke (Session session) {
                // TODO: catch duplicate row, return false
                Member member = (Member)session.load(Member.class, memberId);
                System.err.println("Dirty? " + session.isDirty() + " (" + member + ")");
                System.err.println("Setting " + member.name + " to " + name);
                member.name = name.toString();
                System.err.println("Now Dirty? " + session.isDirty());
                return true;
            }
        });
    }

    /**
     * Deletes the specified member from the repository.
     */
    public void deleteMember (final Member member)
        throws PersistenceException
    {
        execute(new VoidOperation() {
            public void invokeVoid (Session session) {
                session.delete(member);
            }
        });
    }

    /**
     * Deducts the specified amount of flow from the specified member's
     * account.
     */
    public void spendFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow(memberId, amount, true);
    }

    /**
     * Adds the specified amount of flow to the specified member's account.
     */
    public void grantFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow(memberId, amount, false);
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work
     * with the coin system which tracks members by username rather than id.
     */
    public void grantFlow (String accountName, int amount)
        throws PersistenceException
    {
        Member member = loadMember(accountName);
        if (member == null) {
            log.warning("Requested to grant flow to unknown member " +
                "[account=" + accountName + ", amount=" + amount + "].");
        } else {
            updateFlow(member.memberId, amount, false);
        }
    }

    /**
     * Mimics the disabling of deleted members by renaming them to an
     * invalid value that we do in our member management system. This is
     * triggered by us receiving a member action indicating that the
     * member was deleted.
     */
    public void disableMember (
        final String accountName, final String disabledName)
        throws PersistenceException
    {
        execute(new VoidOperation() {
            public void invokeVoid (Session session) {
                Member member = (Member)session.createCriteria(Member.class).
                    add(Restrictions.eq("accountName", accountName)).
                    uniqueResult();
                if (member != null) {
                    member.accountName = disabledName;
                }
            }
        });
    }

    /**
     * Note that a member's session has ended: increment their sessions, add in
     * the number of minutes spent online, and set their last session time to
     * now.
     */
    public void noteSessionEnded (final int memberId, final int minutes)
        throws PersistenceException
    {
        execute(new VoidOperation() {
            public void invokeVoid (Session session) {
                Member member = (Member)session.load(Member.class, memberId);
                member.sessions++;
                member.sessionMinutes += minutes;
                member.lastSession = new Date(System.currentTimeMillis());
            }
        });
    }

    /**
     * Get the FriendEntry record for all friends (pending, too) of the
     * specified memberId. The online status of each friend will be false.
     */
    public ArrayList<FriendEntry> getFriends (final int memberId)
        throws PersistenceException
    {
        return new ArrayList<FriendEntry>();
//         return execute(new Operation<ArrayList<FriendEntry>>() {
//             public ArrayList<FriendEntry> invoke (
//                     Connection conn, DatabaseLiaison liaison)
//                 throws SQLException, PersistenceException
//             {
//                 ArrayList<FriendEntry> list = new ArrayList<FriendEntry>();
//                 Statement stmt = conn.createStatement();
//                 try {
//                     ResultSet rs = stmt.executeQuery(
//                         "select NAME, MEMBER_ID, INVITER_ID, STATUS " +
//                         "from FRIENDS straight join MEMBERS " +
//                         "where (INVITER_ID=" + memberId +
//                         " and MEMBER_ID=INVITEE_ID) " +
//                         "union select NAME, MEMBER_ID, INVITER_ID, STATUS " +
//                         "from FRIENDS straight join MEMBERS " +
//                         "where (INVITEE_ID=" + memberId +
//                         " and MEMBER_ID=INVITER_ID)");
//                     while (rs.next()) {
//                         MemberName name = new MemberName(
//                             rs.getString(1), rs.getInt(2));
//                         boolean established = rs.getBoolean(4);
//                         byte status = established ? FriendEntry.FRIEND
//                             : ((memberId == rs.getInt(3))
//                                 ? FriendEntry.PENDING_THEIR_APPROVAL
//                                 : FriendEntry.PENDING_MY_APPROVAL);
//                         list.add(new FriendEntry(name, false, status));
//                     }
//                     return list;

//                 } finally {
//                     JDBCUtil.close(stmt);
//                 }
//             }
//         });
    }

    /**
     * Invite or approve the specified member as our friend.
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
//         return executeUpdate(new Operation<FriendEntry>() {
//             public FriendEntry invoke (Connection conn, DatabaseLiaison liaison)
//                 throws SQLException, PersistenceException
//             {
//                 Statement stmt = conn.createStatement();
//                 try {
//                     MemberName otherName = idToName(stmt, otherId);
//                     if (otherName == null) {
//                         log.warning("Failed to establish friends: " +
//                             "member no longer exists " +
//                             "[missingId=" + otherId +
//                             ", requestorId=" + memberId + "].");
//                         return null;
//                     }

//                     // see if there is already a connection, either way
//                     ResultSet rs = stmt.executeQuery(
//                         "select INVITER_ID, STATUS from FRIENDS " +
//                         "where (INVITER_ID=" + memberId +
//                         " and INVITEE_ID=" + otherId + ") " +
//                         "union select INVITER_ID, STATUS from FRIENDS " +
//                         "where INVITER_ID=" + otherId +
//                         " and INVITEE_ID=" + memberId + ")");
//                     int inviterId = -1;
//                     boolean status = false;
//                     if (rs.next()) {
//                         inviterId = rs.getInt(1);
//                         status = rs.getBoolean(2);
//                     }
//                     rs.close();

//                     if (inviterId == -1) {
//                         // there is no connection yet: invite the other
//                         String sql = "insert into FRIENDS " +
//                             "(INVITER_ID, INVITEE_ID, STATUS) values (" +
//                             memberId + ", " + otherId + ", false)";
//                         JDBCUtil.checkedUpdate(stmt, sql, 1);
//                         return new FriendEntry(otherName, false,
//                             FriendEntry.PENDING_THEIR_APPROVAL);

//                     } else if (inviterId == otherId) {
//                         // we're responding to an invite
//                         String sql = "update FRIENDS " +
//                             "set STATUS=true where INVITER_ID=" + otherId +
//                             " and INVITEE_ID=" + memberId;
//                         JDBCUtil.checkedUpdate(stmt, sql, 1);
//                         return new FriendEntry(otherName, false,
//                             FriendEntry.FRIEND);

//                     } else {
//                         // we've already done all we can
//                         return new FriendEntry(otherName, false,
//                             status ? FriendEntry.FRIEND
//                                    : FriendEntry.PENDING_THEIR_APPROVAL);
//                     }

//                 } finally {
//                     JDBCUtil.close(stmt);
//                 }
//             }
//         });
        return null;
    }

    /**
     * Remove a friend mapping from the database where the memberId for one
     * is known and only the name for the other is known.
     */
    public void removeFriends (final int memberId, final int otherId)
        throws PersistenceException
    {
//         update("delete from FRIENDS where " +
//             "(INVITER_ID=" + memberId + " and INVITEE_ID=" + otherId + ") " +
//             "or (INVITER_ID=" + otherId + " and INVITEE_ID=" + memberId + ")");
    }

    /**
     * Delete all the friend relations involving the specified userid,
     * usually because they're being deleted.
     */
    public void deleteAllFriends (int memberId)
        throws PersistenceException
    {
//         update("delete from FRIENDS where INVITER_ID = " + memberId +
//            " or INVITEE_ID = " + memberId);
    }

    /**
     * A convenience method to look up and return a member's name, given their
     * id, or null if unknown.
     */
    protected MemberName idToName (Session session, int memberId)
    {
        Member member = (Member)session.get(Member.class, memberId);
        return (member == null) ? null : member.getName();
    }

    /** Helper function for {@link #spendFlow} and {@link #grantFlow}. */
    protected void updateFlow (final int memberId, int amount, boolean spend)
    {
        if (amount <= 0) {
            throw new HibernateException(
                "Illegal flow [member=" + memberId + ", amount=" + amount +
                ", spend=" + spend + "]");
        }

        final int adjust = spend ? -amount : amount;
        execute(new VoidOperation() {
            public void invokeVoid (Session session) {
                Member member = (Member)session.get(Member.class, memberId);
                member.flow += adjust;
            }
        });
    }
}
