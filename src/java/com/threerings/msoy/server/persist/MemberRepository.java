//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.PersistenceContext.CacheTraverser;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.SimpleCacheKey;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.data.Invitation;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
public class MemberRepository extends DepotRepository
{
    /** The cache identifier for the friends-of-a-member collection query. */
    public static final String FRIENDS_CACHE_ID = "FriendsCache";

    public MemberRepository (ConnectionProvider conprov)
    {
        super(conprov);

        // TEMP
        _ctx.registerMigration(MemberRecord.class, new EntityMigration.Retype(4, "lastSession"));

        _ctx.registerMigration(FriendRecord.class, new EntityMigration(2) {
            public int invoke (Connection conn) throws SQLException {
                if (!JDBCUtil.tableContainsColumn(conn, _tableName, "status")) {
                    // we'll accept this inconsistency
                    log.warning(_tableName + ".status already dropped.");
                    return 0;
                }

                Statement stmt = conn.createStatement();
                try {
                    log.info("Deleting pending friends and dropping 'status' from " + _tableName);
                    int n = stmt.executeUpdate("delete from " + _tableName + " where status = 0");
                    n += stmt.executeUpdate("alter table " + _tableName + " drop column status");
                    return n;
                } finally {
                    stmt.close();
                }
            }

        });

        // added 4-27-2007
        _ctx.registerMigration(MemberRecord.class, new EntityMigration.Rename(8, "inviterId",
            MemberRecord.INVITING_FRIEND_ID));

        // added 4-30-2007
        _ctx.registerMigration(InvitationRecord.class, new EntityMigration.Retype(2, "issued"));
        _ctx.registerMigration(InvitationRecord.class, new EntityMigration.Retype(2, "viewed"));
        // END TEMP

        _flowRepo = new FlowRepository(_ctx);

        // add a cache invalidator that listens to single FriendRecord updates
        _ctx.addCacheListener(FriendRecord.class, new CacheListener<FriendRecord>() {
            public void entryInvalidated (CacheKey key, FriendRecord friend) {
                _ctx.cacheInvalidate(FRIENDS_CACHE_ID, friend.inviterId);
                _ctx.cacheInvalidate(FRIENDS_CACHE_ID, friend.inviteeId);
            }
            public void entryCached (CacheKey key, FriendRecord newEntry, FriendRecord oldEntry) {
                // nothing to do here
            }
        });

        // add a cache invalidator that listens to MemberRecord updates
        _ctx.addCacheListener(MemberRecord.class, new CacheListener<MemberRecord>() {
            public void entryInvalidated (CacheKey key, MemberRecord member) {
                _ctx.cacheInvalidate(MemberNameRecord.getKey(member.memberId));
            }
            public void entryCached (CacheKey key, MemberRecord newEntry, MemberRecord oldEntry) {
            }
        });

    }

    public FlowRepository getFlowRepository ()
    {
        return _flowRepo;
    }

    /**
     * Loads up the member record associated with the specified account.  Returns null if no
     * matching record could be found.
     */
    public MemberRecord loadMember (String accountName)
        throws PersistenceException
    {
        return load(MemberRecord.class, new Where(MemberRecord.ACCOUNT_NAME_C, accountName));
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with the specified id. The
     * record will be fetched from the cache if possible and cached if not.
     */
    public MemberRecord loadMember (int memberId)
        throws PersistenceException
    {
        return load(MemberRecord.class, memberId);
    }

    /**
     * Calculate a count of the active member population, currently defined as anybody
     * whose last session is within the past 60 days.
     *
     * TODO: Cache this!
     */
    public int getActivePopulationCount ()
        throws PersistenceException
    {
        MemberCountRecord record = load (
            MemberCountRecord.class,
            new FromOverride(MemberRecord.class),
            new Where(new GreaterThan(MemberRecord.LAST_SESSION_C,
                                      new LiteralExp("SUBDATE(CURDATE(), 60)"))),
            new FieldOverride(MemberCountRecord.POPULATION, new LiteralExp("COUNT(*)")));
        return record.population;
    }

    /**
     * Looks up a member's name by id. Returns null if no member exists with the specified id.
     */
    public MemberNameRecord loadMemberName (int memberId)
        throws PersistenceException
    {
        List<MemberNameRecord> result = loadMemberNames(new int[] { memberId });
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Looks up some members' names by id.
     *
     * TODO: Implement findAll(Persistent.class, Comparable... keys) or the like,
     *       as per MDB's suggestion, say so we can cache properly.
     */
    public List<MemberNameRecord> loadMemberNames (int[] memberIds)
        throws PersistenceException
    {
        if (memberIds.length == 0) {
            return Collections.emptyList();
        }
        Comparable[] idArr = IntListUtil.box(memberIds);
        return findAll(MemberNameRecord.class,
                       new FromOverride(MemberRecord.class),
                       new Where(new In(MemberRecord.MEMBER_ID_C, idArr)));
    }

    /**
     * Returns name information for all members that match the supplied search string. 
     * Display names are searched for exact matches.
     */
    public List<MemberNameRecord> findMemberNames (String search, int limit)
        throws PersistenceException
    {
        return findAll(MemberNameRecord.class,
                       new FromOverride(MemberRecord.class),
                       new Where(new Equals(MemberRecord.NAME_C, search)),
                       new Limit(0, limit));
    }

    /**
     * Loads up the member associated with the supplied session token. Returns null if the session
     * has expired or is not valid.
     */
    public MemberRecord loadMemberForSession (String sessionToken)
        throws PersistenceException
    {
        SessionRecord session = load(SessionRecord.class, sessionToken);
        return (session == null) ? null :
            load(MemberRecord.class, session.memberId);
    }

    /**
     * Creates a mapping from the supplied memberId to a session token (or reuses an existing
     * mapping). The member is assumed to have provided valid credentials and we will allow anyone
     * who presents the returned session token access as the specified member. If an existing
     * session is reused, its expiration date will be adjusted as if the session was newly created
     * as of now (using the supplied <code>persist</code> setting).
     */
    public String startOrJoinSession (int memberId, int expireDays)
        throws PersistenceException
    {
        // create a new session record for this member
        SessionRecord nsess = new SessionRecord();
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.add(Calendar.DATE, expireDays);
        nsess.expires = new Date(cal.getTimeInMillis());
        nsess.memberId = memberId;
        nsess.token = StringUtil.md5hex("" + memberId + now + Math.random());

        try {
            insert(nsess);
        } catch (DuplicateKeyException dke) {
            // if that fails with a duplicate key, reuse the old record but adjust its expiration
            SessionRecord esess = load(
                SessionRecord.class, new Where(SessionRecord.MEMBER_ID_C, memberId));
            esess.expires = nsess.expires;
            update(esess, SessionRecord.EXPIRES);

            // then, use the existing record
            nsess = esess;
        }

        return nsess.token;
    }

    /**
     * Refreshes a session using the supplied authentication token.
     *
     * @return the member associated with the session if it is valid and was refreshed, null if the
     * session has expired.
     */
    public MemberRecord refreshSession (String token, int expireDays)
        throws PersistenceException
    {
        SessionRecord sess = load(SessionRecord.class, token);
        if (sess == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, expireDays);
        sess.expires = new Date(cal.getTimeInMillis());
        update(sess);
        return loadMember(sess.memberId);
    }

    /**
     * Clears out a session to member id mapping. This should be called when a user logs off.
     */
    public void clearSession (String sessionToken)
        throws PersistenceException
    {
        delete(SessionRecord.class, sessionToken);
    }

    /**
     * Insert a new member record into the repository and assigns them a unique member id in the
     * process. The {@link MemberRecord#created} field will be filled in by this method if it is
     * not already.
     */
    public void insertMember (MemberRecord member)
        throws PersistenceException
    {
        if (member.created == null) {
            long now = System.currentTimeMillis();
            member.created = new Date(now);
            member.lastSession = new Timestamp(now);
            member.lastHumanityAssessment = new Timestamp(now);
            member.humanity = MemberObject.MAX_HUMANITY/2;
        }
        insert(member);
    }

    /**
     * Configures a member's account name (email address).
     */
    public void configureAccountName (int memberId, String accountName)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.ACCOUNT_NAME, accountName);
    }

    /**
     * Configures a member's display name.
     */
    public void configureDisplayName (int memberId, String name)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.NAME, name);
    }

    /**
     * Configures a member's permanent name.
     */
    public void configurePermaName (int memberId, String permaName)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.PERMA_NAME, permaName);
    }

    /**
     * Configures a member's avatar.
     */
    public void configureAvatarId (int memberId, int avatarId)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.AVATAR_ID, avatarId);
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
     * Set the home scene id for the specified memberId.
     */
    public void setHomeSceneId (int memberId, int homeSceneId)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.HOME_SCENE_ID, homeSceneId);
    }

    /**
     * Mimics the disabling of deleted members by renaming them to an invalid value that we do in
     * our member management system. This is triggered by us receiving a member action indicating
     * that the member was deleted.
     */
    public void disableMember (String accountName, String disabledName)
        throws PersistenceException
    {
        // TODO: Cache Invalidation
        int mods = updatePartial(
            MemberRecord.class, new Where(MemberRecord.ACCOUNT_NAME_C, accountName), null,
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
            log.warning("Attempt to disable member account resulted in weirdness " +
                        "[aname=" + accountName + ", dname=" + disabledName +
                        ", mods=" + mods + "].");
            break;
        }
    }

    /**
     * Note that a member's session has ended: increment their sessions, add in the number of
     * minutes spent online, and set their last session time to now. We also test to see if it
     * is time to reassess this member's humanity.
     *
     * @param minutes the duration of the session in minutes.
     * @param humanityReassessFreq the number of seconds between humanity reassessments or zero if
     * humanity assessment is disabled.
     */
    public void noteSessionEnded (int memberId, int minutes, int humanityReassessFreq)
        throws PersistenceException
    {
        long now = System.currentTimeMillis();
        MemberRecord record = loadMember(memberId);
        Timestamp nowStamp = new Timestamp(now);

        // reassess their humanity if the time has come
        int secsSinceLast = (int)((now - record.lastHumanityAssessment.getTime())/1000);
        if (humanityReassessFreq > 0 && humanityReassessFreq < secsSinceLast) {
            record.humanity = _flowRepo.assessHumanity(memberId, record.humanity, secsSinceLast);
            record.lastHumanityAssessment = nowStamp;
        }

// TEMP: disabled
//         // expire flow without updating MemberObject, since we're dropping session anyway
//         _flowRepo.expireFlow(record, minutes);
// END TEMP

        record.sessions ++;
        record.sessionMinutes += minutes;
        record.lastSession = nowStamp;
        update(record);
    }

    /**
     * Note that a game has ended: potentially reassess its anti-abuse factor.
     */
    public void noteGameEnded (int gameId, int playerMinutes)
        throws PersistenceException
    {
        _flowRepo.maybeAssessAntiAbuseFactor(gameId, playerMinutes);
    }

    /**
     * Returns the NeighborFriendRecords for all the established friends of a given member, through
     * an inner join between {@link MemberRecord} and {@link FriendRecord}.
     */
    public List<NeighborFriendRecord> getNeighborhoodFriends (final int memberId)
        throws PersistenceException
    {
        SQLOperator joinCondition =
            new Or(new And(new Equals(FriendRecord.INVITER_ID_C, memberId),
                           new Equals(FriendRecord.INVITEE_ID_C, MemberRecord.MEMBER_ID_C)),
                   new And(new Equals(FriendRecord.INVITEE_ID_C, memberId),
                           new Equals(FriendRecord.INVITER_ID_C, MemberRecord.MEMBER_ID_C)));
        return findAll(
            NeighborFriendRecord.class,
            new FromOverride(MemberRecord.class),
            OrderBy.descending(MemberRecord.LAST_SESSION_C),
            new Join(FriendRecord.class, joinCondition));
    }

    /**
     * Returns the NeighborFriendRecords for all the given members.
     */
    public List<NeighborFriendRecord> getNeighborhoodMembers (final int[] memberIds)
        throws PersistenceException
    {
        if (memberIds.length == 0) {
            return Collections.emptyList();
        }
        Comparable[] idArr = IntListUtil.box(memberIds);
        // OK, we definitely need some kind of default class for computed classes...
        return findAll(
            NeighborFriendRecord.class,
            new FromOverride(MemberRecord.class),
            new Where(new In(MemberRecord.MEMBER_ID_C, idArr)));
    }

    /**
     * Grants the given number of invites to the given memberId
     */
    public void grantInvites (int memberId, int number)
        throws PersistenceException
    {
        InviterRecord inviterRec = load(InviterRecord.class, memberId);
        if (inviterRec != null) {
            inviterRec.invitesGranted += number;
            update(inviterRec, InviterRecord.INVITES_GRANTED);
        } else {
            inviterRec = new InviterRecord();
            inviterRec.memberId = memberId;
            inviterRec.invitesGranted = number;
            insert(inviterRec);
        }
    }

    /**
     * Grants the given number of invites to all users who's last session expired after the given
     * Timestamp.
     *
     * @param lastSession Anybody who's been logged in since this timestamp will get the invites.
     *                    If this parameter is null, everybody will get the invites.
     */
    public void grantInvites (int number, Timestamp lastSession)
        throws PersistenceException
    {
        List<MemberRecord> activeUsers;
        if (lastSession != null) {
            activeUsers = findAll(MemberRecord.class, new Where(
                new GreaterThanEquals(MemberRecord.LAST_SESSION_C, lastSession)));
        } else {
            activeUsers = findAll(MemberRecord.class);
        }

        for (MemberRecord memRec : activeUsers) {
            grantInvites(memRec.memberId, number);
        }
    }

    /**
     * Get the number of invites this member has available to send out.
     */
    public int getInvitesGranted (int memberId)
        throws PersistenceException
    {
        InviterRecord inviter = load(InviterRecord.class, memberId);
        return inviter != null ? inviter.invitesGranted : 0;
    }

    /**
     * get the total number of invites that this user has sent
     */
    public int getInvitesSent (int memberId)
        throws PersistenceException
    {
        InviterRecord inviter = load(InviterRecord.class, memberId);
        return inviter != null ? inviter.invitesSent : 0;
    }

    /**
     * Add a new invitation. Also decrements the available invitation count for the inviterId and
     * increments the number of invites sent.
     */
    public void addInvite (String inviteeEmail, int inviterId, String inviteId)
        throws PersistenceException
    {
        insert(new InvitationRecord(inviteeEmail, inviterId, inviteId));

        InviterRecord inviterRec = load(InviterRecord.class, inviterId);
        inviterRec.invitesGranted--;
        inviterRec.invitesSent++;
        update(inviterRec);
    }

    /**
     * Check if the invitation is available for use, or has been claimed already.
     */
    public boolean inviteAvailable (String inviteId)
        throws PersistenceException
    {
        return (load(InvitationRecord.class, new Where(InvitationRecord.INVITE_ID_C, inviteId))).
            inviteeId == 0;
    }

    /**
     * Update the invitation indicated with the new memberId, and make friends of these people.
     */
    public void linkInvite (Invitation invite, MemberRecord member)
        throws PersistenceException
    {
        InvitationRecord invRec = load(InvitationRecord.class, InvitationRecord.getKey(
            invite.inviteeEmail, invite.inviter.getMemberId()));
        invRec.inviteeId = member.memberId;
        update(invRec, InvitationRecord.INVITEE_ID);

        inviteFriend(invite.inviter.getMemberId(), member.memberId);
    }

    /**
     * Get a list of the invites that this user has already sent out that have not yet been
     * accepted.
     */
    public List<InvitationRecord> loadPendingInvites (int memberId)
        throws PersistenceException
    {
        return findAll(InvitationRecord.class, new Where(new And(new Equals(
            InvitationRecord.INVITER_ID_C, memberId), new Equals(InvitationRecord.INVITEE_ID_C,
            0))));
    }

    /**
     * Return the InvitationRecord that corresponds to the given unique code.
     */
    public InvitationRecord loadInvite (String inviteId, boolean markViewed)
        throws PersistenceException
    {
        InvitationRecord invRec = load(
            InvitationRecord.class, new Where(InvitationRecord.INVITE_ID_C, inviteId));
        if (invRec != null && invRec.viewed == null) {
            invRec.viewed = new Timestamp((new java.util.Date()).getTime());
            update(invRec, InvitationRecord.VIEWED);
        }
        return invRec;
    }

    /**
     * Get the invite that was sent to thsi email address by this member.
     */
    public InvitationRecord loadInvite (String email, int inviterId)
        throws PersistenceException
    {
        return load(InvitationRecord.class, InvitationRecord.getKey(email, inviterId));
    }

    /**
     * Add an email address to the opt-out list.
     */
    public void addOptOutEmail (String email)
        throws PersistenceException
    {
        insert(new OptOutRecord(email));
    }

    /**
     * Returns true if the given email address is on the opt-out list
     */
    public boolean hasOptedOut (String email)
        throws PersistenceException
    {
        return load(OptOutRecord.class, email) != null;
    }

    /**
     * Adds the invitee's email address to the opt-out list, and sets this invitation's inviteeId
     * to -1, indicating that it is no longer available, and the invitee chose to opt-out.
     */
    public void optOutInvite (Invitation invite)
        throws PersistenceException
    {
        addOptOutEmail(invite.inviteeEmail);
        InvitationRecord invRec = loadInvite(invite.inviteId, false);
        if (invRec != null) {
            invRec.inviteeId = -1;
            update(invRec, InvitationRecord.INVITEE_ID);
        }
    }

    /**
     * Sets the reported level for the given member
     */
    public void setUserLevel (int memberId, int level)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.LEVEL, level);
    }

    public List<MemberInviteStatusRecord> getMembersInvitedBy (int memberId)
        throws PersistenceException
    {
        return findAll(MemberInviteStatusRecord.class,
                       new FromOverride(MemberRecord.class),
                       new Join(MemberRecord.MEMBER_ID_C, InviterRecord.MEMBER_ID_C).
                            setType(Join.Type.LEFT_OUTER),
                       new Where(MemberRecord.INVITING_FRIEND_ID_C, memberId),
                       new FieldOverride(MemberInviteStatusRecord.INVITES_GRANTED, 
                            InviterRecord.INVITES_GRANTED_C),
                       new FieldOverride(MemberInviteStatusRecord.INVITES_SENT,
                            InviterRecord.INVITES_SENT_C));
    }

    @Entity @Computed
    protected static class FriendCount extends PersistentRecord
    {
        public static final String COUNT = "count";
        @Computed
        public int count;
    }

    /**
     * Determine what the friendship status is between one member and another.
     */
    public boolean getFriendStatus (int firstId, int secondId)
        throws PersistenceException
    {
        List<FriendRecord> friends = findAll(
            FriendRecord.class,
            new Where(new And(new Or(new And(new Equals(FriendRecord.INVITER_ID_C, firstId),
                                             new Equals(FriendRecord.INVITEE_ID_C, secondId)),
                                     new And(new Equals(FriendRecord.INVITER_ID_C, secondId),
                                             new Equals(FriendRecord.INVITEE_ID_C, firstId))))));
        return friends.size() > 0;
    }

    /**
     * Get the FriendEntry record for all friends (pending, too) of the specified memberId. The
     * online status of each friend will be false.
     *
     * The {@link FriendEntry} records returned by this method should be considered read-only,
     * and must be cloned before they are modified or sent to untrusted code.
     */
    public List<FriendEntry> getFriends (final int memberId)
        throws PersistenceException
    {
        // force the creation of the FriendRecord table if necessary
        _ctx.getMarshaller(FriendRecord.class);

        CacheKey key = new SimpleCacheKey(FRIENDS_CACHE_ID, memberId);
        return _ctx.invoke(new CollectionQuery<List<FriendEntry>>(key) {
            public List<FriendEntry> invoke (Connection conn)
                throws SQLException
            {
                String query = "select name, memberId, inviterId from FriendRecord " +
                    "inner join MemberRecord on " +
                    "((inviterId=" + memberId + " and memberId=inviteeId) or" +
                    " (inviteeId=" + memberId + " and memberId=inviterId)) order by name";
                ArrayList<FriendEntry> list = new ArrayList<FriendEntry>();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        MemberName name = new MemberName(rs.getString(1), rs.getInt(2));
                        list.add(new FriendEntry(name, false));
                    }
                    return list;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }

            // from Query
            public List<FriendEntry> transformCacheHit (CacheKey key, List<FriendEntry> value) {
                // we do not clone this result
                return value;

            }
        });
    }

    /**
     * Invite or approve the specified member as our friend.
     *
     * @param memberId The id of the member performing this action.
     * @param otherId The id of the other member.
     *
     * @return the new FriendEntry record for this friendship (modulo online information), or null
     * if there was an error finding the friend.
     */
    public FriendEntry inviteFriend (int memberId,  int otherId)
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
                                new Where(FriendRecord.INVITER_ID_C, memberId,
                                          FriendRecord.INVITEE_ID_C, otherId)));
        existing.addAll(findAll(FriendRecord.class,
                                new Where(FriendRecord.INVITER_ID_C, otherId,
                                          FriendRecord.INVITEE_ID_C, memberId)));

        // invalidate the FriendsCache for both members
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, memberId));
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, otherId));

        if (existing.size() == 0) {
            // there is no connection yet: add the other
            FriendRecord rec = new FriendRecord();
            rec.inviterId = memberId;
            rec.inviteeId = otherId;
            insert(rec);
        }
        return new FriendEntry(other.getName(), false);
    }

    /**
     * Remove a friend mapping from the database.
     */
    public void removeFriends (final int memberId, final int otherId)
        throws PersistenceException
    {
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, memberId));
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, otherId));

        Key key = FriendRecord.getKey(memberId, otherId);
        deleteAll(FriendRecord.class, key, key);

        key = FriendRecord.getKey(otherId, memberId);
        deleteAll(FriendRecord.class, key, key);
    }

    /**
     * Delete all the friend relations involving the specified memberId, usually because that
     * member is being deleted.
     */
    public void deleteAllFriends (final int memberId)
        throws PersistenceException
    {
        CacheInvalidator invalidator = new CacheInvalidator() {
            public void invalidate (PersistenceContext ctx) {
                // remove the FriendsCache entry for the member
                ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, memberId));

                // then remove both FriendRecord and FriendsCache entries for all related members
                ctx.cacheTraverse(FriendRecord.class, new CacheTraverser<FriendRecord> () {
                    public void visitCacheEntry (PersistenceContext ctx, String cacheId,
                        Serializable key, FriendRecord record) {
                        if (record.inviteeId == memberId) {
                            ctx.cacheInvalidate(FRIENDS_CACHE_ID, record.inviterId);
                            ctx.cacheInvalidate(FriendRecord.class, record.inviterId);
                        } else if (record.inviterId == memberId) {
                            ctx.cacheInvalidate(FRIENDS_CACHE_ID, record.inviterId);
                            ctx.cacheInvalidate(FriendRecord.class, record.inviterId);
                        }
                    }
                });
            }
        };

        deleteAll(FriendRecord.class,
                  new Where(new Or(new Equals(FriendRecord.INVITER_ID_C, memberId),
                                   new Equals(FriendRecord.INVITEE_ID_C, memberId))),
                  invalidator);
    }

    protected FlowRepository _flowRepo;
}
