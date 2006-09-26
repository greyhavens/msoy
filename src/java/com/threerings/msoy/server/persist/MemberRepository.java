//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.util.StringUtil;

import com.threerings.util.Name;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberName;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
public class MemberRepository extends DepotRepository
{
    public MemberRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Loads up the member record associated with the specified account.
     * Returns null if no matching record could be found. The record will be
     * fetched from the cache if possible and cached if not.
     */
    public MemberRecord loadMember (String accountName)
        throws PersistenceException
    {
        return load(MemberRecord.class,
            new Key(MemberRecord.ACCOUNT_NAME, accountName));
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with
     * the specified id. The record will be fetched from the cache if possible
     * and cached if not.
     */
    public MemberRecord loadMember (int memberId)
        throws PersistenceException
    {
        return load(MemberRecord.class, memberId);
    }

    /**
     * Loads up the member associated with the supplied session token. Returns
     * null if the session has expired or is not valid.
     */
    public MemberRecord loadMemberForSession (String sessionToken)
        throws PersistenceException
    {
        SessionRecord session = load(SessionRecord.class, sessionToken);
        return (session == null) ? null :
            load(MemberRecord.class, session.memberId);
    }

    /**
     * Creates a mapping from the supplied memberId to a session token (or
     * reuses an existing mapping). The member is assumed to have provided
     * valid credentials and we will allow anyone who presents the returned
     * session token access as the specified member. If an existing session is
     * reused, its expiration date will be adjusted as if the session was newly
     * created as of now (using the supplied <code>persist</code> setting).
     *
     * @param persist if true the session will be set to expire in one month,
     * if false it will be set to expire in one day.
     */
    public String startOrJoinSession (int memberId, boolean persist)
        throws PersistenceException
    {
        // create a new session record for this member
        SessionRecord nsess = new SessionRecord();
	Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
	cal.add(Calendar.DATE, persist ? 30 : 1);
        nsess.expires = new Date(cal.getTimeInMillis());
        nsess.memberId = memberId;
        nsess.token = StringUtil.md5hex(
            "" + memberId + now + Math.random());

        try {
            insert(nsess);
        } catch (DuplicateKeyException dke) {
            // if that fails with a duplicate key, reuse the old record but
            // adjust its expiration
            SessionRecord esess = load(SessionRecord.class,
                new Key(SessionRecord.MEMBER_ID, memberId));
            esess.expires = nsess.expires;
            update(esess, SessionRecord.EXPIRES);

            // then, use the existing record
            nsess = esess;
        }

        return nsess.token;
    }

    /**
     * Clears out a session to member id mapping. This should be called when a
     * user logs off.
     */
    public void clearSession (String sessionToken)
        throws PersistenceException
    {
        delete(SessionRecord.class, sessionToken);
    }

    /**
     * Insert a new member record into the repository and assigns them a unique
     * member id in the process. The {@link MemberRecord#created} field will be
     * filled in by this method if it is not already.
     */
    public void insertMember (MemberRecord member)
        throws PersistenceException
    {
        if (member.created == null) {
            member.created = new Date(System.currentTimeMillis());
            member.lastSession = member.created;
        }
        insert(member);
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
        try {
            updatePartial(MemberRecord.class, memberId,
                MemberRecord.NAME, name);
            return true;
        } catch (DuplicateKeyException dke) {
            return false;
        }
    }

    /**
     * Deletes the specified member from the repository.
     */
    public void deleteMember (MemberRecord member)
        throws PersistenceException
    {
        delete(member);
    }

    /**
     * Deducts the specified amount of flow from the specified member's
     * account.
     */
    public void spendFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow(MemberRecord.MEMBER_ID, memberId, amount, "spend");
    }

    /**
     * Adds the specified amount of flow to the specified member's account.
     */
    public void grantFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow(MemberRecord.MEMBER_ID, memberId, amount, "grant");
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work
     * with the coin system which tracks members by username rather than id.
     */
    public void grantFlow (String accountName, int amount)
        throws PersistenceException
    {
        updateFlow(MemberRecord.ACCOUNT_NAME, accountName, amount, "grant");
    }

    /**
     * Set the home scene id for the specified memberId.
     */
    public void setHomeSceneId (int memberId, int homeSceneId)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId,
            MemberRecord.HOME_SCENE_ID, homeSceneId);
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
        int mods = updatePartial(MemberRecord.class,
            new Key(MemberRecord.ACCOUNT_NAME, accountName),
            MemberRecord.ACCOUNT_NAME, disabledName);
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
        updateLiteral(MemberRecord.class, memberId,
            MemberRecord.SESSIONS, "sessions + 1",
            MemberRecord.SESSION_MINUTES, "sessionMinutes + " + minutes,
            MemberRecord.LAST_SESSION, "NOW()");
    }

    /**
     * Get the FriendEntry record for all friends (pending, too) of the
     * specified memberId. The online status of each friend will be false.
     */
    public ArrayList<FriendEntry> getFriends (final int memberId)
        throws PersistenceException
    {
        // force the creation of the FriendRecord table if necessary
        getMarshaller(FriendRecord.class);

        Key key = new Key("FriendsCache", memberId);
        return invoke(new CollectionQuery<ArrayList<FriendEntry>>(key) {
            public ArrayList<FriendEntry> invoke (Connection conn)
                throws SQLException
            {
                String query = "select name, memberId, inviterId, status " +
                    "from FriendRecord straight join MemberRecord where (" +
                    "(inviterId=" + memberId + " and memberId=inviteeId) or " +
                    "(inviteeId=" + memberId + " and memberId=inviterId))";
                ArrayList<FriendEntry> list = new ArrayList<FriendEntry>();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        MemberName name = new MemberName(
                            rs.getString(1), rs.getInt(2));
                        boolean established = rs.getBoolean(4);
                        byte status = established ? FriendEntry.FRIEND
                            : ((memberId == rs.getInt(3))
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
     * Invite or approve the specified member as our friend.
     *
     * @param memberId The id of the member performing this action.
     * @param otherId The id of the other member.
     *
     * @return the new FriendEntry record for this friendship (modulo online
     * information), or null if there was an error finding the friend.
     */
    public FriendEntry inviteOrApproveFriend (int memberId,  int otherId)
        throws PersistenceException
    {
        // first load the member record of the potential friend
        MemberRecord other = load(MemberRecord.class, otherId);
        if (other.name == null) {
            log.warning("Failed to establish friends: member no longer " +
                "exists [missingId=" + otherId + ", reqId=" + memberId + "].");
            return null;
        }

        // see if there is already a connection, either way
        ArrayList<FriendRecord> existing = new ArrayList<FriendRecord>();
        existing.addAll(findAll(FriendRecord.class,
                            new Key2(FriendRecord.INVITER_ID, memberId,
                                FriendRecord.INVITEE_ID, otherId)));
        existing.addAll(findAll(FriendRecord.class,
                            new Key2(FriendRecord.INVITER_ID, otherId,
                                FriendRecord.INVITEE_ID, memberId)));

        // TODO: update or invalidate "FriendsCache" for both parties

        if (existing.size() > 0) {
            FriendRecord rec = existing.get(0);
            if (rec.inviterId == otherId) {
                // we're responding to an invite
                rec.status = true;
                update(rec);
                return new FriendEntry(
                    other.getName(), false, FriendEntry.FRIEND);

            } else {
                // we've already done all we can
                return new FriendEntry(other.getName(), false, rec.status ?
                    FriendEntry.FRIEND : FriendEntry.PENDING_THEIR_APPROVAL);
            }

        } else {
            // there is no connection yet: invite the other
            FriendRecord rec = new FriendRecord();
            rec.inviterId = memberId;
            rec.inviteeId = otherId;
            insert(rec);
            return new FriendEntry(other.getName(), false,
                FriendEntry.PENDING_THEIR_APPROVAL);
        }
    }

    /**
     * Remove a friend mapping from the database.
     */
    public void removeFriends (int memberId, int otherId)
        throws PersistenceException
    {
        // TODO: update or invalidate "FriendsCache" for both parties
        deleteAll(FriendRecord.class,
            new Key2(FriendRecord.INVITER_ID, memberId,
                FriendRecord.INVITEE_ID, otherId));
        deleteAll(FriendRecord.class,
            new Key2(FriendRecord.INVITER_ID, otherId,
                FriendRecord.INVITEE_ID, memberId));
    }

    /**
     * Delete all the friend relations involving the specified memberId,
     * usually because that member is being deleted.
     */
    public void deleteAllFriends (int memberId)
        throws PersistenceException
    {
        // TODO: update or invalidate "FriendsCache"
        deleteAll(FriendRecord.class,
            new Key(FriendRecord.INVITER_ID, memberId));
        deleteAll(FriendRecord.class,
            new Key(FriendRecord.INVITEE_ID, memberId));
    }

    /** Helper function for {@link #spendFlow} and {@link #grantFlow}. */
    protected void updateFlow (
        String index, Comparable key, int amount, String type)
        throws PersistenceException
    {
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal flow " + type + " [index=" + index +
                ", amount=" + amount + "]");
        }

        String op = type.equals("grant") ? "+" : "-";
        int mods = updateLiteral(
            MemberRecord.class, new Key(index, key),
            MemberRecord.FLOW, MemberRecord.FLOW + op + amount);
        if (mods == 0) {
            throw new PersistenceException(
                "Flow " + type + " modified zero rows " +
                "[where=" + index + "=" + key + ", amount=" + amount + "]");
        } else if (mods > 1) {
            log.warning("Flow " + type + " modified multiple rows " +
                "[where=" + index + "=" + key + ", amount=" + amount +
                ", mods=" + mods + "].");
        }
    }
}
