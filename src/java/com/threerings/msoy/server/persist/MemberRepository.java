//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.Timestamp;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CacheKey;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;

import com.samskivert.depot.PersistenceContext.CacheListener;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;

import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;

import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;

import com.samskivert.depot.operator.Arithmetic;

import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals.FullText;
import com.samskivert.depot.operator.Conditionals.GreaterThan;
import com.samskivert.depot.operator.Conditionals.GreaterThanEquals;
import com.samskivert.depot.operator.Conditionals.In;
import com.samskivert.depot.operator.Conditionals.LessThan;
import com.samskivert.depot.operator.Conditionals.LessThanEquals;
import com.samskivert.depot.operator.Conditionals.Like;
import com.samskivert.depot.operator.Conditionals.NotEquals;

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

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VizMemberName;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.server.persist.MemberRecord.Flag;
import com.threerings.msoy.spam.server.persist.SpamRepository;

import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.MemberCard;

import com.threerings.presents.annotation.BlockingThread;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
@Singleton @BlockingThread
public class MemberRepository extends DepotRepository
{
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

    @Entity @Computed
    public static class MemberSearchRecord extends PersistentRecord
    {
        public int memberId;

        public double rank;
    }

    @Inject public MemberRepository (PersistenceContext ctx)
    {
        super(ctx);

        // add a cache invalidator that listens to MemberRecord updates
        _ctx.addCacheListener(MemberRecord.class, new CacheListener<MemberRecord>() {
            public void entryInvalidated (CacheKey key, MemberRecord member) {
                _ctx.cacheInvalidate(MemberNameRecord.getKey(member.memberId));
            }
            public void entryCached (CacheKey key, MemberRecord newEntry, MemberRecord oldEntry) {
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

        // drop this superfluous index
        ctx.registerMigration(MemberExperienceRecord.class,
            new SchemaMigration.DropIndex(2, "ixDateOccurred"));
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with the specified id. The
     * record will be fetched from the cache if possible and cached if not.
     */
    public MemberRecord loadMember (int memberId)
    {
        return load(MemberRecord.class, memberId);
    }

    /**
     * Loads up the member record associated with the specified account.  Returns null if no
     * matching record could be found.
     */
    public MemberRecord loadMember (String accountName)
    {
        return load(MemberRecord.class,
                    new Where(MemberRecord.ACCOUNT_NAME, accountName.toLowerCase()));
    }

    /**
     * Loads the member record with the specified permaname. Returns null if no matching record
     * could be found.
     */
    public MemberRecord loadMemberByPermaname (String permaName)
    {
        return load(MemberRecord.class,
                    new Where(MemberRecord.PERMA_NAME, permaName.toLowerCase()));
    }

    /**
     * Records the supplied visitorId to entry vector mapping for later correlation and analysis.
     */
    public void noteEntryVector (String visitorId, String vector)
    {
        EntryVectorRecord record = new EntryVectorRecord();
        record.visitorId = visitorId;
        record.vector = vector;
        record.created = new Timestamp(System.currentTimeMillis());
        insert(record);
    }

    /**
     * Purges entry vector records that have not become associated with members and are older than
     * two weeks.
     */
    public void purgeEntryVectors ()
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - ENTRY_VECTOR_EXPIRE);
        int deleted = deleteAll(
            EntryVectorRecord.class,
            new Where(new And(new Equals(EntryVectorRecord.MEMBER_ID, 0),
                              new LessThan(EntryVectorRecord.CREATED, cutoff))));
        if (deleted > 0) {
            log.info("Purged " + deleted + " expired entry vector records.");
        }
    }

    /**
     * Loads the member records with the supplied set of ids.
     */
    public List<MemberRecord> loadMembers (Collection<Integer> memberIds)
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
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -60); // TODO: unmagick
        Date when = new Date(cal.getTimeInMillis());
        return load(CountRecord.class, new FromOverride(MemberRecord.class),
                    new Where(new GreaterThan(
                        MemberRecord.LAST_SESSION, new ValueExp(when)))).count;
    }

    /**
     * Looks up a member's name by id. Returns null if no member exists with the specified id.
     */
    public MemberName loadMemberName (int memberId)
    {
        MemberNameRecord name = load(MemberNameRecord.class,
                                     new Where(MemberRecord.MEMBER_ID, memberId));
        return (name == null) ? null : name.toMemberName();
    }

    /**
     * Extracts the set of member id from the supplied collection of records using the supplied
     * <code>getId</code> function and loads up the associated names.
     */
    public <C> IntMap<MemberName> loadMemberNames (Iterable<C> records, Function<C,Integer> getId)
    {
        Set<Integer> memberIds = new ArrayIntSet();
        for (C record : records) {
            memberIds.add(getId.apply(record));
        }
        return loadMemberNames(memberIds);
    }

    /**
     * Looks up members' names by id.
     */
    public IntMap<MemberName> loadMemberNames (Set<Integer> memberIds)
    {
        IntMap<MemberName> names = IntMaps.newHashIntMap();
        for (MemberNameRecord name : loadAll(MemberNameRecord.class, memberIds)) {
            names.put(name.memberId, name.toMemberName());
        }
        return names;
    }

    /**
     * Loads the member ids and addresses of all members that have not opted out of announcement
     * emails and are not currently spanked.
     */
    public List<Tuple<Integer, String>> loadMemberEmailsForAnnouncement ()
    {
        SQLExpression annFlags = new Arithmetic.BitAnd(MemberRecord.FLAGS,
            MemberRecord.Flag.NO_ANNOUNCE_EMAIL.getBit() | MemberRecord.Flag.SPANKED.getBit());
        List<Tuple<Integer, String>> emails = Lists.newArrayList();
        Where where = new Where(new Equals(annFlags, 0));
        for (MemberEmailRecord record : findAll(MemberEmailRecord.class, where)) {
            emails.add(Tuple.newTuple(record.memberId, record.accountName));
        }
        return emails;
    }

    /**
     * Looks up a member name and id from by username.
     */
    public MemberName loadMemberName (String accountName)
    {
        MemberNameRecord record = load(
            MemberNameRecord.class,
            new Where(MemberRecord.ACCOUNT_NAME, accountName.toLowerCase()));
        return (record == null ? null : record.toMemberName());
    }

    /**
     * Loads up the card for the specified member. Returns null if no member exists with that id.
     *
     * @param filterDeleted if true, null will be returned instead of a card for members that have
     * been marked as deleted.
     */
    public MemberCard loadMemberCard (int memberId, boolean filterDeleted)
    {
        SQLExpression where = new Equals(MemberRecord.MEMBER_ID, memberId);
        if (filterDeleted) {
            where = new And(where, new NotEquals(MemberRecord.ACCOUNT_NAME,
                                                 memberId + MemberRecord.DELETED_SUFFIX));
        }
        MemberCardRecord mcr = load(
            MemberCardRecord.class, new FromOverride(MemberRecord.class),
            new Join(MemberRecord.MEMBER_ID, ProfileRecord.MEMBER_ID),
            new Where(where));
        return (mcr == null) ? null : mcr.toMemberCard();
    }

    /**
     * Loads up member's names and profile photos by id. Note: deleted members are <em>not</em>
     * filtered from this list.
     *
     * TODO: investigate callers for possibility of paging
     */
    public List<MemberCardRecord> loadMemberCards (Collection<Integer> memberIds)
    {
        return loadMemberCards(memberIds, 0, 0, false);
    }

    /**
     * Loads up collection of members' names and profile photos by id, optionally paged and sorted
     * by last time online. Note: deleted members are <em>not</em> filtered from this list.
     *
     * @param memberIds the ids of the members whose cards to load
     * @param offset the index of the first item to return in the sorted list
     * @param limit the number of cards to load, or 0 to load all
     * @param sortByLastOnline whether to sort the results or just load some cards
     */
    public List<MemberCardRecord> loadMemberCards (
        Collection<Integer> memberIds, int offset, int limit, boolean sortByLastOnline)
    {
        if (memberIds.size() == 0) {
            return Collections.emptyList();
        }

        // set up our query and load the records
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(MemberRecord.class));
        clauses.add(new Join(MemberRecord.MEMBER_ID, ProfileRecord.MEMBER_ID));
        clauses.add(new Where(new In(MemberRecord.MEMBER_ID, memberIds)));
        if (sortByLastOnline) {
            clauses.add(OrderBy.descending(MemberRecord.LAST_SESSION));
        }
        if (limit != 0) {
            clauses.add(new Limit(offset, limit));
        }
        return findAll(MemberCardRecord.class, clauses);
    }

    /**
     * Returns ids for all members whose display name is an exact (in the case-insensitive sense)
     * match of the supplied search string.
     */
    public List<Integer> findMembersByExactDisplayName (String search, int limit)
    {
        return Lists.transform(
            findAllKeys(MemberRecord.class, false,
                new Where(new Equals(new FunctionExp("LOWER", MemberRecord.NAME),
                          new FunctionExp("LOWER", new ValueExp(search)))),
                new Limit(0, limit)),
            RecordFunctions.<MemberRecord>getIntKey());
    }

    /**
     * Returns ids for all members whose display name match the supplied search string in a
     * natural language sense.
     * 
     * TODO: Stop calling this fancy version from GroupServlet.
     */
    public List<MemberSearchRecord> findMembersByDisplayName (String search, int limit)
    {
        FullText fts = new FullText(MemberRecord.class, MemberRecord.FTS_NAME, search);

        return findAll(MemberSearchRecord.class, new FromOverride(MemberRecord.class),
            new FieldDefinition("memberId", MemberRecord.MEMBER_ID),
            new FieldDefinition("rank", fts.rank()),
            new Where(fts.match()),
            OrderBy.descending(fts.rank()),
            new Limit(0, limit));
    }
    /**
     * Returns MemberCardRecords for all members that are part of the given collection and also
     * substring match their displayName against the given search string.
     */
    public List<Integer> findMembersInCollection (String search,
        Collection<Integer> memberIds)
    {
        if (memberIds.size() == 0) {
            return Collections.emptyList();
        }
        search = search.toLowerCase();
        Where whereClause = new Where(new And(
            new In(MemberRecord.MEMBER_ID, memberIds),
            new Like(new FunctionExp("LOWER", MemberRecord.NAME), "%" + search + "%")));
        return Lists.transform(findAllKeys(MemberRecord.class, false, whereClause),
                               RecordFunctions.<MemberRecord>getIntKey());
    }

    /**
     * Loads ids of member records that are initial candidates for a retention email. Members are
     * selected if 1. their last login time is between two timestamps, and 2. if they have not
     * decided to forego announcement emails.
     */
    public List<Integer> findRetentionCandidates (Date earliestLastSession, Date latestLastSession)
    {
        ColumnExp lastSess = MemberRecord.LAST_SESSION;
        Where where = new Where(new And(
            new GreaterThan(lastSess, new ValueExp(earliestLastSession)),
            new LessThanEquals(lastSess, new ValueExp(latestLastSession)),
            new Equals(new Arithmetic.BitAnd(
                MemberRecord.FLAGS, Flag.NO_ANNOUNCE_EMAIL.getBit() | Flag.SPANKED.getBit()), 0)));
        return Lists.transform(findAllKeys(MemberRecord.class, false, where),
                               RecordFunctions.<MemberRecord>getIntKey());
    }

    /**
     * Returns the members with the highest levels
     *
     * TODO: This appears not to be in use, but if that were to change, please remember to add
     * TODO: an index on MemberRecord.LEVEL.
     */
    public List<Integer> getLeadingMembers (int limit)
    {
        List<Integer> ids = Lists.newArrayList();
        List<MemberRecord> mrecs = findAll(
            MemberRecord.class, OrderBy.descending(MemberRecord.LEVEL), new Limit(0, limit));
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
     * as of now (using the supplied <code>expireDays</code> setting).
     */
    public String startOrJoinSession (int memberId, int expireDays)
    {
        // calculate a new expiration time
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.add(Calendar.DATE, expireDays);
        Date expires = new Date(cal.getTimeInMillis());

        // update an existing session
        SessionRecord session = updateExistingSession(memberId, expires);

        if (session == null) {
            // if that didn't work create a new one
            session = new SessionRecord();
            session.memberId = memberId;
            session.token = StringUtil.md5hex("" + memberId + now + Math.random());
            session.expires = expires;

            // record it
            try {
                insert(session);

            } catch (DuplicateKeyException dke) {
                // quick on the draw?
                log.warning("Duplicate session key on insert immediately after load",
                    "memberId", memberId);
                session = updateExistingSession(memberId, expires);
            }
        }

        return session.token;
    }

    /**
     * Refreshes a session using the supplied authentication token.
     *
     * @return the member associated with the session if it is valid and was refreshed, null if the
     * session has expired.
     */
    public MemberRecord refreshSession (String token, int expireDays)
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
    {
        delete(SessionRecord.class, sessionToken);
    }

    /**
     * Clears out a session to member id mapping.
     */
    public void clearSession (int memberId)
    {
        SessionRecord record =
            load(SessionRecord.class, new Where(SessionRecord.MEMBER_ID, memberId));
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
    {
        Preconditions.checkNotNull(member.visitorId, "MemberRecord.visitorId must be non-null");

        // account name must always be lower case
        member.accountName = member.accountName.toLowerCase();
        if (member.created == null) {
            long now = System.currentTimeMillis();
            member.created = new Date(now);
            member.lastSession = new Timestamp(now);
            member.lastHumanityAssessment = new Timestamp(now);
            member.humanity = MsoyCodes.STARTING_HUMANITY;
        }
        insert(member);

        // note that this visitor id/entry vector pair is now associated with a member record
        updatePartial(EntryVectorRecord.class, member.visitorId,
                      EntryVectorRecord.MEMBER_ID, member.memberId);
    }

    /**
     * Returns the entry vector noted for the specified member account or null.
     */
    public String loadEntryVector (int memberId)
    {
        EntryVectorRecord erec = load(
            EntryVectorRecord.class, new Where(EntryVectorRecord.MEMBER_ID, memberId));
        return (erec == null) ? null : erec.vector;
    }

    /**
     * Configures a member's account name (email address).
     */
    public void configureAccountName (int memberId, String accountName)
    {
        accountName = accountName.toLowerCase(); // account name must always be lower case
        updatePartial(MemberRecord.class, memberId,
                      MemberRecord.ACCOUNT_NAME, accountName.toLowerCase());
    }

    /**
     * Updates registration fields, for when a prevsiously inserted permaguest registers.
     */
    public void updateRegistration (int memberId, String accountName, String displayName)
    {
        accountName = accountName.toLowerCase(); // account name must always be lower case
        updatePartial(MemberRecord.class, memberId,
                      MemberRecord.ACCOUNT_NAME, accountName,
                      MemberRecord.NAME, displayName);
    }

    /**
     * Updates the specified member's affiliate member id. This only happens in one very rare
     * circumstance where we want your inviting member's id to trump a preexisting affiliate id.
     */
    public void updateAffiliateMemberId (int memberId, int affiliateMemberId)
    {
        updatePartial(MemberRecord.class, memberId,
                      MemberRecord.AFFILIATE_MEMBER_ID, affiliateMemberId);
    }

    /**
     * Configures a member's display name.
     */
    public void configureDisplayName (int memberId, String name)
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.NAME, name);
    }

    /**
     * Configures a member's permanent name.
     */
    public void configurePermaName (int memberId, String permaName)
    {
        // permaName will be a non-null lower-case string
        updatePartial(MemberRecord.class, memberId, MemberRecord.PERMA_NAME, permaName);
    }

    /**
     * Writes the supplied member's flags to the database.
     */
    public void storeFlags (MemberRecord mrec)
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
     * Deletes the {@link MemberRecord} for the specified ids. This should <em>only</em> be called
     * for permaguest records. Registered members are left "on the books" (just their MemberRecord,
     * everything else is purged).
     */
    public void deleteMembers (Collection<Integer> memberIds)
    {
        deleteAll(MemberRecord.class, new Where(new In(MemberRecord.MEMBER_ID, memberIds)));
    }

    /**
     * Marks all of the supplied records as disabled. This is where registered members go to die.
     */
    public void disableMembers (Collection<Integer> memberIds)
    {
        // we have to do this one at a time
        for (int memberId : memberIds) {
            updatePartial(MemberRecord.class, memberId,
                          MemberRecord.ACCOUNT_NAME, memberId + MemberRecord.DELETED_SUFFIX);
        }
        // NOTE: currently, the OOOUserRecord will have been deleted for all of these member
        // records, if we some day ditch our OOOUser crap and move passwords into MemberRecord,
        // this method should set their password to "" so that this account cannot be logged into
    }

    /**
     * Deletes all data associated with the supplied member (except their MemberRecord). This is
     * done as a part of purging a member's account.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(ExternalMapRecord.class,
                  new Where(new In(ExternalMapRecord.MEMBER_ID, memberIds)));
        deleteAll(SessionRecord.class,
                  new Where(new In(SessionRecord.MEMBER_ID, memberIds)));
        deleteAll(MemberExperienceRecord.class,
                  new Where(new In(MemberExperienceRecord.MEMBER_ID, memberIds)));
        deleteAll(MemberWarningRecord.class,
                  new Where(new In(MemberWarningRecord.MEMBER_ID, memberIds)));
        deleteAll(AffiliateRecord.class,
                  new Where(new In(AffiliateRecord.MEMBER_ID, memberIds)));
        deleteAll(InviterRecord.class,
                  new Where(new In(InviterRecord.MEMBER_ID, memberIds)));
        deleteAll(CharityRecord.class,
                  new Where(new In(CharityRecord.MEMBER_ID, memberIds)));
        deleteAll(EntryVectorRecord.class,
                  new Where(new In(EntryVectorRecord.MEMBER_ID, memberIds)));
        deleteAll(FriendRecord.class,
                  new Where(new Or(new In(FriendRecord.INVITER_ID, memberIds),
                                   new In(FriendRecord.INVITEE_ID, memberIds))));
        // we don't purge InvitationRecord or GameInvitationRecord; they will probably be few in
        // number and are arguably interesting for historical reasons; this may need to be
        // revisited if we achieve "internet scale"
    }

    /**
     * Set the home scene id for the specified memberId.
     */
    public void setHomeSceneId (int memberId, int homeSceneId)
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.HOME_SCENE_ID, homeSceneId);
    }

    /**
     * Mimics the disabling of deleted members by renaming them to an invalid value that we do in
     * our member management system. This is triggered by us receiving a member action indicating
     * that the member was deleted.
     */
    public void disableMember (String accountName, String disabledName)
    {
        // TODO: Cache Invalidation
        int mods = updatePartial(
            MemberRecord.class, new Where(MemberRecord.ACCOUNT_NAME, accountName.toLowerCase()),
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
    {
        long now = System.currentTimeMillis();
        MemberRecord record = loadMember(memberId);
        Timestamp nowStamp = new Timestamp(now);

        // reassess their humanity if the time has come
        int secsSinceLast = (int)((now - record.lastHumanityAssessment.getTime())/1000);
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
    public List<NeighborFriendRecord> getNeighborhoodFriends (int memberId)
    {
        SQLOperator joinCondition =
            new Or(new And(new Equals(FriendRecord.INVITER_ID, memberId),
                           new Equals(FriendRecord.INVITEE_ID, MemberRecord.MEMBER_ID)),
                   new And(new Equals(FriendRecord.INVITEE_ID, memberId),
                           new Equals(FriendRecord.INVITER_ID, MemberRecord.MEMBER_ID)));
        return findAll(
            NeighborFriendRecord.class,
            new FromOverride(MemberRecord.class),
            OrderBy.descending(MemberRecord.LAST_SESSION),
            new Join(FriendRecord.class, joinCondition));
    }

    /**
     * Returns the NeighborFriendRecords for all the given members.
     */
    public List<NeighborFriendRecord> getNeighborhoodMembers (int[] memberIds)
    {
        if (memberIds.length == 0) {
            return Collections.emptyList();
        }
        Comparable<?>[] idArr = IntListUtil.box(memberIds);
        return findAll(
            NeighborFriendRecord.class,
            new FromOverride(MemberRecord.class),
            new Where(new In(MemberRecord.MEMBER_ID, idArr)));
    }

    /**
     * Grants the specified number of invites to the given member.
     */
    public void grantInvites (int memberId, int number)
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
    {
        List<MemberRecord> activeUsers;
        if (lastSession != null) {
            activeUsers = findAll(MemberRecord.class, new Where(
                new GreaterThanEquals(MemberRecord.LAST_SESSION, lastSession)));
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
    {
        InviterRecord inviter = load(InviterRecord.class, memberId);
        return inviter != null ? inviter.invitesGranted : 0;
    }

    /**
     * get the total number of invites that this user has sent
     */
    public int getInvitesSent (int memberId)
    {
        InviterRecord inviter = load(InviterRecord.class, memberId);
        return inviter != null ? inviter.invitesSent : 0;
    }

    public String generateInviteId ()
    {
        return _inviteIdGenerator.generate();
    }

    /**
     * Add a new invitation. Also decrements the available invitation count for the inviterId and
     * increments the number of invites sent, iff the inviterId is non-zero.
     */
    public void addInvite (String inviteeEmail, int inviterId, String inviteId)
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
    {
        InvitationRecord rec = load(
            InvitationRecord.class, new Where(InvitationRecord.INVITE_ID, inviteId));
        return (rec == null || rec.inviteeId != 0) ? null : rec;
    }

    /**
     * Update the invitation indicated with the new memberId.
     */
    public void linkInvite (String inviteId, MemberRecord member)
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
    {
        return findAll(
            InvitationRecord.class,
            new Where(InvitationRecord.INVITER_ID, memberId,
                      InvitationRecord.INVITEE_ID, 0));
    }

    /**
     * Return the InvitationRecord that corresponds to the given unique code.
     */
    public InvitationRecord loadInvite (String inviteId, boolean markViewed)
    {
        InvitationRecord invRec = load(InvitationRecord.class, inviteId);
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
    {
        // TODO: This does a row scan on email after using ixInviter. Should be OK, but let's check.
        return load(InvitationRecord.class, new Where(
            InvitationRecord.INVITEE_EMAIL, inviteeEmail,
            InvitationRecord.INVITER_ID, inviterId));
    }

    /**
     * Gets a new unique random id for use with game invitations.
     */
    public String generateGameInviteId ()
    {
        return _gameInviteIdGenerator.generate();
    }

    /**
     * Adds a new game invite sent to the given user with the given id.
     */
    public void addGameInvite (String inviteeEmail, String inviteId)
    {
        GameInvitationRecord invite = new GameInvitationRecord();
        invite.inviteeEmail = inviteeEmail;
        invite.inviteId = inviteId;
        insert(invite);
    }

    /**
     * Returns the game invitation record that corresponds to the given invitee.
     */
    public GameInvitationRecord loadGameInviteByEmail (String inviteeEmail)
    {
        return load(GameInvitationRecord.class, inviteeEmail);
    }

    /**
     * Returns the game invitation record that corresponds to the given id.
     */
    public GameInvitationRecord loadGameInviteById (String inviteId)
    {
        return load(GameInvitationRecord.class,
            new Where(GameInvitationRecord.INVITE_ID, inviteId));
    }

    /**
     * Delete the InvitationRecord that corresponds to the given unique code.
     */
    public void deleteInvite (String inviteId)
    {
        delete(InvitationRecord.class, inviteId);
    }

    /**
     * Adds the invitee's email address to the opt-out list, and sets this invitation's inviteeId
     * to -1, indicating that it is no longer available, and the invitee chose to opt-out.
     */
    public void optOutInvite (String inviteId)
    {
        InvitationRecord invRec = loadInvite(inviteId, false);
        if (invRec != null) {
            invRec.inviteeId = -1;
            update(invRec, InvitationRecord.INVITEE_ID);
            _spamRepo.addOptOutEmail(invRec.inviteeEmail);
        }
    }

    /**
     * Sets the reported level for the given member
     */
    public void setUserLevel (int memberId, int level)
    {
        updatePartial(MemberRecord.class, memberId, MemberRecord.LEVEL, level);
    }

    /**
     * Loads the names of the members invited by the specified member.
     */
    public List<MemberName> loadMembersInvitedBy (int memberId)
    {
        // TODO: this needs fixing: your affiliate is not necessarily your inviter
        // (I mean, if you have an inviter, they will be your affiliate, and you can't have
        // both, but we can't just look at an affiliate id and know that that's your inviter)
        Join join = new Join(MemberRecord.MEMBER_ID,
                             InviterRecord.MEMBER_ID).setType(Join.Type.LEFT_OUTER);
        Where where = new Where(MemberRecord.AFFILIATE_MEMBER_ID, memberId);
        List<MemberName> names = Lists.newArrayList();
        for (MemberNameRecord name : findAll(MemberNameRecord.class, join, where)) {
            names.add(name.toMemberName());
        }
        return names;
    }

    /**
     * Returns info on all members invited by the specified member. This method will only return
     * 500 results, which avoids mayhem when asking for all members invited by memberId 0.
     */
    public List<MemberInviteStatusRecord> getMembersInvitedBy (int memberId)
    {
        // TODO: this needs fixing: your affiliate is not necessarily your inviter
        // (I mean, if you have an inviter, they will be your affiliate, and you can't have
        // both, but we can't just look at an affiliate id and know that that's your inviter)
        return findAll(MemberInviteStatusRecord.class,
                       new Join(MemberRecord.MEMBER_ID,
                                InviterRecord.MEMBER_ID).setType(Join.Type.LEFT_OUTER),
                       new Where(MemberRecord.AFFILIATE_MEMBER_ID, memberId),
                       new Limit(0, 500));
    }

    /**
     * Load the memberIds that have been permanently muted by the specified member.
     */
    public int[] loadMutelist (int memberId)
    {
        List<MuteRecord> list =
            findAll(MuteRecord.class, new Where(new Equals(MuteRecord.MUTER_ID, memberId)));
        int[] ids = new int[list.size()];
        for (int ii = 0; ii < ids.length; ii++) {
            ids[ii] = list.get(ii).muteeId;
        }
        return ids;
    }

    /**
     * Tests to see whether the muter has muted the mutee.
     */
    public boolean isMuted (int muterId, int muteeId)
    {
        return (null != load(MuteRecord.class, MuteRecord.getKey(muterId, muteeId)));
    }

    /**
     * Update the specified mute relationship.
     */
    public void setMuted (int muterId, int muteeId, boolean muted)
    {
        MuteRecord record = new MuteRecord();
        record.muterId = muterId;
        record.muteeId = muteeId;
        if (muted) {
            store(record);
        } else {
            delete(record);
        }
    }

    /**
     * Determine what the friendship status is between one member and another.
     */
    public boolean getFriendStatus (int firstId, int secondId)
    {
        // TODO: migrate existing friend records to (firstId<secondId) format then just do one query
        return ((load(FriendRecord.class, FriendRecord.getKey(firstId, secondId)) != null) ||
                (load(FriendRecord.class, FriendRecord.getKey(secondId, firstId)) != null));
    }

    /**
     * Loads the member ids of the specified member's friends.
     */
    public IntSet loadFriendIds (int memberId)
    {
        IntSet memIds = new ArrayIntSet();
        List<Where> clauses = Collections.singletonList(
            new Where(new Or(new Equals(FriendRecord.INVITER_ID, memberId),
                             new Equals(FriendRecord.INVITEE_ID, memberId))));
        for (FriendRecord record : findAll(FriendRecord.class, clauses)) {
            memIds.add(record.getFriendId(memberId));
        }
        return memIds;
    }

    /**
     * Loads the ids of all members who are flagged as "greeters". Most recently online greeters
     * are first in the array.
     * TODO: write com.samskivert.util.IntList and use that instead of List<Integer>
     */
    public List<Integer> loadGreeterIds ()
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(GREETER_FLAG_IS_SET));
        clauses.add(OrderBy.descending(MemberRecord.LAST_SESSION));
        return Lists.transform(findAllKeys(MemberRecord.class, false, clauses),
                               RecordFunctions.<MemberRecord>getIntKey());
    }

    /**
     * Loads the FriendEntry record for all friends of the specified member. The online status of
     * each friend will be false. The friends will be returned in order of most recently online to
     * least.
     */
    public List<FriendEntry> loadAllFriends (int memberId)
    {
        return loadFriends(memberId, 0);
    }

    /**
     * Loads the FriendEntry record for some or all of the most recently online friends of the
     * specified member. The online status of each friend will be false. The friends will be
     * returned in order of most recently online to least.
     *
     * @param limit the number of friends to load or 0 for all of them.
     */
    public List<FriendEntry> loadFriends (int memberId, int limit)
    {
        // load up the ids of this member's friends (ordered from most recently online to least)
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(new Or(new Equals(FriendRecord.INVITER_ID, memberId),
                                     new Equals(FriendRecord.INVITEE_ID, memberId))));
        SQLExpression condition = new And(
            new Or(new And(new Equals(FriendRecord.INVITER_ID, memberId),
                           new Equals(MemberRecord.MEMBER_ID, FriendRecord.INVITEE_ID)),
                   new And(new Equals(FriendRecord.INVITEE_ID, memberId),
                           new Equals(MemberRecord.MEMBER_ID, FriendRecord.INVITER_ID))));
        clauses.add(new Join(MemberRecord.class, condition));
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }
        clauses.add(OrderBy.descending(MemberRecord.LAST_SESSION));

        // prepare an ordered map of the friends in question
        Map<Integer, FriendEntry> friends = Maps.newLinkedHashMap();
        for (FriendRecord record : findAll(FriendRecord.class, clauses)) {
            friends.put(record.getFriendId(memberId), null);
        }

        // now load up member card records for these guys and convert them to friend entries
        for (MemberCardRecord crec : loadMemberCards(friends.keySet())) {
            MemberCard card = crec.toMemberCard();
            friends.put(crec.memberId, new FriendEntry(
                            new VizMemberName(card.name, card.photo), card.headline, false));
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
    public MemberCard noteFriendship (int memberId,  int otherId)
    {
        // first load the member record of the potential friend
        MemberCard other = loadMemberCard(otherId, true);
        if (other == null) {
            log.warning("Failed to establish friends: member no longer exists " +
                        "[missingId=" + otherId + ", reqId=" + memberId + "].");
            return null;
        }

        // if they already have a connection, let the caller know by excepting (TODO: this can go
        // away when we migrate all records to lowestId/highestId)
        if (getFriendStatus(memberId, otherId)) {
            throw new DuplicateKeyException(memberId + " and " + otherId + " are already friends");
        }

        // always store friend ids with the lower of the two ids first
        FriendRecord rec = new FriendRecord();
        rec.inviterId = Math.min(memberId, otherId);
        rec.inviteeId = Math.max(memberId, otherId);
        insert(rec);

        return other;
    }

    /**
     * Remove a friend mapping from the database.
     */
    public void clearFriendship (int memberId, int otherId)
    {
        // TODO: migrate existing friend records to (firstId<secondId) format then can do one delete
        delete(FriendRecord.class, FriendRecord.getKey(memberId, otherId));
        delete(FriendRecord.class, FriendRecord.getKey(otherId, memberId));
    }

    /**
     * Returns the id of the account associated with the supplied external account (the caller is
     * responsible for confirming the authenticity of the external id information) or 0 if no
     * account is associated with that external account.
     */
    public int lookupExternalAccount (ExternalAuther auther, String externalId)
    {
        ExternalMapRecord record = load(
            ExternalMapRecord.class, ExternalMapRecord.getKey(auther.toByte(), externalId));
        return (record == null) ? 0 : record.memberId;
    }

    /**
     * Returns the Whirled member ids of any of the supplied external ids that have been mapped to
     * Whirled accounts.
     */
    public List<Integer> lookupExternalAccounts (ExternalAuther auther, List<String> externalIds)
    {
        List<Integer> memberIds = Lists.newArrayList();
        if (externalIds.isEmpty()) {
            return memberIds;
        }
        Where where = new Where(new And(new Equals(ExternalMapRecord.PARTNER_ID, auther.toByte()),
                                        new In(ExternalMapRecord.EXTERNAL_ID, externalIds)));
        for (ExternalMapRecord record : findAll(ExternalMapRecord.class, where)) {
            memberIds.add(record.memberId);
        }
        return memberIds;
    }

    /**
     * Notes that the specified Whirled account is associated with the specified external account.
     */
    public void mapExternalAccount (ExternalAuther auther, String externalId, int memberId)
    {
        ExternalMapRecord record = new ExternalMapRecord();
        record.partnerId = auther.toByte();
        record.externalId = externalId;
        record.memberId = memberId;
        store(record);
    }

    /**
     * Returns a list of all mappings for the specified account to external authentication sources.
     */
    public Map<ExternalAuther, String> loadExternalMappings (int memberId)
    {
        Map<ExternalAuther, String> authers = Maps.newHashMap();
        for (ExternalMapRecord emr : findAll(ExternalMapRecord.class,
                                             new Where(ExternalMapRecord.MEMBER_ID, memberId))) {
            authers.put(ExternalAuther.fromByte((byte)emr.partnerId), emr.externalId);
        }
        return authers;
    }

    /**
     * Creates a temp ban record for a member, or updates a pre-existing temp ban record.
     */
    public void tempBanMember (int memberId, Timestamp expires, String warning)
    {
        MemberWarningRecord record = new MemberWarningRecord();
        record.memberId = memberId;
        record.banExpires = expires;
        record.warning = warning;
        store(record);
        updateBanned(memberId, true);
    }

    /**
     * Updates the warning message for a member, or creates a new warning message if none exists.
     */
    public void updateMemberWarning (int memberId, String warning)
    {
        if (updatePartial(MemberWarningRecord.class, memberId,
                          MemberWarningRecord.WARNING, warning) == 0) {
            MemberWarningRecord record = new MemberWarningRecord();
            record.memberId = memberId;
            record.warning = warning;
            insert(record);
            updateBanned(memberId, true);
        }
    }

    /**
     * Clears a warning message from a member (this includes any temp ban information).
     */
    public void clearMemberWarning (int memberId)
    {
        delete(MemberWarningRecord.class, memberId);
        updateBanned(memberId, false);
    }

    /**
     * Returns the MemberWarningRecord for the memberId, or null if none found.
     */
    public MemberWarningRecord loadMemberWarningRecord (int memberId)
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
    public int runMemberMigration (MemberMigration migration)
        throws Exception
    {
        // TODO: break this up into chunks when our member base is larger
        int migrated = 0;
        for (MemberRecord mrec : findAll(MemberRecord.class)) {
            migration.apply(mrec);
            migrated++;
        }
        return migrated;
    }

    public void deleteExperiences (int memberId)
    {
        deleteAll(MemberExperienceRecord.class,
            new Where(MemberExperienceRecord.MEMBER_ID, memberId));
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
            new Where(MemberExperienceRecord.MEMBER_ID, memberId),
            OrderBy.ascending(MemberExperienceRecord.DATE_OCCURRED));
    }

    /**
     * Saves the given charity record.
     */
    public void saveCharity (CharityRecord record)
    {
        store(record);
    }

    /**
     * Deletes the charity record for the given member.
     */
    public void deleteCharity (int memberId)
    {
        delete(CharityRecord.class, memberId);
    }

    /**
     * Retrieves all charities.
     */
    public List<CharityRecord> getCharities ()
    {
        return findAll(CharityRecord.class);
    }

    /**
     * Retrieves all core charities, i.e., those that can be randomly selected when a user has not
     * yet chosen a specific charity.
     */
    public List<CharityRecord> getCoreCharities ()
    {
        return findAll(CharityRecord.class, new Where(CharityRecord.CORE, true));
    }

    /**
     * Retrieves the charity record for the given member.  If the member is not a charity, returns
     * null.
     */
    public CharityRecord getCharityRecord (int memberId)
    {
        return load(CharityRecord.class, memberId);
    }

    /**
     * Changes the member's charity to the specified member ID.
     */
    public void updateSelectedCharity (int memberId, int charityMemberId)
    {
        Preconditions.checkArgument(
            charityMemberId >= 0, "Charity member ID must be zero or positive.");
        updatePartial(MemberRecord.class, memberId,
                      MemberRecord.CHARITY_MEMBER_ID, charityMemberId);
    }

    /**
     * Returns the member ids of all permaguest accounts that have not logged in in the past 10
     * days and have not achieved at least level 5.
     */
    public List<Integer> loadExpiredWeakPermaguestIds ()
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - WEAK_PERMAGUEST_EXPIRE);
        And bits = new And(new Like(MemberRecord.ACCOUNT_NAME, PERMA_PATTERN),
                           new LessThan(MemberRecord.LEVEL, STRONG_PERMAGUEST_LEVEL),
                           new LessThan(MemberRecord.LAST_SESSION, cutoff));
        return Lists.transform(findAllKeys(MemberRecord.class, false, new Where(bits),
                                           new Limit(0, MAX_WEAK_ACCOUNTS)),
                               RecordFunctions.<MemberRecord>getIntKey());
    }

    /**
     * Update the expiration time of an existing session. Returns the loaded session or
     * null if none previously existed.
     */
    protected SessionRecord updateExistingSession (int memberId, Date expires)
    {
        SessionRecord session = load(
            SessionRecord.class, new Where(SessionRecord.MEMBER_ID, memberId));

        if (session != null) {
            session.expires = expires;
            update(session, SessionRecord.EXPIRES);
        }

        return session;
    }

    /**
     * Updates the BANNED flag for a given member. Used by the member warning methods in this
     * class.
     */
    protected void updateBanned (int memberId, boolean value)
    {
        MemberRecord mrec = loadMember(memberId);
        if (mrec.updateFlag(MemberRecord.Flag.SPANKED, value)) {
            storeFlags(mrec);
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberRecord.class);
        classes.add(FriendRecord.class);
        classes.add(SessionRecord.class);
        classes.add(InvitationRecord.class);
        classes.add(InviterRecord.class);
        classes.add(ExternalMapRecord.class);
        classes.add(MemberWarningRecord.class);
        classes.add(AffiliateRecord.class);
        classes.add(MemberExperienceRecord.class);
        classes.add(CharityRecord.class);
        classes.add(EntryVectorRecord.class);
        classes.add(GameInvitationRecord.class);
        classes.add(MuteRecord.class);
    }

    /**
     * Abstract algorithm for generating new unique invite ids. Delegates the testing for existence
     * to implementation.
     */
    protected static abstract class InviteIDGenerator
    {
        /**
         * Creates a new invite id generator.
         */
        public InviteIDGenerator (String name)
        {
            _name = name;
        }

        /**
         * Generates a new, unique invite id.
         */
        public String generate ()
        {
            // find a free invite id
            String inviteId;
            int tries = 0;
            while (exists(inviteId = randomId())) {
                tries++;
            }
            if (tries > 5) {
                log.warning(_name + " inviteId space is getting saturated", "tries", tries);
            }
            return inviteId;
        }

        /**
         * Tests if a given invite id already exists.
         */
        abstract protected boolean exists (String id);

        /**
         * Generates an arbitrary non-unique id.
         */
        protected static String randomId ()
        {
            String rand = "";
            for (int ii = 0; ii < INVITE_ID_LENGTH; ii++) {
                rand += INVITE_ID_CHARACTERS.charAt((int)(Math.random() *
                    INVITE_ID_CHARACTERS.length()));
            }
            return rand;
        }

        /** Name to be used in saturation warnings. */
        protected String _name;
    }

    /** Generator for normal invitations. */
    protected InviteIDGenerator _inviteIdGenerator = new InviteIDGenerator ("InvitationRecord") {
        protected boolean exists (String id) {
            return loadInvite(id, false) != null;
        }
    };

    /** Generator for normal game invitations. */
    protected InviteIDGenerator _gameInviteIdGenerator =
        new InviteIDGenerator ("GameInvitationRecord") {
            protected boolean exists (String id) {
                return loadGameInviteById(id) != null;
            }
        };

    @Inject protected SpamRepository _spamRepo;
    @Inject protected UserActionRepository _actionRepo;

    protected static final int INVITE_ID_LENGTH = 10;
    protected static final String INVITE_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
    protected static final NotEquals GREETER_FLAG_IS_SET = new NotEquals(new Arithmetic.BitAnd(
        MemberRecord.FLAGS, MemberRecord.Flag.GREETER.getBit()), 0);

    /** Period after which we expire entry vector records that are not associated with members. */
    protected static final long ENTRY_VECTOR_EXPIRE = 14 * 24*60*60*1000L;

    /** A "like" pattern that matches permaguest accounts. */
    protected static final String PERMA_PATTERN = MemberMailUtil.makePermaguestEmail("%");

    /** Period after which we expire "weak" permaguest accounts (those of low level). */
    protected static final long WEAK_PERMAGUEST_EXPIRE = 10 * 24*60*60*1000L;

    /** A permaguest that fails to achieve this level is considered weak, and purged. */
    protected static final int STRONG_PERMAGUEST_LEVEL = 5;

    /** We'll only return 1000 weak accounts at a time for purging. */
    protected static final int MAX_WEAK_ACCOUNTS = 1000;
}
