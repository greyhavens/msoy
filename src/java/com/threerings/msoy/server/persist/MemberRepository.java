//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.DatabaseLiaison;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DateFuncs;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext.CacheListener;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.FullText;

import com.threerings.util.StreamableArrayIntSet;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.server.persist.MemberRecord.Flag;
import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebUserService;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
@Singleton @BlockingThread
public class MemberRepository extends DepotRepository
{
    public static final int FUNNEL_TOTAL_DAYS = 60;
    public static final int FUNNEL_RETURNED_DAYS = 1;
    public static final int FUNNEL_RETAINED_DAYS = 7;

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

        _ctx.registerMigration(ExternalMapRecord.class,
            new SchemaMigration.Rename(4, "partnerId", ExternalMapRecord.AUTHER));
        _ctx.registerMigration(ExternalMapRecord.class,
            new SchemaMigration.Retype(4, ExternalMapRecord.AUTHER));

        // we need an explicit column add here so we get the default value without having it stuck
        // in the meta data
        _ctx.registerMigration(ExternalMapRecord.class,
            new SchemaMigration.Add(5, ExternalMapRecord.SITE_ID, "0"));

        // NOTE: Depot does not update the primary key when a new @Id column is added, so manually
        // force it to regenerate the pkey index by dropping it first. This may be psql specific.
        // see http://code.google.com/p/depot/issues/detail?id=8
        _ctx.registerMigration(ExternalMapRecord.class,
            new SchemaMigration(5) {
                @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException {
                    liaison.dropPrimaryKey(conn, "ExternalMapRecord", "ExternalMapRecord_pkey");
                    return 1;
                }
            });

        _ctx.registerMigration(MemberRecord.class,
            new SchemaMigration.Drop(31, "humanity"));
        _ctx.registerMigration(MemberRecord.class,
            new SchemaMigration.Drop(31, "lastHumanityAssessment"));

        registerMigration(new DataMigration("2009-09 ExternalMapRecord siteId") {
            @Override public void invoke ()
                throws DatabaseException {
                // change all site id fields away from the old default to the new
                final int OLD_DEFAULT = 0;
                final int NEW_DEFAULT_APP_ID = 1;
                final int NEW_DEFAULT = ExternalSiteId.facebookApp(NEW_DEFAULT_APP_ID).siteId;
                updatePartial(ExternalMapRecord.class, new Where(
                    ExternalMapRecord.SITE_ID.eq(OLD_DEFAULT)),
                    null, ExternalMapRecord.SITE_ID, NEW_DEFAULT);
            }
        });

        // add a cache invalidator that listens to MemberRecord updates
        _ctx.addCacheListener(MemberRecord.class, new CacheListener<MemberRecord>() {
            public void entryInvalidated (MemberRecord member) {
                _ctx.cacheInvalidate(MemberNameRecord.getKey(member.memberId));
            }
            public void entryCached (MemberRecord newEntry, MemberRecord oldEntry) {
            }
            @Override public String toString () {
                return "MemberRecord -> MemberNameRecord";
            }
        });
    }

    /**
     * Loads up a member record by id. Returns null if no member exists with the specified id. The
     * record will be fetched from the cache if possible and cached if not.
     */
    public MemberRecord loadMember (int memberId)
    {
        return load(MemberRecord.getKey(memberId));
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
     * Test to see if an {@link EntryVectorRecord} association already exists for the given
     * visitor.
     */
    public EntryVectorRecord entryVectorExists (String visitorId)
    {
        return load(EntryVectorRecord.getKey(visitorId));
    }

    /**
     * Records the supplied visitorId to entry vector mapping for later correlation and analysis.
     */
    public void noteEntryVector (String visitorId, String vector, int memberId)
    {
        EntryVectorRecord record = new EntryVectorRecord();
        record.visitorId = visitorId;
        record.vector = vector;
        record.created = new Timestamp(System.currentTimeMillis());
        record.memberId = memberId;
        insert(record);
    }

    /**
     * Updates an entry vector for the given visitorId. Currently it's only possible to upgrade
     * from a 'page.default' vector.
     */
    public boolean updateEntryVector (String visitorId, String vector)
    {
        EntryVectorRecord record = load(EntryVectorRecord.getKey(visitorId));
        if (record != null) {
            if (record.vector.equals("page.default")) {
                record.vector = vector;
                update(record);
                return true;
            }
            log.warning("Attempted to update non-trivial entry vector", "visitorId", visitorId,
                "old", record.vector, "new", vector);
            return false;
        }
        log.warning("Attempted to update non-existent vector", "visitorId", visitorId, "new", vector);
        return false;
    }

    /**
     * Purges entry vector records that have not become associated with members and are older than
     * two months.
     */
    public void purgeEntryVectors ()
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - ENTRY_VECTOR_EXPIRE);
        int deleted = deleteAll(
            EntryVectorRecord.class,
            new Where(Ops.and(EntryVectorRecord.MEMBER_ID.eq(0),
                              EntryVectorRecord.CREATED.lessThan(cutoff))));
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
        Timestamp cutoff = RepositoryUtil.getCutoff(60); // TODO: unmagick
        return load(CountRecord.class, new FromOverride(MemberRecord.class),
                    new Where(MemberRecord.LAST_SESSION.greaterThan(cutoff))).count;
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
    public <C> Map<Integer, MemberName> loadMemberNames (Iterable<C> records, Function<C,Integer> getId)
    {
        Set<Integer> memberIds = Sets.newHashSet();
        for (C record : records) {
            memberIds.add(getId.apply(record));
        }
        return loadMemberNames(memberIds);
    }

    /**
     * Looks up members' names by id.
     */
    public Map<Integer, MemberName> loadMemberNames (Set<Integer> memberIds)
    {
        Map<Integer, MemberName> names = Maps.newHashMap();
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
        // no_announce and spanked must be off and validated must be on, thus we match:
        // (no_announce|spanked|validated) & flags == validated
        int valbit = MemberRecord.Flag.VALIDATED.getBit();
        int bits = (MemberRecord.Flag.NO_ANNOUNCE_EMAIL.getBit() |
                    MemberRecord.Flag.SPANKED.getBit() | valbit);
        SQLExpression<?> where = MemberRecord.FLAGS.bitAnd(bits).eq(valbit);
        List<Tuple<Integer, String>> emails = Lists.newArrayList();
        for (MemberEmailRecord record : findAll(MemberEmailRecord.class, new Where(where))) {
            // !MemberMailUtil.isPlaceholderAddress(record.accountName) (no longer needed)
            if (!MemberRecord.isDeleted(record.memberId, record.accountName)) {
                emails.add(Tuple.newTuple(record.memberId, record.accountName));
            }
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
        SQLExpression<?> where = MemberRecord.MEMBER_ID.eq(memberId);
        if (filterDeleted) {
            where = Ops.and(where, MemberRecord.ACCOUNT_NAME.notEq(
                                memberId + MemberRecord.DELETED_SUFFIX));
        }
        MemberCardRecord mcr = load(
            MemberCardRecord.class, new FromOverride(MemberRecord.class),
            MemberRecord.MEMBER_ID.join(ProfileRecord.MEMBER_ID),
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
        clauses.add(MemberRecord.MEMBER_ID.join(ProfileRecord.MEMBER_ID));
        clauses.add(new Where(MemberRecord.MEMBER_ID.in(memberIds)));
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
                        new Where(StringFuncs.lower(MemberRecord.NAME).eq(
                                      StringFuncs.lower(Exps.value(search)))),
                        new Limit(0, limit)), Key.<MemberRecord>toInt());
    }

    /**
     * Returns ids for all members whose display name match the supplied search string in a
     * natural language sense.
     *
     * TODO: Stop calling this fancy version from GroupServlet.
     */
    public List<MemberSearchRecord> findMembersByDisplayName (String search, int limit)
    {
        FullText fts = new FullText(MemberRecord.class, MemberRecord.FTS_NAME, search, true);

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
        Where where = new Where(
            Ops.and(MemberRecord.MEMBER_ID.in(memberIds),
                    Ops.like(StringFuncs.lower(MemberRecord.NAME), "%" + search + "%")));
        return Lists.transform(findAllKeys(MemberRecord.class, false, where),
                               Key.<MemberRecord>toInt());
    }

    /**
     * Execute a funnel report query that summarizes entry records by (date, vector group),
     * optionally joined with the given external column and/or the given where condition.
     */
    public List<FunnelByDateRecord> funnelByDate (ColumnExp<?> joinColumn,
                                                  SQLExpression<?> whereBit)
    {
        final FluentExp<?> dateExp = DateFuncs.date(EntryVectorRecord.CREATED);
        List<QueryClause> clauses = Lists.newArrayList(
            new FromOverride(EntryVectorRecord.class),
            new FieldOverride(FunnelByDateRecord.DATE, dateExp),
            new GroupBy(dateExp, EntryVectorRecord.VECTOR),
            OrderBy.ascending(dateExp));

        if (joinColumn != null) {
            clauses.add(new Join(EntryVectorRecord.MEMBER_ID, joinColumn));
        }

        FluentExp<Boolean> condition = DateFuncs.now().dateSub(EntryVectorRecord.CREATED).lessEq(
            Exps.days(FUNNEL_TOTAL_DAYS));
        if (whereBit != null) {
            condition = Ops.and(condition, whereBit);
        }
        clauses.add(new Where(condition));

        return findAll(FunnelByDateRecord.class, clauses);
    }

    /**
     * Execute a funnel report query that summarizes entry records grouped by vector,
     * optionally joined with the given external column and/or the given where condition.
     */
    public List<FunnelByVectorRecord> funnelByVector (ColumnExp<?> joinColumn,
                                                      SQLExpression<?> whereBit)
    {
        List<QueryClause> clauses = Lists.newArrayList(
            new FromOverride(EntryVectorRecord.class),
            new GroupBy(EntryVectorRecord.VECTOR));

        if (joinColumn != null) {
            clauses.add(new Join(EntryVectorRecord.MEMBER_ID, joinColumn));
        }

        FluentExp<Boolean> condition = DateFuncs.now().dateSub(EntryVectorRecord.CREATED)
            .lessEq(Exps.days(FUNNEL_TOTAL_DAYS));
        if (whereBit != null) {
            condition = Ops.and(condition, whereBit);
        }
        clauses.add(new Where(condition));

        return findAll(FunnelByVectorRecord.class, clauses);
    }

    /**
     * Loads ids of member records that are initial candidates for a retention email. Members are
     * selected if 1. their last login time is between two timestamps, and 2. if they have not
     * decided to forego announcement emails.
     */
    public List<Integer> findRetentionCandidates (Date earliestLastSession, Date latestLastSession)
    {
        ColumnExp<Timestamp> lastSess = MemberRecord.LAST_SESSION;
        int noBits = Flag.NO_ANNOUNCE_EMAIL.getBit() | Flag.SPANKED.getBit();
        int yesBits = Flag.VALIDATED.getBit();
        Where where = new Where(Ops.and(lastSess.greaterThan(earliestLastSession),
                                        lastSess.lessEq(latestLastSession),
                                        MemberRecord.FLAGS.bitAnd(noBits).eq(0),
                                        MemberRecord.FLAGS.bitAnd(yesBits).notEq(0)));
        return Lists.transform(findAllKeys(MemberRecord.class, false, where),
                               Key.<MemberRecord>toInt());
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
        SessionRecord session = load(SessionRecord.getKey(sessionToken));
        if (session != null && session.expires.getTime() < System.currentTimeMillis()) {
            session = null;
        }
        return (session == null) ? null : load(MemberRecord.getKey(session.memberId));
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

    public String startOrJoinSession (int memberId)
    {
        return startOrJoinSession(memberId, WebUserService.SESSION_DAYS);
    }

    /**
     * Refreshes a session using the supplied authentication token.
     *
     * @return the member associated with the session if it is valid and was refreshed, null if the
     * session has expired.
     */
    public MemberRecord refreshSession (String token, int expireDays)
    {
        SessionRecord sess = load(SessionRecord.getKey(token));
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
        delete(SessionRecord.getKey(sessionToken));
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
     * Purges session records that have not been refreshed in a month.
     */
    public void purgeSessionRecords ()
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - SESSION_RECORD_EXPIRE);
        int deleted = deleteAll(SessionRecord.class,
            new Where(SessionRecord.EXPIRES.lessThan(cutoff)));
        if (deleted > 0) {
            log.info("Purged " + deleted + " expired session records.");
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
        }
        insert(member);

        // note that this visitor id/entry vector pair is now associated with a member record
        updatePartial(EntryVectorRecord.getKey(member.visitorId),
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
        updatePartial(MemberRecord.getKey(memberId),
                      MemberRecord.ACCOUNT_NAME, accountName.toLowerCase());
    }

    /**
     * Updates the specified member's affiliate member id. This only happens in one very rare
     * circumstance where we want your inviting member's id to trump a preexisting affiliate id.
     */
    public void updateAffiliateMemberId (int memberId, int affiliateMemberId)
    {
        updatePartial(MemberRecord.getKey(memberId),
                      MemberRecord.AFFILIATE_MEMBER_ID, affiliateMemberId);
    }

    /**
     * Configures a member's display name.
     */
    public void configureDisplayName (int memberId, String name)
    {
        updatePartial(MemberRecord.getKey(memberId), MemberRecord.NAME, name);
    }

    /**
     * Configures a member's permanent name.
     */
    public void configurePermaName (int memberId, String permaName)
    {
        // permaName will be a non-null lower-case string
        updatePartial(MemberRecord.getKey(memberId), MemberRecord.PERMA_NAME, permaName);
    }

    /**
     * Writes the supplied member's flags to the database.
     */
    public void storeFlags (MemberRecord mrec)
    {
        updatePartial(MemberRecord.getKey(mrec.memberId), MemberRecord.FLAGS, mrec.flags);
    }

    /**
     * Writes the supplied member's experiences to the database.
     */
    public void storeExperiences (MemberRecord mrec)
    {
        updatePartial(MemberRecord.getKey(mrec.memberId),
                      MemberRecord.EXPERIENCES, mrec.experiences);
    }

    /**
     * Configures a member's avatar.
     */
    public void configureAvatarId (int memberId, int avatarId)
    {
        updatePartial(MemberRecord.getKey(memberId), MemberRecord.AVATAR_ID, avatarId);
    }

    /**
     * Configures a member's current theme.
     */
    public void configureThemeId (int memberId, int themeGroupId)
    {
        updatePartial(MemberRecord.getKey(memberId), MemberRecord.THEME_GROUP_ID, themeGroupId);
    }

    /**
     * Updates a member's badgesVersion.
     */
    public void updateBadgesVersion (int memberId, short badgesVersion)
    {
        updatePartial(MemberRecord.getKey(memberId), MemberRecord.BADGES_VERSION, badgesVersion);
    }

    /**
     * Deletes the {@link MemberRecord} for the specified ids. This should <em>only</em> be called
     * for permaguest records. Registered members are left "on the books" (just their MemberRecord,
     * everything else is purged).
     */
    public void deleteMembers (Collection<Integer> memberIds)
    {
        deleteAll(MemberRecord.class, new Where(MemberRecord.MEMBER_ID.in(memberIds)));
    }

    /**
     * Marks all of the supplied records as disabled. This is where registered members go to die.
     */
    public void disableMembers (Collection<Integer> memberIds)
    {
        // we have to do this one at a time
        for (int memberId : memberIds) {
            updatePartial(MemberRecord.getKey(memberId),
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
                  new Where(ExternalMapRecord.MEMBER_ID.in(memberIds)));
        deleteAll(SessionRecord.class,
                  new Where(SessionRecord.MEMBER_ID.in(memberIds)));
        deleteAll(MemberExperienceRecord.class,
                  new Where(MemberExperienceRecord.MEMBER_ID.in(memberIds)));
        deleteAll(MemberWarningRecord.class,
                  new Where(MemberWarningRecord.MEMBER_ID.in(memberIds)));
        deleteAll(AffiliateRecord.class,
                  new Where(AffiliateRecord.MEMBER_ID.in(memberIds)));
        deleteAll(CharityRecord.class,
                  new Where(CharityRecord.MEMBER_ID.in(memberIds)));
        deleteAll(EntryVectorRecord.class,
                  new Where(EntryVectorRecord.MEMBER_ID.in(memberIds)));
        deleteAll(FriendshipRecord.class,
                  new Where(Ops.or(FriendshipRecord.MEMBER_ID.in(memberIds),
                                   FriendshipRecord.FRIEND_ID.in(memberIds))));
        // we don't purge InvitationRecord or GameInvitationRecord; they will probably be few in
        // number and are arguably interesting for historical reasons; this may need to be
        // revisited if we achieve "internet scale"
    }

    /**
     * Set the home scene id for the specified memberId.
     */
    public void setHomeSceneId (int memberId, int homeSceneId)
    {
        updatePartial(MemberRecord.getKey(memberId), MemberRecord.HOME_SCENE_ID, homeSceneId);
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
     * minutes spent online, and set their last session time to now.
     *
     * @param minutes the duration of the session in minutes.
     */
    public void noteSessionEnded (int memberId, int minutes)
    {
        MemberRecord record = loadMember(memberId);
        if (record == null) {
            log.warning("Non-existent member's session ended?", "id", memberId);
            return;
        }
        record.sessions++;
        record.sessionMinutes += minutes;
        record.lastSession = new Timestamp(System.currentTimeMillis());
        update(record);
    }

    /**
     * Sets the reported level for the given member
     */
    public void setUserLevel (int memberId, int level)
    {
        updatePartial(MemberRecord.getKey(memberId), MemberRecord.LEVEL, level);
    }

    /**
     * Gets the number of members that are affiliated to this member.
     */
    public int countMembersAffiliatedTo (int memberId)
    {
        Where where = new Where(MemberRecord.AFFILIATE_MEMBER_ID, memberId);
        return load(CountRecord.class, where, new FromOverride(MemberRecord.class)).count;
    }

    /**
     * Loads a page of names of the members invited by the specified member.
     */
    public List<MemberName> loadMembersAffiliatedTo (int memberId, int offset, int count)
    {
        Where where = new Where(MemberRecord.AFFILIATE_MEMBER_ID, memberId);
        Limit limit = new Limit(offset, count);
        OrderBy order = OrderBy.ascending(MemberNameRecord.MEMBER_ID);
        List<MemberName> names = Lists.newArrayList();
        for (MemberNameRecord name : findAll(MemberNameRecord.class, where, limit, order)) {
            names.add(name.toMemberName());
        }
        return names;
    }

    /**
     * Load the memberIds that have been permanently muted by the specified member.
     */
    public int[] loadMutelist (int memberId)
    {
        List<MuteRecord> list =
            findAll(MuteRecord.class, new Where(MuteRecord.MUTER_ID.eq(memberId)));
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
        return (null != load(MuteRecord.getKey(muterId, muteeId)));
    }

    /**
     * Removes all member ids from the give recipient list that have muted the given sender. This
     * can be used to filter the sending of player-to-player bulk mail messages.
     */
    public void filterMuterRecipients (int senderId, Set<Integer> recipientIds)
    {
        for (MuteRecord mrec : findAll(MuteRecord.class, new Where(Ops.and(
            MuteRecord.MUTEE_ID.eq(senderId), MuteRecord.MUTER_ID.in(recipientIds))))) {
            recipientIds.remove(mrec.muterId);
        }
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
     * NOTE: this does not return Friendship.INVITEE, because that requires
     * looking up the friendId's relationship with us.
     */
    public Friendship getFriendship (int memberId, int friendId)
    {
        FriendshipRecord frec = load(FriendshipRecord.getKey(memberId, friendId));
        return (frec == null) ? Friendship.NOT_FRIENDS :
            (frec.valid ? Friendship.FRIENDS : Friendship.INVITED);
    }

    /**
     * Determine the two-way friendship status between two members.
     * You only need to call this method if you need to know if the memberId may
     * have been invited by the friendId; otheriwse please call getFriendship().
     */
    public Friendship getTwoWayFriendship (int memberId, int friendId)
    {
        Friendship fr = getFriendship(memberId, friendId);
        if (fr == Friendship.NOT_FRIENDS) {
            fr = getFriendship(friendId, memberId);
            if (fr == Friendship.INVITED) {
                return Friendship.INVITEE;
            }
        }
        return fr;
    }

    /**
     * Count the number of fully-established friendships that the specified player has.
     */
    public int countFullFriends (int memberId)
    {
        return load(CountRecord.class, new FromOverride(FriendshipRecord.class),
                    fullFriendWhere(memberId)).count;
    }

    /**
     * Loads the member ids of the specified member's friends.
     */
    // TODO: make a version that just loads a list, and use that most every place
    public Set<Integer> loadFriendIds (int memberId)
    {
        List<FriendshipRecord> list = findAll(FriendshipRecord.class, fullFriendWhere(memberId));
        int[] ids = new int[list.size()];
        for (int ii = 0; ii < ids.length; ii++) {
            ids[ii] = list.get(ii).friendId;
        }
        return new StreamableArrayIntSet(ids);
    }

    /**
     * Loads the friend IDs that have logged in in the last month.
     */
    public List<Integer> loadActiveFriendIds (int memberId)
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - 30*24*60*60*1000L);

        // load up the ids of this member's friends (ordered from most recently online to least)
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(Ops.and(
            MemberRecord.LAST_SESSION.greaterThan(cutoff),
            FriendshipRecord.MEMBER_ID.eq(memberId),
            FriendshipRecord.VALID.eq(true))));
        clauses.add(FriendshipRecord.FRIEND_ID.join(MemberRecord.MEMBER_ID));

        List<Integer> friendIds = Lists.newArrayList();
        for (FriendshipRecord frec : findAll(FriendshipRecord.class, clauses)) {
            friendIds.add(frec.friendId);
        }
        return friendIds;
    }


    /**
     * Return a mapping of all friend relationships extending from this member. This means
     * that only FRIENDS and INVITED will be returned.
     */
    public Map<Integer, Friendship> loadFriendships (int memberId)
    {
        Map<Integer, Friendship> ships = Maps.newHashMap();
        for (FriendshipRecord frec : findAll(FriendshipRecord.class,
                new Where(FriendshipRecord.MEMBER_ID, memberId))) {
            ships.put(frec.friendId, frec.valid ? Friendship.FRIENDS : Friendship.INVITED);
        }
        return ships;
    }

    /**
     * Return a mapping of all the specified friend relationships extending from this member.
     * Only FRIENDS and INVITED will be returned, NOT_FRIENDS is implicit with missing ids
     * in the returned map.
     */
    public Map<Integer, Friendship> loadFriendships (int memberId, Collection<Integer> otherIds)
    {
        Map<Integer, Friendship> ships = Maps.newHashMap();
        if (otherIds.isEmpty()) {
            return ships;
        }
        Where where = new Where(Ops.and(FriendshipRecord.MEMBER_ID.eq(memberId),
                                        FriendshipRecord.FRIEND_ID.in(otherIds)));
        for (FriendshipRecord frec : findAll(FriendshipRecord.class, where)) {
            ships.put(frec.friendId, frec.valid ? Friendship.FRIENDS : Friendship.INVITED);
        }
        return ships;
    }

    /**
     * Loads the FriendEntry record for all the specified members.
     */
    public FriendEntry[] loadFriendEntries (Collection<Integer> ids)
    {
        List<MemberCardRecord> cards = loadMemberCards(ids);
        FriendEntry[] entries = new FriendEntry[cards.size()];
        int ii = 0;
        for (MemberCardRecord crec : cards) {
            entries[ii++] = new FriendEntry(crec.toVizMemberName(), crec.headline);
        }
        return entries;
    }

    /**
     * Loads the FriendEntry record for some or all of the most recently online friends of the
     * specified member, sorted by last-online time. The online status of each friend will be false.
     *
     * @param limit the number of friends to load or 0 for all of them.
     */
    public List<MemberCard> loadFriends (int memberId, int limit)
    {
        // load up the ids of this member's friends (ordered from most recently online to least)
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(fullFriendWhere(memberId));
        clauses.add(FriendshipRecord.FRIEND_ID.join(MemberRecord.MEMBER_ID));
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
            clauses.add(OrderBy.descending(MemberRecord.LAST_SESSION));
        }

        // figure out a list of the friend ids
        List<Integer> ids = Lists.newArrayList();
        for (FriendshipRecord frec : findAll(FriendshipRecord.class, clauses)) {
            ids.add(frec.friendId);
        }

        // now load up member card records for these guys and convert them to friend entries
        List<MemberCard> friends = Lists.newArrayListWithCapacity(ids.size());
        for (MemberCardRecord crec : loadMemberCards(ids)) {
            friends.add(crec.toMemberCard());
        }
        return friends;
    }

    /**
     * Makes the specified members friends.
     *
     * @param memberId The id of the member performing this action.
     * @param friendId The id of the other member.
     *
     * @return the member card for the invited friend, or null if the invited friend no longer
     * exists.
     * @exception DuplicateKeyException if the members are already friends.
     */
    public MemberCard noteFriendship (int memberId, int friendId)
    {
        // first load the member record of the potential friend
        MemberCard other = loadMemberCard(friendId, true);
        if (other == null) {
            log.warning("Failed to establish friends: member no longer exists",
                "missingId", friendId, "reqId", memberId);
            return null;
        }

        // TODO: wrap this in a transaction when we support that
        store(new FriendshipRecord(memberId, friendId, true));
        store(new FriendshipRecord(friendId, memberId, true));

        return other;
    }

    /**
     * Note that a friendship invitation has been extended. "Fan".
     */
    public void noteFriendInvitationSent (int memberId, int friendId)
    {
        store(new FriendshipRecord(memberId, friendId, false));
    }

    /**
     * Remove a friend mapping from the database.
     */
    public void clearFriendship (int memberId, int friendId)
    {
        // clear our friendiness right on out
        delete(FriendshipRecord.getKey(memberId, friendId));

        // but keep the friend marked as liking us, if and only if they already had a record
        updatePartial(FriendshipRecord.getKey(friendId, memberId), FriendshipRecord.VALID, false);
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
                               Key.<MemberRecord>toInt());
    }

    /**
     * Returns the id of the account associated with the supplied external account (the caller is
     * responsible for confirming the authenticity of the external id information) or 0 if no
     * account is associated with that external account.
     */
    public int lookupExternalAccount (ExternalSiteId site, String externalId)
    {
        ExternalMapRecord record = load(ExternalMapRecord.getKey(site, externalId));
        return (record == null) ? 0 : record.memberId;
    }

    /**
     * Returns any mapping that associates the given site or related sites with the given user,
     * or null if the account is not associated with any related site. An exact site match is
     * returned if it exists.
     */
    public ExternalMapRecord lookupAnyExternalAccount (ExternalSiteId site, String externalId)
    {
        ExternalMapRecord record = load(ExternalMapRecord.getKey(site, externalId));
        if (record == null) {
            // the order by clause is just for determinism
            List<ExternalMapRecord> related = findAll(ExternalMapRecord.class, new Where(Ops.and(
                ExternalMapRecord.AUTHER.eq(site.auther),
                ExternalMapRecord.EXTERNAL_ID.eq(externalId))),
                OrderBy.ascending(ExternalMapRecord.SITE_ID), new Limit(0, 1));
            record = related.size() > 0 ? related.get(0) : null;
        }
        return record;
    }

    /**
     * Loads all external account records for the given site that match one of the provided
     * external ids.
     */
    public List<ExternalMapRecord> loadExternalAccounts (
        ExternalSiteId site, Collection<String> externalIds)
    {
        if (externalIds.isEmpty()) {
            return Collections.emptyList();
        }
        Where where = new Where(Ops.and(ExternalMapRecord.AUTHER.eq(site.auther),
                                        ExternalMapRecord.SITE_ID.eq(site.siteId),
                                        ExternalMapRecord.EXTERNAL_ID.in(externalIds)));
        return findAll(ExternalMapRecord.class, where);
    }

    /**
     * Returns the Whirled member ids of any of the supplied external ids that have been mapped to
     * Whirled accounts using the given authentication source. This should only be used in
     * situations where maintaining site-specific data is not required.
     */
    public List<Integer> lookupExternalAccounts (ExternalSiteId.Auther auther, List<String> extIds)
    {
        if (extIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> memberIds = Lists.newArrayList();
        for (ExternalMapRecord record : findAll(ExternalMapRecord.class, new Where(Ops.and(
            ExternalMapRecord.AUTHER.eq(auther), ExternalMapRecord.EXTERNAL_ID.in(extIds))))) {
            memberIds.add(record.memberId);
        }
        return memberIds;
    }

    /**
     * Notes that the specified Whirled account is associated with the specified external site.
     */
    public void mapExternalAccount (ExternalSiteId site, String externalId, int memberId)
    {
        ExternalMapRecord record = new ExternalMapRecord();
        record.auther = site.auther;
        record.siteId = site.siteId;
        record.externalId = externalId;
        record.memberId = memberId;
        store(record);
    }

    /**
     * Returns a mapping of external sites to external ids for the specified account.
     */
    public Map<ExternalSiteId, String> loadExternalMappings (int memberId)
    {
        Map<ExternalSiteId, String> authers = Maps.newHashMap();
        for (ExternalMapRecord emr : findAll(ExternalMapRecord.class,
                                             new Where(ExternalMapRecord.MEMBER_ID, memberId))) {
            authers.put(emr.getSiteId(), emr.externalId);
        }
        return authers;
    }

    /**
     * Loads up and returns the most recently saved external session key for the specified
     * member and site. Returns null if the member has no mapping for that site or no saved
     * session key.
     */
    public String lookupExternalSessionKey (ExternalSiteId site, int memberId)
    {
        ExternalMapRecord record = loadExternalMapEntry(site, memberId);
        return (record == null) ? null : record.sessionKey;
    }

    /**
     * Loads up and returns the external map record for the specified member and external site.
     * Returns null if there is no such mapping.
     */
    public ExternalMapRecord loadExternalMapEntry (ExternalSiteId site, int memberId)
    {
        return load(ExternalMapRecord.class, ExternalMapRecord.getMemberKey(site, memberId));
    }

    /**
     * Updates the supplied member's session key mapping for the specified external site.
     */
    public void updateExternalSessionKey (ExternalSiteId site, int memberId, String sessionKey)
    {
        // load the record so that we can do our update using the primary key
        ExternalMapRecord record = load(
            ExternalMapRecord.class, ExternalMapRecord.getMemberKey(site, memberId));
        if (record != null) {
            updatePartial(ExternalMapRecord.getKey(site, record.externalId),
                          ExternalMapRecord.SESSION_KEY, sessionKey);
        }
    }

    /**
     * Loads all mappings for the given external site.
     */
    public List<ExternalMapRecord> loadExternalMappings (ExternalSiteId site)
    {
        // doing an unlimited, cached findAll here is causing ye olde "java.io.IOException: Tried
        // to send an out-of-range integer as a 2-byte value" in the depot. However, bypassing the
        // cache could still cause problems. So rather than do that, release and wait to see if
        // another release is needed, just build up the list manually in batches.
        // TODO: this should not be so complicated, implement a decent way to just get a full set
        // of data in cases where speed is not the most important factor
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(ExternalMapRecord.AUTHER.eq(site.auther)));
        // sort so things don't get jumbled
        clauses.add(OrderBy.ascending(ExternalMapRecord.MEMBER_ID));
        clauses.add(null);

        List<ExternalMapRecord> records = Lists.newArrayList();
        int limitSlot = clauses.size() - 1;
        for (int offset = 0, batchSize = 5000; batchSize > 0; offset += batchSize) {
            clauses.set(limitSlot, new Limit(offset, batchSize));
            List<ExternalMapRecord> batch = findAll(ExternalMapRecord.class, clauses);
            records.addAll(batch);
            batchSize = batch.size();
        }
        return records;
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
        updateSpanked(memberId, true);
    }

    /**
     * Updates the warning message for a member, or creates a new warning message if none exists.
     */
    public void updateMemberWarning (int memberId, String warning)
    {
        if (updatePartial(MemberWarningRecord.getKey(memberId),
                          MemberWarningRecord.WARNING, warning) == 0) {
            MemberWarningRecord record = new MemberWarningRecord();
            record.memberId = memberId;
            record.warning = warning;
            insert(record);
            updateSpanked(memberId, true);
        }
    }

    /**
     * Clears a warning message from a member (this includes any temp ban information).
     */
    public void clearMemberWarning (int memberId)
    {
        delete(MemberWarningRecord.getKey(memberId));
        updateSpanked(memberId, false);
    }

    /**
     * Returns the MemberWarningRecord for the memberId, or null if none found.
     */
    public MemberWarningRecord loadMemberWarningRecord (int memberId)
    {
        return load(MemberWarningRecord.getKey(memberId));
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
        delete(CharityRecord.getKey(memberId));
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
        return load(CharityRecord.getKey(memberId));
    }

    /**
     * Changes the member's charity to the specified member ID.
     */
    public void updateSelectedCharity (int memberId, int charityMemberId)
    {
        Preconditions.checkArgument(
            charityMemberId >= 0, "Charity member ID must be zero or positive.");
        updatePartial(MemberRecord.getKey(memberId),
                      MemberRecord.CHARITY_MEMBER_ID, charityMemberId);
    }

    /**
     * Returns the member ids of all permaguest accounts that have not logged in in the past N
     * days and have not achieved at least level 5. The list is limited to a maximum number.
     */
    public List<Integer> loadExpiredWeakPermaguestIds (long now)
    {
        return Lists.transform(findAllKeys(MemberRecord.class, false, weakPermaguestWhere(now),
                                           new Limit(0, MAX_WEAK_ACCOUNTS)),
                               Key.<MemberRecord>toInt());
    }

    /**
     * Returns the number of permaguest accounts that have not logged in in the past N days and
     * have not achieved at least level 5.
     */
    public int countExpiredWeakPermaguestIds (long now)
    {
        return load(CountRecord.class, weakPermaguestWhere(now),
            new FromOverride(MemberRecord.class)).count;
    }

    /**
     * Notes that a payment has been awarded for getting a friend to join. The caller must ensure
     * all conditions of payment have been met.
     * @throws DuplicateKeyException if the friend has already awarded a payment to someone else
     */
    public void noteFriendPayment (int friendId, int paidMemberId)
    {
        FriendPayoutRecord payout = new FriendPayoutRecord();
        payout.friendId = friendId;
        payout.paidMemberId = paidMemberId;
        payout.time = new Timestamp(System.currentTimeMillis());
        insert(payout);
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
     * Updates the SPANKED flag for a given member. Used by the member warning methods in this
     * class.
     */
    protected void updateSpanked (int memberId, boolean value)
    {
        MemberRecord mrec = loadMember(memberId);
        if (mrec.updateFlag(MemberRecord.Flag.SPANKED, value)) {
            storeFlags(mrec);
        }
    }

    /**
     * Convenience to return a Where for finding full friendship.
     */
    protected Where fullFriendWhere (int memberId)
    {
        return new Where(Ops.and(FriendshipRecord.MEMBER_ID.eq(memberId),
                                 FriendshipRecord.VALID.eq(true)));
    }

    protected Where weakPermaguestWhere (long now)
    {
        Timestamp cutoff = new Timestamp(now - WEAK_PERMAGUEST_EXPIRE);
        return new Where(Ops.and(MemberRecord.ACCOUNT_NAME.like(PERMA_PATTERN),
                                 MemberRecord.LEVEL.lessThan(STRONG_PERMAGUEST_LEVEL),
                                 MemberRecord.LAST_SESSION.lessThan(cutoff)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberRecord.class);
        classes.add(FriendshipRecord.class);
        classes.add(SessionRecord.class);
        classes.add(ExternalMapRecord.class);
        classes.add(MemberWarningRecord.class);
        classes.add(AffiliateRecord.class);
        classes.add(MemberExperienceRecord.class);
        classes.add(CharityRecord.class);
        classes.add(EntryVectorRecord.class);
        classes.add(MuteRecord.class);
        classes.add(FriendPayoutRecord.class);
    }

    @Inject protected UserActionRepository _actionRepo;

    protected static final SQLExpression<?> GREETER_FLAG_IS_SET =
        MemberRecord.FLAGS.bitAnd(MemberRecord.Flag.GREETER.getBit()).notEq(0);

    /**
     * Period after which we expire entry vector records that are not associated with members.
     * This should not be shorter than the period for which we want to create a funnel report.
     */
    protected static final long ENTRY_VECTOR_EXPIRE = FUNNEL_TOTAL_DAYS * 24*60*60*1000L;

    /** Period after which we expire session records. */
    protected static final long SESSION_RECORD_EXPIRE = 6 * 30 * 24*60*60*1000L;

    /** A "like" pattern that matches permaguest accounts. */
    protected static final String PERMA_PATTERN = MemberMailUtil.makePermaguestEmail("%");

    /**
     * Period after which we expire "weak" permaguest accounts (those of low level). This should
     * not be shorter than the period for which we want to create a funnel report.
     */
    protected static final long WEAK_PERMAGUEST_EXPIRE = FUNNEL_TOTAL_DAYS * 24*60*60*1000L;

    /** A permaguest that fails to achieve this level is considered weak, and purged. */
    protected static final int STRONG_PERMAGUEST_LEVEL = 5;

    /** We'll only return 1000 weak accounts at a time for purging. */
    protected static final int MAX_WEAK_ACCOUNTS = 1500;
}
