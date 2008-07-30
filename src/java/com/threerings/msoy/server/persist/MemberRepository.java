//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.PersistenceContext.CacheTraverser;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.SimpleCacheKey;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.web.data.MemberCard;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
@Singleton @BlockingThread
public class MemberRepository extends DepotRepository
{
    /** The cache identifier for the friends-of-a-member collection query. */
    public static final String FRIENDS_CACHE_ID = "FriendsCache";

    @Inject public MemberRepository (PersistenceContext ctx)
    {
        super(ctx);

        // add a cache invalidator that listens to single FriendRecord updates
        _ctx.addCacheListener(FriendRecord.class, new CacheListener<FriendRecord>() {
            public void entryInvalidated (CacheKey key, FriendRecord friend) {
                _ctx.cacheInvalidate(FRIENDS_CACHE_ID, friend.inviterId);
                _ctx.cacheInvalidate(FRIENDS_CACHE_ID, friend.inviteeId);
            }
            public void entryCached (CacheKey key, FriendRecord newEntry, FriendRecord oldEntry) {
                // nothing to do here
            }
            public String toString () {
                return "FriendRecord -> FriendsCache";
            }
        });

        // add a cache invalidator that listens to MemberRecord updates
        _ctx.addCacheListener(MemberRecord.class, new CacheListener<MemberRecord>() {
            public void entryInvalidated (CacheKey key, MemberRecord member) {
                _ctx.cacheInvalidate(MemberNameRecord.getKey(member.memberId));
            }
            public void entryCached (CacheKey key, MemberRecord newEntry, MemberRecord oldEntry) {
            }
            public String toString () {
                return "MemberRecord -> MemberNameRecord";
            }
        });

        // TEMP added 2008.3.15
        _ctx.registerMigration(MemberRecord.class, new EntityMigration(15) {
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String tName = liaison.tableSQL("MemberRecord");
                String cName = liaison.columnSQL(MemberRecord.EXPERIENCES);
                Statement stmt = conn.createStatement();
                try {
                    int rows = stmt.executeUpdate(
                        "UPDATE " + tName + " set " + cName + " = 0");
                    log.info("Cleared experiences from " + rows + " members.");
                    return rows;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
            public boolean runBeforeDefault () {
                return false;
            }
        });
        // END TEMP

        // TEMP added 2008.2.13
        _ctx.registerMigration(MemberRecord.class, new EntityMigration(12) {
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String tName = liaison.tableSQL("MemberRecord");
                String cName = liaison.columnSQL("accountName");

                Statement stmt = conn.createStatement();
                try {
                    int rows = stmt.executeUpdate(
                        "UPDATE " + tName + " set " + cName + " = LOWER(" + cName + ")");
                    log.info("Lowercased " + rows + " accountName rows in MemberRecord");
                    return rows;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        _ctx.registerMigration(OptOutRecord.class, new EntityMigration(2) {
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String tName = liaison.tableSQL("OptOutRecord");
                String cName = liaison.columnSQL("email");

                Statement stmt = conn.createStatement();
                try {
                    int rows = stmt.executeUpdate(
                        "UPDATE " + tName + " set " + cName + " = LOWER(" + cName + ")");
                    log.info("Lowercased " + rows + " email rows in OptOutRecord");
                    return rows;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        _ctx.registerMigration(MemberRecord.class, new EntityMigration(19) {
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String tName = liaison.tableSQL("MemberRecord");
                String cName = liaison.columnSQL("permaName");

                Statement stmt = conn.createStatement();
                try {
                    int rows = stmt.executeUpdate(
                        "UPDATE " + tName + " set " + cName + " = LOWER(" + cName + ")");
                    log.info("Lowercased " + rows + " permaName rows in MemberRecord");
                    return rows;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        _ctx.registerMigration(MemberRecord.class, new EntityMigration(20) {
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String tName = liaison.tableSQL("MemberRecord");
                String cName = liaison.columnSQL("permaName");

                Statement stmt = conn.createStatement();
                try {
                    int rows = stmt.executeUpdate(
                        "UPDATE " + tName + " set " + cName + " = LOWER(" + cName + ")");
                    log.info("Lowercased " + rows + " permaName rows in MemberRecord");
                    return rows;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        _ctx.registerMigration(MemberRecord.class, new EntityMigration.Drop(21,
            "normalizedPermaName"));

        // END TEMP

        // TEMP added 2008.2.21
        _ctx.registerMigration(MemberRecord.class, new EntityMigration.Drop(13, "avrGameId"));
        // END TEMP
    }

    /**
     * Returns the repository used by this repository to manage flow.
     */
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
        return load(MemberRecord.class,
                    new Where(MemberRecord.ACCOUNT_NAME_C, accountName.toLowerCase()));
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
     * Loads the member records with the supplied set of ids.
     */
    public List<MemberRecord> loadMembers (Set<Integer> memberIds)
        throws PersistenceException
    {
        if (memberIds.isEmpty()) {
            return Collections.emptyList();
        }
        return findAll(MemberRecord.class, new Where(new In(MemberRecord.MEMBER_ID_C, memberIds)));
    }

    /**
     * Returns the total number of members in Whirled.
     */
    public int getPopulationCount ()
        throws PersistenceException
    {
        return load(CountRecord.class, new FromOverride(MemberRecord.class)).count;
    }

    /**
     * Calculate a count of the active member population, currently defined as anybody
     * whose last session is within the past 60 days.
     */
    public int getActivePopulationCount ()
        throws PersistenceException
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -60); // TODO: unmagick
        Date when = new Date(cal.getTimeInMillis());
        return load(CountRecord.class, new FromOverride(MemberRecord.class),
                    new Where(new GreaterThan(MemberRecord.LAST_SESSION_C, // TODO: DateExp?
                                              new LiteralExp("'" + when + "'")))).count;
    }

    /**
     * Looks up a member's name by id. Returns null if no member exists with the specified id.
     */
    public MemberName loadMemberName (int memberId)
        throws PersistenceException
    {
        MemberNameRecord name = load(MemberNameRecord.class,
                                     new Where(MemberRecord.MEMBER_ID_C, memberId));
        return (name == null) ? null : name.toMemberName();
    }

    /**
     * Extracts the set of member id from the supplied collection of records using the supplied
     * <code>getId</code> function and loads up the associated names.
     */
    public <C> IntMap<MemberName> loadMemberNames (Iterable<C> records, Function<C,Integer> getId)
        throws PersistenceException
    {
        Set<Integer> memberIds = new ArrayIntSet();
        for (C record : records) {
            memberIds.add(getId.apply(record));
        }
        return loadMemberNames(memberIds);
    }

    /**
     * Looks up members' names by id.
     *
     * TODO: Implement findAll(Persistent.class, Comparable... keys) or the like,
     *       as per MDB's suggestion, say so we can cache properly.
     */
    public IntMap<MemberName> loadMemberNames (Set<Integer> memberIds)
        throws PersistenceException
    {
        IntMap<MemberName> names = IntMaps.newHashIntMap();
        if (memberIds.size() > 0) {
            for (MemberNameRecord name : findAll(
                     MemberNameRecord.class,
                     new Where(new In(MemberRecord.MEMBER_ID_C, memberIds)))) {
                names.put(name.memberId, name.toMemberName());
            }
        }
        return names;
    }

    /**
     * Looks up a member name and id from by username.
     */
    public MemberName loadMemberName (String username)
        throws PersistenceException
    {
        MemberNameRecord record = load(MemberNameRecord.class,
                new Where(MemberRecord.ACCOUNT_NAME_C, username));
        return (record == null ? null : record.toMemberName());
    }

    /**
     * Loads up the card for the specified member. Returns null if no member exists with that id.
     */
    public MemberCard loadMemberCard (int memberId)
        throws PersistenceException
    {
        MemberCardRecord mcr = load(
            MemberCardRecord.class, new FromOverride(MemberRecord.class),
            new Join(MemberRecord.MEMBER_ID_C, ProfileRecord.MEMBER_ID_C),
            new Where(MemberRecord.MEMBER_ID_C, memberId));
        return (mcr == null) ? null : mcr.toMemberCard();
    }

    /**
     * Loads up member's names and profile photos by id.
     */
    public List<MemberCardRecord> loadMemberCards (Set<Integer> memberIds)
        throws PersistenceException
    {
        if (memberIds.size() == 0) {
            return Collections.emptyList();
        }
        return findAll(MemberCardRecord.class,
                       new FromOverride(MemberRecord.class),
                       new Join(MemberRecord.MEMBER_ID_C, ProfileRecord.MEMBER_ID_C),
                       new Where(new In(MemberRecord.MEMBER_ID_C, memberIds)));
    }

    /**
     * Returns ids for all members who's display name matches the supplied search string.
     */
    public List<Integer> findMembersByDisplayName (String search, boolean exact, int limit)
        throws PersistenceException
    {
        search = search.toLowerCase();

        SQLOperator op;
        if (exact) {
            op = new Equals(new FunctionExp("LOWER", MemberRecord.NAME_C), search);
        } else {
            op = new FullTextMatch(MemberRecord.class, MemberRecord.FTS_NAME, search);
        }
        List<Integer> ids = Lists.newArrayList();

        // TODO: turn this into a findAllKeys query
//         for (Key<MemberRecord> key :
//                  findAllKeys(MemberRecord.class, where, new Limit(0, limit))) {
//             ids.add((Integer)key.condition.getValues().get(0));
//         }
        for (MemberRecord mrec : findAll(MemberRecord.class, new Where(op), new Limit(0, limit))) {
            ids.add(mrec.memberId);
        }

        return ids;
    }

    /**
     * Returns the members with the highest levels
     */
    public List<Integer> getLeadingMembers (int limit)
        throws PersistenceException
    {
        List<Integer> ids = Lists.newArrayList();
        List<MemberRecord> mrecs = findAll(
            MemberRecord.class, OrderBy.descending(MemberRecord.LEVEL_C), new Limit(0, limit));
        for (MemberRecord mrec : mrecs) {
            ids.add(mrec.memberId);
        }

        return ids;
    }

    /**
     * Loads up the member associated with the supplied session token. Returns null if the session
     * has expired or is not valid.
     */
    public MemberRecord loadMemberForSession (String sessionToken)
        throws PersistenceException
    {
        SessionRecord session = load(SessionRecord.class, sessionToken);
        if (session != null && session.expires.getTime() < System.currentTimeMillis()) {
            session = null;
        }
        return (session == null) ? null : load(MemberRecord.class, session.memberId);
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
     * Clears out a session to member id mapping.
     */
    public void clearSession (int memberId)
        throws PersistenceException
    {
        SessionRecord record =
            load(SessionRecord.class, new Where(SessionRecord.MEMBER_ID_C, memberId));
        if (record != null) {
            delete(record);
        }
    }

    /**
     * Insert a new member record into the repository and assigns them a unique member id in the
     * process. The {@link MemberRecord#created} field will be filled in by this method if it is
     * not already.
     */
    public void insertMember (MemberRecord member)
        throws PersistenceException
    {
        // flatten account name (email address) on insertion
        member.accountName = member.accountName.toLowerCase();
        if (member.created == null) {
            long now = System.currentTimeMillis();
            member.created = new Date(now);
            member.lastSession = new Timestamp(now);
            member.lastHumanityAssessment = new Timestamp(now);
            member.humanity = MsoyCodes.STARTING_HUMANITY;
        }
        insert(member);
    }

    /**
     * Configures a member's account name (email address).
     */
    public void configureAccountName (int memberId, String accountName)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId,
                      MemberRecord.ACCOUNT_NAME, accountName.toLowerCase());
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
        // permaName will be a non-null lower-case string
        updatePartial(MemberRecord.class, memberId, MemberRecord.PERMA_NAME, permaName);
    }

    /**
     * Writes the supplied member's flags to the database.
     */
    public void storeFlags (MemberRecord mrec)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, mrec.memberId, MemberRecord.FLAGS, mrec.flags);
    }

    /**
     * Writes the supplied member's experiences to the database.
     */
    public void storeExperiences (MemberRecord mrec)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, mrec.memberId, MemberRecord.EXPERIENCES, mrec.experiences);
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

        // TODO: delete a whole bunch of shit (not here, in whatever ends up calling this)
        // - inventory items
        // - item tags
        // - item ratings
        // - game ratings
        // - game cookies
        // - trophies
        // - comments
        // - rooms, furni, etc.
        // - mail messages
        // - profile data
        // - swiftly projects (?)
        // - invitations
        // - friendships
        // - group memberships
        // - member action records, action summary record
        // - thread read tracking info
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
            MemberRecord.class, new Where(MemberRecord.ACCOUNT_NAME_C, accountName.toLowerCase()),
            null, MemberRecord.ACCOUNT_NAME, disabledName);
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
        Comparable<?>[] idArr = IntListUtil.box(memberIds);
        return findAll(
            NeighborFriendRecord.class,
            new FromOverride(MemberRecord.class),
            new Where(new In(MemberRecord.MEMBER_ID_C, idArr)));
    }

    /**
     * Grants the specified number of invites to the given member.
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
     * Grants the given number of invites to all users whose last session expired after the given
     * Timestamp.
     *
     * @param lastSession Anybody who's been logged in since this timestamp will get the invites.
     *                    If this parameter is null, everybody will get the invites.
     *
     * @return an array containing the member ids of all members that received invites.
     */
    public int[] grantInvites (int number, Timestamp lastSession)
        throws PersistenceException
    {
        List<MemberRecord> activeUsers;
        if (lastSession != null) {
            activeUsers = findAll(MemberRecord.class, new Where(
                new GreaterThanEquals(MemberRecord.LAST_SESSION_C, lastSession)));
        } else {
            activeUsers = findAll(MemberRecord.class);
        }

        IntSet ids = new ArrayIntSet();
        for (MemberRecord memRec : activeUsers) {
            grantInvites(memRec.memberId, number);
            ids.add(memRec.memberId);
        }
        return ids.toIntArray();
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

    public String generateInviteId ()
        throws PersistenceException
    {
        // find a free invite id
        String inviteId;
        int tries = 0;
        while (loadInvite(inviteId = randomInviteId(), false) != null) {
            tries++;
        }
        if (tries > 5) {
            log.warning("InvitationRecord.inviteId space is getting saturated, it took " + tries +
                " tries to find a free id");
        }
        return inviteId;
    }

    /**
     * Add a new invitation. Also decrements the available invitation count for the inviterId and
     * increments the number of invites sent, iff the inviterId is non-zero.
     */
    public void addInvite (String inviteeEmail, int inviterId, String inviteId)
        throws PersistenceException
    {
        insert(new InvitationRecord(inviteeEmail, inviterId, inviteId));

        if (inviterId > 0) {
            InviterRecord inviterRec = load(InviterRecord.class, inviterId);
            if (inviterRec == null) {
                inviterRec = new InviterRecord();
                inviterRec.memberId = inviterId;
            }
// TODO: nix this when we nix invite limiting
//             inviterRec.invitesGranted--;
            inviterRec.invitesSent++;
            store(inviterRec);
        }
    }

    /**
     * Check if the invitation is available for use, or has been claimed already. Returns null if
     * it has already been claimed, an invite record if not.
     */
    public InvitationRecord inviteAvailable (String inviteId)
        throws PersistenceException
    {
        InvitationRecord rec = load(InvitationRecord.class,
                                    new Where(InvitationRecord.INVITE_ID_C, inviteId));
        return (rec == null || rec.inviteeId != 0) ? null : rec;
    }

    /**
     * Update the invitation indicated with the new memberId.
     */
    public void linkInvite (String inviteId, MemberRecord member)
        throws PersistenceException
    {
        InvitationRecord invRec = load(InvitationRecord.class, inviteId);
        invRec.inviteeId = member.memberId;
        update(invRec, InvitationRecord.INVITEE_ID);
    }

    /**
     * Get a list of the invites that this user has already sent out that have not yet been
     * accepted.
     */
    public List<InvitationRecord> loadPendingInvites (int memberId)
        throws PersistenceException
    {
        return findAll(
            InvitationRecord.class,
            new Where(InvitationRecord.INVITER_ID_C, memberId,
                      InvitationRecord.INVITEE_ID_C, 0));
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
     * Return the InvitationRecord that corresponds to the given inviter
     */
    public InvitationRecord loadInvite (String inviteeEmail, int inviterId)
        throws PersistenceException
    {
        // TODO: This does a row scan on email after using ixInviter. Should be OK, but let's check.
        return load(InvitationRecord.class, new Where(
            InvitationRecord.INVITEE_EMAIL_C, inviteeEmail,
            InvitationRecord.INVITER_ID_C, inviterId));
    }

    /**
     * Delete the InvitationRecord that corresponds to the given unique code.
     */
    public void deleteInvite (String inviteId)
        throws PersistenceException
    {
        delete(InvitationRecord.class, inviteId);
    }

    /**
     * Add an email address to the opt-out list.
     */
    public void addOptOutEmail (String email)
        throws PersistenceException
    {
        insert(new OptOutRecord(email.toLowerCase()));
    }

    /**
     * Returns true if the given email address is on the opt-out list
     */
    public boolean hasOptedOut (String email)
        throws PersistenceException
    {
        return load(OptOutRecord.class, email.toLowerCase()) != null;
    }

    /**
     * Adds the invitee's email address to the opt-out list, and sets this invitation's inviteeId
     * to -1, indicating that it is no longer available, and the invitee chose to opt-out.
     */
    public void optOutInvite (String inviteId)
        throws PersistenceException
    {
        InvitationRecord invRec = loadInvite(inviteId, false);
        if (invRec != null) {
            invRec.inviteeId = -1;
            update(invRec, InvitationRecord.INVITEE_ID);
            addOptOutEmail(invRec.inviteeEmail);
        }
    }

    /**
     * Loads up a referral record by member record id.
     * Returns null if no record exists for that member.
     */
    public ReferralRecord loadReferral (int memberId)
        throws PersistenceException
    {
        return load(ReferralRecord.class, memberId);
    }

    /**
     * Adds or updates the referral record for a member with the given id.
     */
    public ReferralRecord setReferral (int memberId, ReferralInfo ref)
        throws PersistenceException
    {
        ReferralRecord newrec = ReferralRecord.fromInfo(memberId, ref);
        if (loadReferral(memberId) == null) {
            insert(newrec);
        } else {
            update(newrec);
        }

        return newrec;
    }

    /**
     * Sets the reported level for the given member
     */
    public void setUserLevel (int memberId, int level)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.LEVEL, level);
    }

    /**
     * Loads the names of the members invited by the specified member.
     */
    public List<MemberName> loadMembersInvitedBy (int memberId)
        throws PersistenceException
    {
        Join join = new Join(MemberRecord.MEMBER_ID_C,
                             InviterRecord.MEMBER_ID_C).setType(Join.Type.LEFT_OUTER);
        Where where = new Where(MemberRecord.INVITING_FRIEND_ID_C, memberId);
        List<MemberName> names = Lists.newArrayList();
        for (MemberNameRecord name : findAll(MemberNameRecord.class, join, where)) {
            names.add(name.toMemberName());
        }
        return names;
    }

    public List<MemberInviteStatusRecord> getMembersInvitedBy (int memberId)
        throws PersistenceException
    {
        return findAll(MemberInviteStatusRecord.class,
                       new Join(MemberRecord.MEMBER_ID_C, InviterRecord.MEMBER_ID_C).
                            setType(Join.Type.LEFT_OUTER),
                       new Where(MemberRecord.INVITING_FRIEND_ID_C, memberId));
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
     * Loads the member ids of the specified member's friends.
     */
    public IntSet loadFriendIds (int memberId)
        throws PersistenceException
    {
        IntSet memIds = new ArrayIntSet();
        SQLExpression condition =
            new Or(new Equals(FriendRecord.INVITER_ID_C, memberId),
                   new Equals(FriendRecord.INVITEE_ID_C, memberId));
        for (FriendRecord record : findAll(FriendRecord.class, new Where(condition))) {
            memIds.add(record.inviterId == memberId ? record.inviteeId : record.inviterId);
        }
        return memIds;
    }

    /**
     * Loads the FriendEntry record for all friends of the specified member. The online status of
     * each friend will be false. The friends will be returned in order of most recently online to
     * least.
     *
     * TODO: Bring back full collection caching to this method.
     *
     * @param limit a limit on the number of friends to load or 0 for all of them.
     */
    public List<FriendEntry> loadFriends (int memberId, int limit)
        throws PersistenceException
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(FriendRecord.class));
        SQLExpression condition = new And(
            new Or(new And(new Equals(FriendRecord.INVITER_ID_C, memberId),
                           new Equals(MemberRecord.MEMBER_ID_C, FriendRecord.INVITEE_ID_C)),
                   new And(new Equals(FriendRecord.INVITEE_ID_C, memberId),
                           new Equals(MemberRecord.MEMBER_ID_C, FriendRecord.INVITER_ID_C))));
        clauses.add(new Join(MemberRecord.class, condition));
        clauses.add(new Join(MemberRecord.MEMBER_ID_C, ProfileRecord.MEMBER_ID_C));
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }
        clauses.add(OrderBy.descending(MemberRecord.LAST_SESSION_C));
        List<MemberCardRecord> records = findAll(MemberCardRecord.class, clauses);
        List<FriendEntry> list = Lists.newArrayList();
        for (MemberCardRecord record : records) {
            MemberCard card = record.toMemberCard();
            list.add(new FriendEntry(card.name, false, card.photo, card.headline));
        }
        return list;
    }

    /**
     * Makes the specified members friends.
     *
     * @param memberId The id of the member performing this action.
     * @param otherId The id of the other member.
     *
     * @return the member card for the invited friend, or null if the invited friend no longer
     * exists.
     * @exception DuplicateKeyException if the members are already friends.
     */
    public MemberCard noteFriendship (int memberId,  int otherId)
        throws PersistenceException
    {
        // first load the member record of the potential friend
        MemberCard other = loadMemberCard(otherId);
        if (other == null) {
            log.warning("Failed to establish friends: member no longer exists " +
                        "[missingId=" + otherId + ", reqId=" + memberId + "].");
            return null;
        }

        // see if there is already a connection, either way
        List<FriendRecord> existing = Lists.newArrayList();
        existing.addAll(findAll(FriendRecord.class,
                                new Where(FriendRecord.INVITER_ID_C, memberId,
                                          FriendRecord.INVITEE_ID_C, otherId)));
        existing.addAll(findAll(FriendRecord.class,
                                new Where(FriendRecord.INVITER_ID_C, otherId,
                                          FriendRecord.INVITEE_ID_C, memberId)));

        // invalidate the FriendsCache for both members
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, memberId));
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, otherId));

        // if they already have a connection, let the caller know by excepting
        if (existing.size() > 0) {
            throw new DuplicateKeyException(memberId + " and " + otherId + " are already friends");
        }

        FriendRecord rec = new FriendRecord();
        rec.inviterId = memberId;
        rec.inviteeId = otherId;
        insert(rec);

        return other;
    }

    /**
     * Remove a friend mapping from the database.
     */
    public void clearFriendship (final int memberId, final int otherId)
        throws PersistenceException
    {
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, memberId));
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, otherId));

        Key<FriendRecord> key = FriendRecord.getKey(memberId, otherId);
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

    /**
     * Returns the id of the account associated with the supplied external account (the caller is
     * responsible for confirming the authenticity of the external id information) or 0 if no
     * account is associated with that external account.
     */
    public int lookupExternalAccount (int partnerId, String externalId)
        throws PersistenceException
    {
        ExternalMapRecord record =
            load(ExternalMapRecord.class, ExternalMapRecord.getKey(partnerId, externalId));
        return (record == null) ? 0 : record.memberId;
    }

    /**
     * Notes that the specified Whirled account is associated with the specified external account.
     */
    public void mapExternalAccount (int partnerId, String externalId, int memberId)
        throws PersistenceException
    {
        ExternalMapRecord record = new ExternalMapRecord();
        record.partnerId = partnerId;
        record.externalId = externalId;
        record.memberId = memberId;
        insert(record);
    }

    /**
     * Creates a temp ban record for a member, or updates a pre-existing temp ban record.
     */
    public void tempBanMember (int memberId, Timestamp expires, String warning)
        throws PersistenceException
    {
        MemberWarningRecord record = new MemberWarningRecord();
        record.memberId = memberId;
        record.banExpires = expires;
        record.warning = warning;
        store(record);
    }

    /**
     * Updates the warning message for a member, or creates a new warning message if none exists.
     */
    public void updateMemberWarning (int memberId, String warning)
        throws PersistenceException
    {
        if (updatePartial(MemberWarningRecord.class, memberId,
                          MemberWarningRecord.WARNING, warning) == 0) {
            MemberWarningRecord record = new MemberWarningRecord();
            record.memberId = memberId;
            record.warning = warning;
            insert(record);
        }
    }

    /**
     * Clears a warning message from a member (this includes any temp ban information).
     */
    public void clearMemberWarning (int memberId)
        throws PersistenceException
    {
        delete(MemberWarningRecord.class, memberId);
    }

    /**
     * Returns the MemberWarningRecord for the memberId, or null if none found.
     */
    public MemberWarningRecord loadMemberWarningRecord (int memberId)
        throws PersistenceException
    {
        return load(MemberWarningRecord.class, memberId);
    }

    protected String randomInviteId ()
    {
        String rand = "";
        for (int ii = 0; ii < INVITE_ID_LENGTH; ii++) {
            rand += INVITE_ID_CHARACTERS.charAt((int)(Math.random() *
                INVITE_ID_CHARACTERS.length()));
        }
        return rand;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberRecord.class);
        classes.add(FriendRecord.class);
        classes.add(SessionRecord.class);
        classes.add(InvitationRecord.class);
        classes.add(InviterRecord.class);
        classes.add(OptOutRecord.class);
        classes.add(ExternalMapRecord.class);
        classes.add(MemberWarningRecord.class);
        classes.add(ReferralRecord.class);
    }

    @Inject protected FlowRepository _flowRepo;

    protected static final int INVITE_ID_LENGTH = 10;
    protected static final String INVITE_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
}
