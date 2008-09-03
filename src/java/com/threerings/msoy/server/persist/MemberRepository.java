//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
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
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.DuplicateKeyException;
import com.samskivert.jdbc.depot.EntityMigration;
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
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.FullTextMatch;
import com.samskivert.jdbc.depot.operator.Conditionals.GreaterThan;
import com.samskivert.jdbc.depot.operator.Conditionals.GreaterThanEquals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.samskivert.jdbc.depot.operator.Logic.Or;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;

import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.web.data.MemberCard;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
@Singleton @BlockingThread
public class MemberRepository extends DepotRepository
{
    /** The cache identifier for the friends-of-a-member collection query. */
    public static final String FRIENDS_CACHE_ID = "FriendsCache";

    /** Used by {@link #runMemberMigration}. */
    public static interface MemberMigration {
        public void apply (MemberRecord record) throws PersistenceException;
    };

    @Inject public MemberRepository (final PersistenceContext ctx)
    {
        super(ctx);

        // add a cache invalidator that listens to single FriendRecord updates
        _ctx.addCacheListener(FriendRecord.class, new CacheListener<FriendRecord>() {
            public void entryInvalidated (final CacheKey key, final FriendRecord friend) {
                _ctx.cacheInvalidate(FRIENDS_CACHE_ID, friend.inviterId);
                _ctx.cacheInvalidate(FRIENDS_CACHE_ID, friend.inviteeId);
            }
            public void entryCached (final CacheKey key, final FriendRecord newEntry,
                                     final FriendRecord oldEntry) {
                // nothing to do here
            }
            @Override
            public String toString () {
                return "FriendRecord -> FriendsCache";
            }
        });

        // add a cache invalidator that listens to MemberRecord updates
        _ctx.addCacheListener(MemberRecord.class, new CacheListener<MemberRecord>() {
            public void entryInvalidated (final CacheKey key, final MemberRecord member) {
                _ctx.cacheInvalidate(MemberNameRecord.getKey(member.memberId));
            }
            public void entryCached (final CacheKey key, final MemberRecord newEntry,
                                     final MemberRecord oldEntry) {
            }
            @Override
            public String toString () {
                return "MemberRecord -> MemberNameRecord";
            }
        });

        // 08-11-2008: This will copy the flow from member records into the member account
        // records.  From this point on, the member account records are considered canonical.
        try {
            ctx.getMarshaller(MemberAccountRecord.class);
        } catch (PersistenceException pe) {
            throw new RuntimeException("Unable to create neeed MemberAccountRecord", pe);
        }
        ctx.registerMigration(MemberRecord.class, new EntityMigration(22) {
            @Override public int invoke (final Connection conn, final DatabaseLiaison dbl)
                throws SQLException
            {
                PreparedStatement insertStmt = conn.prepareStatement(
                    "insert into " + dbl.tableSQL("MemberAccountRecord") +
                    "(" + dbl.columnSQL("memberId") + ", " + dbl.columnSQL("coins") + ", " +
                    dbl.columnSQL("bars") + ", " + dbl.columnSQL("bling") + ", " +
                    dbl.columnSQL("dateLastUpdated") + ", " + dbl.columnSQL("versionId") + ", " +
                    dbl.columnSQL("accCoins") + ", " + dbl.columnSQL("accBars") + ", " +
                    dbl.columnSQL("accBling") + ") " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                PreparedStatement updateStmt = conn.prepareStatement(
                    "update " +
                    dbl.tableSQL("MemberAccountRecord") +
                    " set " + dbl.columnSQL("coins") + " = " + dbl.columnSQL("coins") + " + ?, " +
                    dbl.columnSQL("accCoins") + " = " + dbl.columnSQL("accCoins") + " + ? " +
                    "where " + dbl.columnSQL("memberId") + " = ?");
                PreparedStatement checkStmt = conn.prepareStatement(
                    "select count(*) from " + dbl.tableSQL("MemberAccountRecord") +
                    " where " + dbl.columnSQL("memberId") + " = ?");

                Statement stmt = conn.createStatement();
                final ResultSet rs = stmt.executeQuery(
                    "select " + dbl.columnSQL("memberId") + ", " + dbl.columnSQL("flow") + ", " +
                    dbl.columnSQL("accFlow") + " from " + dbl.tableSQL("MemberRecord"));

                final List<Integer> memberIdInsertList = new ArrayList<Integer>();
                final List<Integer> memberIdUpdateList = new ArrayList<Integer>();
                while (rs.next()) {
                    final int memberId = rs.getInt(1);
                    checkStmt.setInt(1, memberId);
                    final ResultSet rs2 = checkStmt.executeQuery();
                    rs2.next();
                    if (rs2.getInt(1) > 0) {
                        memberIdUpdateList.add(memberId);
                        updateStmt.setInt(1, rs.getInt(2));
                        updateStmt.setInt(2, rs.getInt(3));
                        updateStmt.setInt(3, memberId);
                        updateStmt.addBatch();
                    } else {
                        memberIdInsertList.add(memberId);
                        insertStmt.setInt(1, memberId);
                        insertStmt.setInt(2, rs.getInt(2));
                        insertStmt.setInt(3, 0);
                        insertStmt.setDouble(4, 0.0);
                        insertStmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                        insertStmt.setInt(6, 1);
                        insertStmt.setInt(7, rs.getInt(3));
                        insertStmt.setInt(8, 0);
                        insertStmt.setDouble(9, 0.0);
                        insertStmt.addBatch();
                    }
                }
                int[] res = insertStmt.executeBatch();
                int total = 0;
                for (int i = 0; i < res.length; i++) {
                    if (res[i] >= 0) {
                        total += res[i];
                    } else {
                        log.warning("Insert for member " + memberIdInsertList.get(i) +
                                    " in the money repository migration failed with code: " +
                                    res[i]);
                    }
                }
                res = updateStmt.executeBatch();
                for (int i = 0; i < res.length; i++) {
                    if (res[i] >= 0) {
                        total += res[i];
                    } else {
                        log.warning("Update for member " + memberIdUpdateList.get(i) +
                                    " in the money repository migration failed with code: " +
                                    res[i]);
                    }
                }

                // normally these would be done in a finally block, but if this fails the whole
                // server is shutting down, so let's save ourselves one level of indentation
                JDBCUtil.close(stmt);
                JDBCUtil.close(insertStmt);
                JDBCUtil.close(updateStmt);
                JDBCUtil.close(checkStmt);
                return total;
            }
        });
    }

    /**
     * Loads up the member record associated with the specified account.  Returns null if no
     * matching record could be found.
     */
    public MemberRecord loadMember (final String accountName)
        throws PersistenceException
    {
        return load(MemberRecord.class,
                    new Where(MemberRecord.ACCOUNT_NAME_C, accountName.toLowerCase()));
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with the specified id. The
     * record will be fetched from the cache if possible and cached if not.
     */
    public MemberRecord loadMember (final int memberId)
        throws PersistenceException
    {
        return load(MemberRecord.class, memberId);
    }

    /**
     * Loads the member records with the supplied set of ids.
     */
    public List<MemberRecord> loadMembers (final Set<Integer> memberIds)
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
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -60); // TODO: unmagick
        final Date when = new Date(cal.getTimeInMillis());
        return load(CountRecord.class, new FromOverride(MemberRecord.class),
                    new Where(new GreaterThan(MemberRecord.LAST_SESSION_C, // TODO: DateExp?
                                              new LiteralExp("'" + when + "'")))).count;
    }

    /**
     * Looks up a member's name by id. Returns null if no member exists with the specified id.
     */
    public MemberName loadMemberName (final int memberId)
        throws PersistenceException
    {
        final MemberNameRecord name = load(MemberNameRecord.class,
                                     new Where(MemberRecord.MEMBER_ID_C, memberId));
        return (name == null) ? null : name.toMemberName();
    }

    /**
     * Extracts the set of member id from the supplied collection of records using the supplied
     * <code>getId</code> function and loads up the associated names.
     */
    public <C> IntMap<MemberName> loadMemberNames (
        final Iterable<C> records, final Function<C,Integer> getId)
        throws PersistenceException
    {
        final Set<Integer> memberIds = new ArrayIntSet();
        for (final C record : records) {
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
    public IntMap<MemberName> loadMemberNames (final Set<Integer> memberIds)
        throws PersistenceException
    {
        final IntMap<MemberName> names = IntMaps.newHashIntMap();
        if (memberIds.size() > 0) {
            for (final MemberNameRecord name : findAll(
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
    public MemberName loadMemberName (final String username)
        throws PersistenceException
    {
        final MemberNameRecord record = load(MemberNameRecord.class,
                new Where(MemberRecord.ACCOUNT_NAME_C, username));
        return (record == null ? null : record.toMemberName());
    }

    /**
     * Loads up the card for the specified member. Returns null if no member exists with that id.
     */
    public MemberCard loadMemberCard (final int memberId)
        throws PersistenceException
    {
        final MemberCardRecord mcr = load(
            MemberCardRecord.class, new FromOverride(MemberRecord.class),
            new Join(MemberRecord.MEMBER_ID_C, ProfileRecord.MEMBER_ID_C),
            new Where(MemberRecord.MEMBER_ID_C, memberId));
        return (mcr == null) ? null : mcr.toMemberCard();
    }

    /**
     * Loads up member's names and profile photos by id.
     */
    public List<MemberCardRecord> loadMemberCards (final Set<Integer> memberIds)
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
    public List<Integer> findMembersByDisplayName (
        String search, final boolean exact, final int limit)
        throws PersistenceException
    {
        search = search.toLowerCase();

        SQLOperator op;
        if (exact) {
            op = new Equals(new FunctionExp("LOWER", MemberRecord.NAME_C), search);
        } else {
            op = new FullTextMatch(MemberRecord.class, MemberRecord.FTS_NAME, search);
        }
        final List<Integer> ids = Lists.newArrayList();

        // TODO: turn this into a findAllKeys query
//         for (Key<MemberRecord> key :
//                  findAllKeys(MemberRecord.class, where, new Limit(0, limit))) {
//             ids.add((Integer)key.condition.getValues().get(0));
//         }
        for (final MemberRecord mrec :
                 findAll(MemberRecord.class, new Where(op), new Limit(0, limit))) {
            ids.add(mrec.memberId);
        }

        return ids;
    }

    /**
     * Returns the members with the highest levels
     */
    public List<Integer> getLeadingMembers (final int limit)
        throws PersistenceException
    {
        final List<Integer> ids = Lists.newArrayList();
        final List<MemberRecord> mrecs = findAll(
            MemberRecord.class, OrderBy.descending(MemberRecord.LEVEL_C), new Limit(0, limit));
        for (final MemberRecord mrec : mrecs) {
            ids.add(mrec.memberId);
        }

        return ids;
    }

    /**
     * Loads up the member associated with the supplied session token. Returns null if the session
     * has expired or is not valid.
     */
    public MemberRecord loadMemberForSession (final String sessionToken)
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
    public String startOrJoinSession (final int memberId, final int expireDays)
        throws PersistenceException
    {
        // create a new session record for this member
        SessionRecord nsess = new SessionRecord();
        final Calendar cal = Calendar.getInstance();
        final long now = cal.getTimeInMillis();
        cal.add(Calendar.DATE, expireDays);
        nsess.expires = new Date(cal.getTimeInMillis());
        nsess.memberId = memberId;
        nsess.token = StringUtil.md5hex("" + memberId + now + Math.random());

        try {
            insert(nsess);
        } catch (final DuplicateKeyException dke) {
            // if that fails with a duplicate key, reuse the old record but adjust its expiration
            final SessionRecord esess = load(
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
    public MemberRecord refreshSession (final String token, final int expireDays)
        throws PersistenceException
    {
        final SessionRecord sess = load(SessionRecord.class, token);
        if (sess == null) {
            return null;
        }

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, expireDays);
        sess.expires = new Date(cal.getTimeInMillis());
        update(sess);
        return loadMember(sess.memberId);
    }

    /**
     * Clears out a session to member id mapping. This should be called when a user logs off.
     */
    public void clearSession (final String sessionToken)
        throws PersistenceException
    {
        delete(SessionRecord.class, sessionToken);
    }

    /**
     * Clears out a session to member id mapping.
     */
    public void clearSession (final int memberId)
        throws PersistenceException
    {
        final SessionRecord record =
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
    public void insertMember (final MemberRecord member)
        throws PersistenceException
    {
        // flatten account name (email address) on insertion
        member.accountName = member.accountName.toLowerCase();
        if (member.created == null) {
            final long now = System.currentTimeMillis();
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
    public void configureAccountName (final int memberId, final String accountName)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId,
                      MemberRecord.ACCOUNT_NAME, accountName.toLowerCase());
    }

    /**
     * Configures a member's display name.
     */
    public void configureDisplayName (final int memberId, final String name)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.NAME, name);
    }

    /**
     * Configures a member's permanent name.
     */
    public void configurePermaName (final int memberId, final String permaName)
        throws PersistenceException
    {
        // permaName will be a non-null lower-case string
        updatePartial(MemberRecord.class, memberId, MemberRecord.PERMA_NAME, permaName);
    }

    /**
     * Writes the supplied member's flags to the database.
     */
    public void storeFlags (final MemberRecord mrec)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, mrec.memberId, MemberRecord.FLAGS, mrec.flags);
    }

    /**
     * Writes the supplied member's experiences to the database.
     */
    public void storeExperiences (final MemberRecord mrec)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, mrec.memberId, MemberRecord.EXPERIENCES, mrec.experiences);
    }

    /**
     * Configures a member's avatar.
     */
    public void configureAvatarId (final int memberId, final int avatarId)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.AVATAR_ID, avatarId);
    }

    /**
     * Deletes the specified member from the repository.
     */
    public void deleteMember (final MemberRecord member)
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
    public void setHomeSceneId (final int memberId, final int homeSceneId)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.HOME_SCENE_ID, homeSceneId);
    }

    /**
     * Mimics the disabling of deleted members by renaming them to an invalid value that we do in
     * our member management system. This is triggered by us receiving a member action indicating
     * that the member was deleted.
     */
    public void disableMember (final String accountName, final String disabledName)
        throws PersistenceException
    {
        // TODO: Cache Invalidation
        final int mods = updatePartial(
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
    public void noteSessionEnded (final int memberId, final int minutes,
                                  final int humanityReassessFreq)
        throws PersistenceException
    {
        final long now = System.currentTimeMillis();
        final MemberRecord record = loadMember(memberId);
        final Timestamp nowStamp = new Timestamp(now);

        // reassess their humanity if the time has come
        final int secsSinceLast = (int)((now - record.lastHumanityAssessment.getTime())/1000);
        if (humanityReassessFreq > 0 && humanityReassessFreq < secsSinceLast) {
            record.humanity = _flowRepo.assessHumanity(memberId, record.humanity, secsSinceLast);
            record.lastHumanityAssessment = nowStamp;
        }

// TEMP: disabled
//         // expire flow without updating MemberObject, since we're dropping session anyway
//         _flowRepo.expireFlow(record, minutes);
// END TEMP

        record.sessions++;
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
        final SQLOperator joinCondition =
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
        final Comparable<?>[] idArr = IntListUtil.box(memberIds);
        return findAll(
            NeighborFriendRecord.class,
            new FromOverride(MemberRecord.class),
            new Where(new In(MemberRecord.MEMBER_ID_C, idArr)));
    }

    /**
     * Grants the specified number of invites to the given member.
     */
    public void grantInvites (final int memberId, final int number)
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
    public int[] grantInvites (final int number, final Timestamp lastSession)
        throws PersistenceException
    {
        List<MemberRecord> activeUsers;
        if (lastSession != null) {
            activeUsers = findAll(MemberRecord.class, new Where(
                new GreaterThanEquals(MemberRecord.LAST_SESSION_C, lastSession)));
        } else {
            activeUsers = findAll(MemberRecord.class);
        }

        final IntSet ids = new ArrayIntSet();
        for (final MemberRecord memRec : activeUsers) {
            grantInvites(memRec.memberId, number);
            ids.add(memRec.memberId);
        }
        return ids.toIntArray();
    }

    /**
     * Get the number of invites this member has available to send out.
     */
    public int getInvitesGranted (final int memberId)
        throws PersistenceException
    {
        final InviterRecord inviter = load(InviterRecord.class, memberId);
        return inviter != null ? inviter.invitesGranted : 0;
    }

    /**
     * get the total number of invites that this user has sent
     */
    public int getInvitesSent (final int memberId)
        throws PersistenceException
    {
        final InviterRecord inviter = load(InviterRecord.class, memberId);
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
    public void addInvite (final String inviteeEmail, final int inviterId, final String inviteId)
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
    public InvitationRecord inviteAvailable (final String inviteId)
        throws PersistenceException
    {
        final InvitationRecord rec = load(InvitationRecord.class,
                                    new Where(InvitationRecord.INVITE_ID_C, inviteId));
        return (rec == null || rec.inviteeId != 0) ? null : rec;
    }

    /**
     * Update the invitation indicated with the new memberId.
     */
    public void linkInvite (final String inviteId, final MemberRecord member)
        throws PersistenceException
    {
        final InvitationRecord invRec = load(InvitationRecord.class, inviteId);
        invRec.inviteeId = member.memberId;
        update(invRec, InvitationRecord.INVITEE_ID);
    }

    /**
     * Get a list of the invites that this user has already sent out that have not yet been
     * accepted.
     */
    public List<InvitationRecord> loadPendingInvites (final int memberId)
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
    public InvitationRecord loadInvite (final String inviteId, final boolean markViewed)
        throws PersistenceException
    {
        final InvitationRecord invRec = load(
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
    public InvitationRecord loadInvite (final String inviteeEmail, final int inviterId)
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
    public void deleteInvite (final String inviteId)
        throws PersistenceException
    {
        delete(InvitationRecord.class, inviteId);
    }

    /**
     * Add an email address to the opt-out list.
     */
    public void addOptOutEmail (final String email)
        throws PersistenceException
    {
        insert(new OptOutRecord(email.toLowerCase()));
    }

    /**
     * Returns true if the given email address is on the opt-out list
     */
    public boolean hasOptedOut (final String email)
        throws PersistenceException
    {
        return load(OptOutRecord.class, email.toLowerCase()) != null;
    }

    /**
     * Adds the invitee's email address to the opt-out list, and sets this invitation's inviteeId
     * to -1, indicating that it is no longer available, and the invitee chose to opt-out.
     */
    public void optOutInvite (final String inviteId)
        throws PersistenceException
    {
        final InvitationRecord invRec = loadInvite(inviteId, false);
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
    public ReferralRecord loadReferral (final int memberId)
        throws PersistenceException
    {
        return load(ReferralRecord.class, memberId);
    }

    /**
     * Adds or updates the referral record for a member with the given id.
     */
    public ReferralRecord setReferral (final int memberId, final ReferralInfo ref)
        throws PersistenceException
    {
        final ReferralRecord newrec = ReferralRecord.fromInfo(memberId, ref);
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
    public void setUserLevel (final int memberId, final int level)
        throws PersistenceException
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.LEVEL, level);
    }

    /**
     * Loads the names of the members invited by the specified member.
     */
    public List<MemberName> loadMembersInvitedBy (final int memberId)
        throws PersistenceException
    {
        final Join join = new Join(MemberRecord.MEMBER_ID_C,
                             InviterRecord.MEMBER_ID_C).setType(Join.Type.LEFT_OUTER);
        final Where where = new Where(MemberRecord.INVITING_FRIEND_ID_C, memberId);
        final List<MemberName> names = Lists.newArrayList();
        for (final MemberNameRecord name : findAll(MemberNameRecord.class, join, where)) {
            names.add(name.toMemberName());
        }
        return names;
    }

    public List<MemberInviteStatusRecord> getMembersInvitedBy (final int memberId)
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
    public boolean getFriendStatus (final int firstId, final int secondId)
        throws PersistenceException
    {
        final List<FriendRecord> friends = findAll(
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
    public IntSet loadFriendIds (final int memberId)
        throws PersistenceException
    {
        final IntSet memIds = new ArrayIntSet();
        final SQLExpression condition =
            new Or(new Equals(FriendRecord.INVITER_ID_C, memberId),
                   new Equals(FriendRecord.INVITEE_ID_C, memberId));
        for (final FriendRecord record : findAll(FriendRecord.class, new Where(condition))) {
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
    public List<FriendEntry> loadFriends (final int memberId, final int limit)
        throws PersistenceException
    {
        final List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(FriendRecord.class));
        final SQLExpression condition = new And(
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
        final List<MemberCardRecord> records = findAll(MemberCardRecord.class, clauses);
        final List<FriendEntry> list = Lists.newArrayList();
        for (final MemberCardRecord record : records) {
            final MemberCard card = record.toMemberCard();
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
    public MemberCard noteFriendship (final int memberId,  final int otherId)
        throws PersistenceException
    {
        // first load the member record of the potential friend
        final MemberCard other = loadMemberCard(otherId);
        if (other == null) {
            log.warning("Failed to establish friends: member no longer exists " +
                        "[missingId=" + otherId + ", reqId=" + memberId + "].");
            return null;
        }

        // see if there is already a connection, either way
        final List<FriendRecord> existing = Lists.newArrayList();
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

        final FriendRecord rec = new FriendRecord();
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
        final CacheInvalidator invalidator = new CacheInvalidator() {
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
    public int lookupExternalAccount (final int partnerId, final String externalId)
        throws PersistenceException
    {
        final ExternalMapRecord record =
            load(ExternalMapRecord.class, ExternalMapRecord.getKey(partnerId, externalId));
        return (record == null) ? 0 : record.memberId;
    }

    /**
     * Notes that the specified Whirled account is associated with the specified external account.
     */
    public void mapExternalAccount (final int partnerId, final String externalId, final int memberId)
        throws PersistenceException
    {
        final ExternalMapRecord record = new ExternalMapRecord();
        record.partnerId = partnerId;
        record.externalId = externalId;
        record.memberId = memberId;
        insert(record);
    }

    /**
     * Creates a temp ban record for a member, or updates a pre-existing temp ban record.
     */
    public void tempBanMember (final int memberId, final Timestamp expires, final String warning)
        throws PersistenceException
    {
        final MemberWarningRecord record = new MemberWarningRecord();
        record.memberId = memberId;
        record.banExpires = expires;
        record.warning = warning;
        store(record);
    }

    /**
     * Updates the warning message for a member, or creates a new warning message if none exists.
     */
    public void updateMemberWarning (final int memberId, final String warning)
        throws PersistenceException
    {
        if (updatePartial(MemberWarningRecord.class, memberId,
                          MemberWarningRecord.WARNING, warning) == 0) {
            final MemberWarningRecord record = new MemberWarningRecord();
            record.memberId = memberId;
            record.warning = warning;
            insert(record);
        }
    }

    /**
     * Clears a warning message from a member (this includes any temp ban information).
     */
    public void clearMemberWarning (final int memberId)
        throws PersistenceException
    {
        delete(MemberWarningRecord.class, memberId);
    }

    /**
     * Returns the MemberWarningRecord for the memberId, or null if none found.
     */
    public MemberWarningRecord loadMemberWarningRecord (final int memberId)
        throws PersistenceException
    {
        return load(MemberWarningRecord.class, memberId);
    }

    /**
     * Runs the supplied migration on all members in the repository (note: don't use this function
     * unless you understand the implications of doing something for potentially millions of
     * members). If any migration fails, your operation will be aborted but the partially applied
     * migrations will not be rolled back. So be sure that your migration is written idempotently
     * so that it can be partially applied and then run again and won't break. Note also that we do
     * not do any distributed locking, so it is up to the caller to be safely running in a entity
     * migration that will properly guard against simultaneous execution on multiple servers.
     *
     * @return the number of member records that were migrated (which will be all of them if the
     * migration executes without failure, but we return the number anyway so that you can be
     * excited about how large it is).
     */
    public int runMemberMigration (final MemberMigration migration)
        throws PersistenceException
    {
        // TODO: break this up into chunks when our member base is larger
        int migrated = 0;
        for (final MemberRecord mrec : findAll(MemberRecord.class)) {
            migration.apply(mrec);
            migrated++;
        }
        return migrated;
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
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
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

    @Inject protected UserActionRepository _flowRepo;

    protected static final int INVITE_ID_LENGTH = 10;
    protected static final String INVITE_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
}
