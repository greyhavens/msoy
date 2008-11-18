//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CacheInvalidator;
import com.samskivert.depot.CacheKey;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.PersistenceContext.CacheListener;
import com.samskivert.depot.PersistenceContext.CacheTraverser;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.SimpleCacheKey;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.Arithmetic;
import com.samskivert.depot.operator.Arithmetic.BitAnd;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals.NotEquals;
import com.samskivert.depot.operator.Conditionals.FullTextMatch;
import com.samskivert.depot.operator.Conditionals.GreaterThan;
import com.samskivert.depot.operator.Conditionals.GreaterThanEquals;
import com.samskivert.depot.operator.Conditionals.In;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic.And;
import com.samskivert.depot.operator.Logic.Or;
import com.samskivert.depot.operator.SQLOperator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.web.gwt.MemberCard;

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
        public void apply (MemberRecord record) throws Exception;
    };

    @Entity @Computed(shadowOf=MemberRecord.class)
    public static class MemberEmailRecord extends PersistentRecord
    {
        public int memberId;
        public String accountName;
    }

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

        ctx.registerMigration(MemberRecord.class, new SchemaMigration.Drop(23, "flow"));
        ctx.registerMigration(MemberRecord.class, new SchemaMigration.Drop(23, "accFlow"));
        ctx.registerMigration(MemberRecord.class,
            new SchemaMigration.Rename(23, "invitingFriendId", MemberRecord.AFFILIATE_MEMBER_ID));
        ctx.registerMigration(MemberRecord.class, new SchemaMigration.Drop(25, "blingAffiliate"));

        // Convert existing tracking numbers from the ReferralRecord, or creates new ones for those
        // members who didn't get their tracking numbers yet.
        // TODO: remove ReferralRecord after this migration happened in production.
        registerMigration(new DataMigration("2008_09_25_referral_to_tracking_id") {
            @Override public void invoke () throws DatabaseException {
                int converted = 0, created = 0;

                // first, copy all referral trackers over to member records
                List<ReferralRecord> todos = findAll(ReferralRecord.class);
                for (ReferralRecord referral : todos) {
                    final String visitorId = VisitorInfo.normalizeVisitorId(referral.tracker);
                    updatePartial(MemberRecord.class, referral.memberId,
                        MemberRecord.VISITOR_ID, visitorId);
                    if (++converted % 100 == 0) {
                        log.info("ReferralRecord conversion: " + converted + " processed...");
                    }
                }

                // now fill in all missing trackers
                List<MemberRecord> missing = findAll(
                    MemberRecord.class, new Where(
                        new Conditionals.IsNull(MemberRecord.VISITOR_ID_C)));

                for (MemberRecord member : missing) {
                    // default visitorId will be based on registration time
                    String visitorId = VisitorInfo.timestampToVisitorId(member.created);
                    updatePartial(MemberRecord.class, member.memberId,
                        MemberRecord.VISITOR_ID, visitorId);
                    if (++created % 100 == 0) {
                        log.info("VisitorID creation: " + converted + " processed...");
                    }
                }

                log.info(String.format("Converted %d old ReferralRecords, created %d new ones",
                                       converted, created));
            }
        });

        // TEMP: blow away bogus AffiliateRecords, repopulate
        registerMigration(new DataMigration("2008_09_26_populateAffiliateRecords") {
            public void invoke () throws DatabaseException
            {
                // first blow away the old broken records
                deleteAll(AffiliateRecord.class, new Where(new LiteralExp("true")));

                // re-populate the AffiliateRecords using the affiliateMemberId field of
                // everyone's MemberRecord
                List<MemberRecord> memRecs = findAll(MemberRecord.class,
                    new Where(new Conditionals.NotEquals(MemberRecord.AFFILIATE_MEMBER_ID_C, 0)));
                for (MemberRecord rec : memRecs) {
                    setAffiliate(rec.memberId, String.valueOf(rec.affiliateMemberId));
                }
            }
        });
    }

    /**
     * Loads up the member record associated with the specified account.  Returns null if no
     * matching record could be found.
     */
    public MemberRecord loadMember (final String accountName)
    {
        return load(MemberRecord.class,
                    new Where(MemberRecord.ACCOUNT_NAME_C, accountName.toLowerCase()));
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with the specified id. The
     * record will be fetched from the cache if possible and cached if not.
     */
    public MemberRecord loadMember (final int memberId)
    {
        return load(MemberRecord.class, memberId);
    }

    /**
     * Loads the member records with the supplied set of ids.
     */
    public List<MemberRecord> loadMembers (final Set<Integer> memberIds)
    {
        return loadAll(MemberRecord.class, memberIds);
    }

    /**
     * Returns the total number of members in Whirled.
     */
    public int getPopulationCount ()
    {
        return load(CountRecord.class, new FromOverride(MemberRecord.class)).count;
    }

    /**
     * Calculate a count of the active member population, currently defined as anybody
     * whose last session is within the past 60 days.
     */
    public int getActivePopulationCount ()
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
    {
        final Set<Integer> memberIds = new ArrayIntSet();
        for (final C record : records) {
            memberIds.add(getId.apply(record));
        }
        return loadMemberNames(memberIds);
    }

    /**
     * Looks up members' names by id.
     */
    public IntMap<MemberName> loadMemberNames (final Set<Integer> memberIds)
    {
        final IntMap<MemberName> names = IntMaps.newHashIntMap();
        for (MemberNameRecord name : loadAll(MemberNameRecord.class, memberIds)) {
            names.put(name.memberId, name.toMemberName());
        }
        return names;
    }

    /**
     * Loads the member ids and addresses of all members that have not opted out of announcement
     * emails.
     */
    public List<Tuple<Integer, String>> loadMemberEmailsForAnnouncement ()
    {
        SQLExpression annFlag = new Arithmetic.BitAnd(
            MemberRecord.FLAGS_C, MemberRecord.Flag.NO_ANNOUNCE_EMAIL.getBit());
        List<Tuple<Integer, String>> emails = Lists.newArrayList();
        Where where = new Where(new Equals(annFlag, 0));
        for (MemberEmailRecord record : findAll(MemberEmailRecord.class, where)) {
            emails.add(Tuple.newTuple(record.memberId, record.accountName));
        }
        return emails;
    }

    /**
     * Looks up a member name and id from by username.
     */
    public MemberName loadMemberName (final String username)
    {
        final MemberNameRecord record = load(MemberNameRecord.class,
                new Where(MemberRecord.ACCOUNT_NAME_C, username));
        return (record == null ? null : record.toMemberName());
    }

    /**
     * Loads up the card for the specified member. Returns null if no member exists with that id.
     */
    public MemberCard loadMemberCard (final int memberId)
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
    {
        search = search.toLowerCase();
        SQLOperator op = exact ?
            new Equals(new FunctionExp("LOWER", MemberRecord.NAME_C), search) :
            new FullTextMatch(MemberRecord.class, MemberRecord.FTS_NAME, search);
        return Lists.transform(
            findAllKeys(MemberRecord.class, false, new Where(op), new Limit(0, limit)),
            RecordFunctions.<MemberRecord>getIntKey());
    }

    /**
     * Returns the members with the highest levels
     */
    public List<Integer> getLeadingMembers (final int limit)
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
    {
        delete(SessionRecord.class, sessionToken);
    }

    /**
     * Clears out a session to member id mapping.
     */
    public void clearSession (final int memberId)
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
    {
        updatePartial(MemberRecord.class, memberId,
                      MemberRecord.ACCOUNT_NAME, accountName.toLowerCase());
    }

    /**
     * Configures a member's display name.
     */
    public void configureDisplayName (final int memberId, final String name)
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.NAME, name);
    }

    /**
     * Configures a member's permanent name.
     */
    public void configurePermaName (final int memberId, final String permaName)
    {
        // permaName will be a non-null lower-case string
        updatePartial(MemberRecord.class, memberId, MemberRecord.PERMA_NAME, permaName);
    }

    /**
     * Writes the supplied member's flags to the database.
     */
    public void storeFlags (final MemberRecord mrec)
    {
        updatePartial(MemberRecord.class, mrec.memberId, MemberRecord.FLAGS, mrec.flags);
    }

    /**
     * Writes the supplied member's experiences to the database.
     */
    public void storeExperiences (MemberRecord mrec)
    {
        updatePartial(MemberRecord.class, mrec.memberId,
            MemberRecord.EXPERIENCES, mrec.experiences);
    }

    /**
     * Configures a member's avatar.
     */
    public void configureAvatarId (int memberId, int avatarId)
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.AVATAR_ID, avatarId);
    }

    /**
     * Updates a member's badgesVersion.
     */
    public void updateBadgesVersion (int memberId, short badgesVersion)
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.BADGES_VERSION, badgesVersion);
    }

    /**
     * Deletes the specified member from the repository.
     */
    public void deleteMember (final MemberRecord member)
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
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.HOME_SCENE_ID, homeSceneId);
    }

    /**
     * Mimics the disabling of deleted members by renaming them to an invalid value that we do in
     * our member management system. This is triggered by us receiving a member action indicating
     * that the member was deleted.
     */
    public void disableMember (final String accountName, final String disabledName)
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
    {
        final long now = System.currentTimeMillis();
        final MemberRecord record = loadMember(memberId);
        final Timestamp nowStamp = new Timestamp(now);

        // reassess their humanity if the time has come
        final int secsSinceLast = (int)((now - record.lastHumanityAssessment.getTime())/1000);
        if (humanityReassessFreq > 0 && humanityReassessFreq < secsSinceLast) {
            record.humanity = _actionRepo.assessHumanity(memberId, record.humanity, secsSinceLast);
            record.lastHumanityAssessment = nowStamp;
        }

// TEMP: disabled
//         // expire flow without updating MemberObject, since we're dropping session anyway
//         _actionRepo.expireFlow(record, minutes);
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
    {
        final InviterRecord inviter = load(InviterRecord.class, memberId);
        return inviter != null ? inviter.invitesGranted : 0;
    }

    /**
     * get the total number of invites that this user has sent
     */
    public int getInvitesSent (final int memberId)
    {
        final InviterRecord inviter = load(InviterRecord.class, memberId);
        return inviter != null ? inviter.invitesSent : 0;
    }

    public String generateInviteId ()
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
    {
        final InvitationRecord rec = load(InvitationRecord.class,
                                    new Where(InvitationRecord.INVITE_ID_C, inviteId));
        return (rec == null || rec.inviteeId != 0) ? null : rec;
    }

    /**
     * Update the invitation indicated with the new memberId.
     */
    public void linkInvite (final String inviteId, final MemberRecord member)
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
    {
        delete(InvitationRecord.class, inviteId);
    }

    /**
     * Add an email address to the opt-out list.
     */
    public void addOptOutEmail (final String email)
    {
        insert(new OptOutRecord(email.toLowerCase()));
    }

    /**
     * Returns true if the given email address is on the opt-out list
     */
    public boolean hasOptedOut (final String email)
    {
        return load(OptOutRecord.class, email.toLowerCase()) != null;
    }

    /**
     * Adds the invitee's email address to the opt-out list, and sets this invitation's inviteeId
     * to -1, indicating that it is no longer available, and the invitee chose to opt-out.
     */
    public void optOutInvite (final String inviteId)
    {
        final InvitationRecord invRec = loadInvite(inviteId, false);
        if (invRec != null) {
            invRec.inviteeId = -1;
            update(invRec, InvitationRecord.INVITEE_ID);
            addOptOutEmail(invRec.inviteeEmail);
        }
    }

    /**
     * Set the affiliate for a newly registered user.
     */
    public void setAffiliate (int memberId, String affiliate)
    {
        AffiliateRecord affRec = new AffiliateRecord();
        affRec.memberId = memberId;
        affRec.affiliate = affiliate;
        insert(affRec);
    }

    /**
     * For every ReferralRecord with a matching affiliate, update the associated MemberRecords
     * to have the specified affiliateMemberId.
     */
    public void updateAffiliateMemberId (String affiliate, int affiliateMemberId)
    {
        // Note: apparently we do not want to do this with a join, as one day the MemberRecord
        // may live on a different database than the AffiliateRecord.

        // get all the memberIds that have the specified affiliate in their AffiliateRecord
        List<Key<AffiliateRecord>> affKeys = findAllKeys(AffiliateRecord.class, false,
            new Where(AffiliateRecord.AFFILIATE_C, affiliate));

        // then transform the AffiliateRecord keys to MemberRecord keys
        // (we make a new ArrayList so that we only transform each key once, instead
        // of just creating a "view" list and re-transforming on every iteration,
        // since this collection is used in the KeySet for both querying and cache invalidating.)
        List<Key<MemberRecord>> memKeys = Lists.newArrayList(Iterables.transform(affKeys,
            new Function<Key<AffiliateRecord>, Key<MemberRecord>>() {
                public Key<MemberRecord> apply (Key<AffiliateRecord> key) {
                    return MemberRecord.getKey((Integer) key.getValues().get(0));
                }
            }));

        // create a KeySet representing these keys
        KeySet<MemberRecord> keySet = new KeySet<MemberRecord>(MemberRecord.class, memKeys);

        // then update all those members to have the new affiliateMemberId
        updatePartial(MemberRecord.class, keySet, keySet,
            MemberRecord.AFFILIATE_MEMBER_ID, affiliateMemberId);
    }

    /**
     * Sets the reported level for the given member
     */
    public void setUserLevel (final int memberId, final int level)
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.LEVEL, level);
    }

    /**
     * Loads the names of the members invited by the specified member.
     */
    public List<MemberName> loadMembersInvitedBy (final int memberId)
    {
        // TODO: this needs fixing: your affiliate is not necessarily your inviter
        // (I mean, if you have an inviter, they will be your affiliate, and you can't have
        // both, but we can't just look at an affiliate id and know that that's your inviter)
        final Join join = new Join(MemberRecord.MEMBER_ID_C,
                             InviterRecord.MEMBER_ID_C).setType(Join.Type.LEFT_OUTER);
        final Where where = new Where(MemberRecord.AFFILIATE_MEMBER_ID_C, memberId);
        final List<MemberName> names = Lists.newArrayList();
        for (final MemberNameRecord name : findAll(MemberNameRecord.class, join, where)) {
            names.add(name.toMemberName());
        }
        return names;
    }

    /**
     * Returns info on all members invited by the specified member. This method will only return
     * 500 results, which avoids mayhem when asking for all members invited by memberId 0.
     */
    public List<MemberInviteStatusRecord> getMembersInvitedBy (final int memberId)
    {
        // TODO: this needs fixing: your affiliate is not necessarily your inviter
        // (I mean, if you have an inviter, they will be your affiliate, and you can't have
        // both, but we can't just look at an affiliate id and know that that's your inviter)
        return findAll(MemberInviteStatusRecord.class,
                       new Join(MemberRecord.MEMBER_ID_C,
                                InviterRecord.MEMBER_ID_C).setType(Join.Type.LEFT_OUTER),
                       new Where(MemberRecord.AFFILIATE_MEMBER_ID_C, memberId),
                       new Limit(0, 500));
    }

    /**
     * Determine what the friendship status is between one member and another.
     */
    public boolean getFriendStatus (final int firstId, final int secondId)
    {
        List<Where> clauses = Collections.singletonList(
            new Where(new Or(new And(new Equals(FriendRecord.INVITER_ID_C, firstId),
                                     new Equals(FriendRecord.INVITEE_ID_C, secondId)),
                             new And(new Equals(FriendRecord.INVITER_ID_C, secondId),
                                     new Equals(FriendRecord.INVITEE_ID_C, firstId)))));
        return findAll(FriendRecord.class, true, clauses).size() > 0;
    }

    /**
     * Loads the member ids of the specified member's friends.
     */
    public IntSet loadFriendIds (int memberId)
    {
        IntSet memIds = new ArrayIntSet();
        List<Where> clauses = Collections.singletonList(
            new Where(new Or(new Equals(FriendRecord.INVITER_ID_C, memberId),
                             new Equals(FriendRecord.INVITEE_ID_C, memberId))));
        for (FriendRecord record : findAll(FriendRecord.class, true, clauses)) {
            memIds.add(record.getFriendId(memberId));
        }
        return memIds;
    }

    /**
     * Loads the ids of all members who are flagged as "greeters".
     */
    public IntSet loadGreeterIds ()
    {
        ArrayIntSet greeters = new ArrayIntSet();
        for (Key<MemberRecord> key : findAllKeys(MemberRecord.class, false,
            Collections.singletonList(new Where(GREETER_FLAG_IS_SET)))) {
            greeters.add((Integer)key.getValues().get(0));
        }
        return greeters;
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
    {
        // load up the ids of this member's friends (ordered from most recently online to least)
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(new Or(new Equals(FriendRecord.INVITER_ID_C, memberId),
                                     new Equals(FriendRecord.INVITEE_ID_C, memberId))));
        SQLExpression condition = new And(
            new Or(new And(new Equals(FriendRecord.INVITER_ID_C, memberId),
                           new Equals(MemberRecord.MEMBER_ID_C, FriendRecord.INVITEE_ID_C)),
                   new And(new Equals(FriendRecord.INVITEE_ID_C, memberId),
                           new Equals(MemberRecord.MEMBER_ID_C, FriendRecord.INVITER_ID_C))));
        clauses.add(new Join(MemberRecord.class, condition));
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }
        clauses.add(OrderBy.descending(MemberRecord.LAST_SESSION_C));

        // prepare an ordered map of the friends in question
        Map<Integer, FriendEntry> friends = Maps.newLinkedHashMap();
        for (FriendRecord record : findAll(FriendRecord.class, true, clauses)) {
            friends.put(record.getFriendId(memberId), null);
        }

        // now load up member card records for these guys and convert them to friend entries
        for (MemberCardRecord crec : loadMemberCards(friends.keySet())) {
            MemberCard card = crec.toMemberCard();
            friends.put(crec.memberId, new FriendEntry(card.name, false, card.photo, card.headline));
        }

        // we might have nulls if there are some legacy bastards with no profile record
        return Lists.newArrayList(
            Iterables.filter(friends.values(), Predicates.not(Predicates.isNull())));
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
    {
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, memberId));
        _ctx.cacheInvalidate(new SimpleCacheKey(FRIENDS_CACHE_ID, otherId));
        delete(FriendRecord.class, FriendRecord.getKey(memberId, otherId));
        delete(FriendRecord.class, FriendRecord.getKey(otherId, memberId));
    }

    /**
     * Delete all the friend relations involving the specified memberId, usually because that
     * member is being deleted.
     */
    public void deleteAllFriends (final int memberId)
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
    {
        final ExternalMapRecord record =
            load(ExternalMapRecord.class, ExternalMapRecord.getKey(partnerId, externalId));
        return (record == null) ? 0 : record.memberId;
    }

    /**
     * Notes that the specified Whirled account is associated with the specified external account.
     */
    public void mapExternalAccount (final int partnerId, final String externalId, final int memberId)
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
    {
        delete(MemberWarningRecord.class, memberId);
    }

    /**
     * Returns the MemberWarningRecord for the memberId, or null if none found.
     */
    public MemberWarningRecord loadMemberWarningRecord (final int memberId)
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
        throws Exception
    {
        // TODO: break this up into chunks when our member base is larger
        int migrated = 0;
        for (final MemberRecord mrec : findAll(MemberRecord.class)) {
            migration.apply(mrec);
            migrated++;
        }
        return migrated;
    }
    
    public void deleteExperiences (int memberId)
    {
        deleteAll(MemberExperienceRecord.class, 
            new Where(MemberExperienceRecord.MEMBER_ID_C, memberId));
    }
    
    public void saveExperiences (List<MemberExperienceRecord> experiences)
    {
        // Might be nice to batch these.  Maybe depot does that automatically, not sure.
        for (MemberExperienceRecord experience : experiences) {
            store(experience);
        }
    }
    
    public List<MemberExperienceRecord> getExperiences (int memberId)
    {
        return findAll(MemberExperienceRecord.class, 
            new Where(MemberExperienceRecord.MEMBER_ID_C, memberId),
            OrderBy.ascending(MemberExperienceRecord.DATE_OCCURRED_C));
    }

    /**
     * Tests if the supplied member id is flagged as a greeter.
     */
    public boolean isGreeter (int memberId)
    {
        return findAllKeys(MemberRecord.class, false, Collections.singletonList(new Where(new And(
            new Equals(MemberRecord.MEMBER_ID_C, memberId), GREETER_FLAG_IS_SET)))).size() > 0;
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
        classes.add(AffiliateRecord.class);
        classes.add(ReferralRecord.class);
        classes.add(MemberExperienceRecord.class);
    }

    @Inject protected UserActionRepository _actionRepo;

    protected static final int INVITE_ID_LENGTH = 10;
    protected static final String INVITE_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
    protected static final NotEquals GREETER_FLAG_IS_SET = new NotEquals(new BitAnd(
        MemberRecord.FLAGS_C, MemberRecord.Flag.GREETER.getBit()), 0);
}
