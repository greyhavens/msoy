//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Tuple;

import com.samskivert.depot.CacheInvalidator;
import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DateFuncs;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.Case;
import com.samskivert.depot.operator.FullText;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService.GroupQuery;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagRepository;

/**
 * Manages the persistent store of group data.
 */
@Singleton @BlockingThread
public class GroupRepository extends DepotRepository
{
    @Entity(name="GroupTagRecord")
    public static class GroupTagRecord extends TagRecord
    {
    }

    @Entity(name="GroupTagHistoryRecord")
    public static class GroupTagHistoryRecord extends TagHistoryRecord
    {
    }

    /**
     * Encapsulates information regarding a word search group froups: we look up each word as
     * a tag and we create a full-text query for it. The resulting Depot expressions are used
     * both to filter and to rank
     * search results.
     */
    public class WordSearch
    {
        public FullText.Match fullTextMatch ()
        {
            return _fts.match();
        }

        public FullText.Rank fullTextRank ()
        {
            return _fts.rank();
        }

        public SQLExpression<?> tagExistsExpression ()
        {
            if (_tagIds.size() == 0) {
                return null;
            }
            Where where = new Where(Ops.and(
                _tagRepo.getTagColumn(GroupTagRecord.TARGET_ID).eq(GroupRecord.GROUP_ID),
                _tagRepo.getTagColumn(GroupTagRecord.TAG_ID).in(_tagIds)));
            return Ops.exists(new SelectClause(GroupTagRecord.class,
                new ColumnExp<?>[] { _tagRepo.getTagColumn(TagRecord.TAG_ID) }, where));
        }

        protected WordSearch (String search)
        {
            // first split our search up into words
            String[] searchTerms = search.toLowerCase().split("\\W+");
            if (searchTerms.length > 0 && searchTerms[0].length() == 0) {
                searchTerms = ArrayUtil.splice(searchTerms, 0, 1);
            }

            // look up each word as a tag
            _tagIds = new ArrayIntSet();
            if (searchTerms.length > 0) {
                for (TagNameRecord tRec : getTagRepository().getTags(searchTerms)) {
                    _tagIds.add(tRec.tagId);
                }
            }

            _fts = new FullText(GroupRecord.class, GroupRecord.FTS_NBC, search, true);
        }

        protected Set<Integer> _tagIds;
        protected FullText _fts;
    }

    @Inject public GroupRepository (PersistenceContext ctx)
    {
        super(ctx);

        _tagRepo = new TagRepository(_ctx) {
            @Override
            protected TagRecord createTagRecord () {
                return new GroupTagRecord();
            }
            @Override
            protected TagHistoryRecord createTagHistoryRecord () {
                return new GroupTagHistoryRecord();
            }
        };

        // TEMP: drop some old fields
        String[] oldFields = {
            "backgroundControl", "backgroundMimeType", "backgroundHash",
            "backgroundThumbConstraint" };
        for (String oldField : oldFields) {
            ctx.registerMigration(GroupRecord.class, new SchemaMigration.Drop(22, oldField));
        }
    }

    /**
     * Returns the repository used to track tags on groups.
     */
    public TagRepository getTagRepository ()
    {
        return _tagRepo;
    }

    /**
     * Returns a list of all public and inv-only groups, sorted by "new & popular"
     *
     * @param count a limit to the number of groups to load or Integer.MAX_VALUE for all of them.
     */
    public List<GroupRecord> getGroups (int count)
    {
        return getGroups(0, count, new GroupQuery(), false);
    }

    /**
     * Returns a list of all public and inv-only groups, sorted by "new & popular"
     *
     * @param query Limit, search and sort options. query.searchString and query.tag are mutually
     * exclusive. If either is set, results will be ordered by relevance instead of query.sort.
     * @param b
     */
    public List<GroupRecord> getGroups (int offset, int count, GroupQuery query, boolean support)
    {
        int tagId = 0;
        if (query.tag != null) {
            TagNameRecord tnr = _tagRepo.getTag(query.tag);
            if (tnr == null) {
                return Collections.emptyList();
            }
            tagId = tnr.tagId;
        }

        WordSearch search = (query.search != null) ? new WordSearch(query.search) : null;

        List<QueryClause> clauses = buildSearchClauses(search, tagId, support);
        clauses.add(buildOrderBy(query.sort, tagId, search));
        clauses.add(new Limit(offset, count));

        return findAll(GroupRecord.class, clauses);
    }

    /**
     * Returns the total count of visible groups for a given query (which corresponds to the
     * number of groups available via calls to {@link #getGroups}).
     */
    public int getGroupCount (GroupQuery query, boolean support)
    {
        int tagId = 0;
        if (query.tag != null) {
            TagNameRecord tnr = _tagRepo.getTag(query.tag);
            if (tnr == null) {
                return 0;
            }
            tagId = tnr.tagId;
        }

        WordSearch search = (query.search != null) ? new WordSearch(query.search) : null;

        List<QueryClause> clauses = buildSearchClauses(search, tagId, support);
        clauses.add(new FromOverride(GroupRecord.class));
        return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
    }

    /**
     * Fetches a single group, by id. Returns null if there's no such group.
     */
    public GroupRecord loadGroup (int groupId)
    {
        return load(GroupRecord.getKey(groupId));
    }

    /**
     * Returns the single group with the given unique name, or null if none is found.
     */
    public GroupRecord loadGroupByName (String name)
    {
        return load(GroupRecord.class, new Where(GroupRecord.NAME.eq(name)));
    }

    /**
     * Fetches multiple groups by id.
     */
    public List<GroupRecord> loadGroups (Collection<Integer> groupIds)
    {
        return loadAll(GroupRecord.class, groupIds);
    }

    /**
     * Looks up a group's name by id. Returns null if no group exists with the specified id.
     */
    public GroupName loadGroupName (int groupId)
    {
        Map<Integer, GroupName> result = loadGroupNames(Collections.singleton(groupId));
        return result.isEmpty() ? null : result.values().iterator().next();
    }

    /**
     * Looks up group names by calling the supplied function on a collection of objects to
     * extract an id from each one. The function may return null to indicate that object
     * should not be include in the returned mapping.
     */
    public <C> Map<Integer, GroupName> loadGroupNames (Iterable<C> records, Function<C, Integer> getId)
    {
        Set<Integer> groupIds = new ArrayIntSet();
        for (C record : records) {
            Integer id = getId.apply(record);
            if (id != null) {
                groupIds.add(id);
            }
        }
        return loadGroupNames(groupIds);
    }

    /**
     * Looks up members' names by id.
     */
    public Map<Integer, GroupName> loadGroupNames (Iterable<Integer> groupIds)
    {
        Map<Integer, GroupName> names = Maps.newHashMap();
        for (GroupNameRecord name : loadAll(GroupNameRecord.class, groupIds)) {
            names.put(name.groupId, name.toGroupName());
        }
        return names;
    }

    /**
     * Creates a new group, defined by a {@link GroupRecord}. The key of the record must be null --
     * it will be filled in through the insertion, and returned.  A blank room is also created that
     * is owned by the group.
     */
    public int createGroup (GroupRecord record)
    {
        if (record.groupId != 0) {
            throw new IllegalArgumentException(
                "Group record must have a null id for creation " + record);
        }
        record.creationDate = new Date(System.currentTimeMillis());
        insert(record);

        return record.groupId;
    }

    /**
     * Updates the game associated with the specified group.
     */
    public void updateGroupGame (int groupId, int gameId)
    {
        updatePartial(GroupRecord.getKey(groupId), GroupRecord.GAME_ID, gameId);
    }

    /**
     * Updates the specified group record with supplied field/value mapping.
     */
    public void updateGroup (int groupId, Map<ColumnExp<?>, Object> updates)
    {
        updatePartial(GroupRecord.getKey(groupId), updates);
    }

    /**
     * Hides the specified group by setting its policy to {@link Group.Policy#EXCLUSIVE}. Since
     * the group has no members, it will no longer be visible to anyone except support. Does no
     * verification on the emptiness of the group.
     */
    public void hideEmptyGroup (int groupId)
    {
        updatePartial(GroupRecord.getKey(groupId), GroupRecord.POLICY, Group.Policy.EXCLUSIVE);
    }

    /**
     * Makes a given person a member of a given group.
     */
    public void addMember (int groupId, int memberId, Rank rank)
    {
        GroupMembershipRecord record = new GroupMembershipRecord();
        record.groupId = groupId;
        record.memberId = memberId;
        record.rank = rank;
        record.rankAssigned = new Timestamp(System.currentTimeMillis());
        insert(record);
        updateMemberCount(groupId);
    }

    /**
     * Sets the rank of a member of a group.
     */
    public void setRank (int groupId, int memberId, Rank newRank)
    {
        int rows = updatePartial(
            GroupMembershipRecord.getKey(memberId, groupId),
            GroupMembershipRecord.RANK, newRank,
            GroupMembershipRecord.RANK_ASSIGNED, new Timestamp(System.currentTimeMillis()));
        if (rows == 0) {
            throw new DatabaseException(
                "Couldn't find group membership to modify [groupId=" + groupId +
                "memberId=" + memberId + "]");
        }
    }

    /**
     * Returns the rank of the specified member in the specified group or {@link
     * GroupMembership.Rank#NON_MEMBER} if they are a non-member.
     */
    public Rank getRank (int groupId, int memberId)
    {
        GroupMembershipRecord gmr = loadMembership(groupId, memberId);
        return (gmr == null) ? Rank.NON_MEMBER : gmr.rank;
    }

    /**
     * Fetches the membership details for a given group and member. If the member is not a member
     * of the specified group, a tuple will be provided with non-member as their rank and the start
     * of the epoch for their rank assigned time.
     */
    public Tuple<Rank, Long> getMembership (int groupId, int memberId)
    {
        GroupMembershipRecord gmr = loadMembership(groupId, memberId);
        return gmr == null ? Tuple.newTuple(Rank.NON_MEMBER, 0L) :
            Tuple.newTuple(gmr.rank, gmr.rankAssigned.getTime());
    }

    /**
     * Resolves the group membership information for the supplied member.
     *
     * @param filter if non-null, only membership in groups that match the supplied filter will be
     * returned.
     */
    public List<GroupMembership> resolveGroupMemberships (
        int memberId, Predicate<Tuple<GroupRecord,GroupMembershipRecord>> filter)
    {
        Map<Integer, GroupMembershipRecord> rmap = getMemberships(memberId);

        // potentially filter exclusive groups and resolve the group names
        Map<Integer, GroupName> groupNames = Maps.newHashMap();
        for (GroupRecord group : loadGroups(rmap.keySet())) {
            if (filter == null || filter.apply(Tuple.newTuple(group, rmap.get(group.groupId)))) {
                groupNames.put(group.groupId, group.toGroupName());
            }
        }

        // convert the persistent membership records into runtime records
        List<GroupMembership> groups = Lists.newArrayList();
        for (GroupMembershipRecord record : rmap.values()) {
            if (groupNames.containsKey(record.groupId)) {
                groups.add(record.toGroupMembership(groupNames));
            }
        }
        return groups;
    }

    /**
     * Returns a list of cards for the groups of which the specified person is a member.
     */
    public List<GroupCard> getMemberGroups (int memberId, boolean includeExclusive)
    {
        Map<Integer, GroupMembershipRecord> rmap = getMemberships(memberId);

        // potentially filter exclusive groups and resolve the group names
        List<GroupCard> groups = Lists.newArrayList();
        for (GroupRecord group : loadGroups(rmap.keySet())) {
            if (group.policy != Group.Policy.EXCLUSIVE || includeExclusive) {
                groups.add(group.toGroupCard());
            }
        }
        return groups;
    }

    /**
     * Load & return the brand shareholder details of a certain group. The result is ordered
     * by share count (descending) and, strictly as a backup, by member id.
     */
    public List<BrandShareRecord> getBrandShares (int groupId)
    {
        return findAll(BrandShareRecord.class,
            new Where(BrandShareRecord.GROUP_ID, groupId),
            new OrderBy(new SQLExpression<?>[] {
                BrandShareRecord.SHARES, BrandShareRecord.MEMBER_ID,
            }, new OrderBy.Order[] { Order.DESC, Order.ASC }));
    }

    /**
     * Load & return the various brand shares held by a certain member.
     */
    public List<BrandShareRecord> getBrands (int memberId)
    {
        // TODO: disabling caching here is non-ideal but there's something mysterious going
        // TODO: on that I will debug after today's release. it's also not a huge deal because
        // TODO: BrandShareRecord is going to be a small table entirely cached in the DB's RAM.
        return findAll(BrandShareRecord.class, CacheStrategy.NONE,
            new Where(BrandShareRecord.MEMBER_ID, memberId));
    }

    /**
     * Find the number of shares held by a given member in a given group.
     */
    public int getBrandShare (int groupId, int memberId)
    {
        BrandShareRecord record = load(BrandShareRecord.getKey(memberId, groupId));
        return record != null ? record.shares : 0;
    }

    /**
     * Update the number of shares the given player has in the brand. The player must be a member
     * of the group or undefined badness may occur.
     */
    public void setBrandShare (int groupId, int memberId, int shares)
    {
        if (shares == 0) {
            delete(BrandShareRecord.getKey(memberId, groupId));
        } else {
            store(new BrandShareRecord(memberId, groupId, shares));
        }
    }

    /**
     * Remove a given person as member of a given group. This method returns false if there was no
     * membership to cancel.
     */
    public boolean leaveGroup (int groupId, int memberId)
    {
        int rows = delete(GroupMembershipRecord.getKey(memberId, groupId));
        updateMemberCount(groupId);
        delete(BrandShareRecord.getKey(memberId, groupId));
        return rows > 0;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public int countMembers (int groupId)
    {
        return load(CountRecord.class,
                    new FromOverride(GroupMembershipRecord.class),
                    new Where(GroupMembershipRecord.GROUP_ID, groupId)).count;
    }

    /**
     * Fetches the ids of the members of a given group.
     */
    public List<Integer> getMemberIds (int groupId)
    {
        return getMemberIds(groupId, null); // all ranks
    }

    /**
     * Fetches the ids of the members of a given group that share a given rank.
     */
    public List<Integer> getMemberIdsWithRank (int groupId, Rank rank)
    {
        return getMemberIds(groupId, rank);
    }

    /**
     * Fetches the memberships for each of a given set of members in a group.
     */
    public List<GroupMembershipRecord> getMembers (int groupId, Collection<Integer> memberIds)
    {
        if (memberIds.size() == 0) {
            return Collections.emptyList();
        }
        return findAll(GroupMembershipRecord.class,
            new Where(Ops.and(GroupMembershipRecord.GROUP_ID.eq(groupId),
                GroupMembershipRecord.MEMBER_ID.in(memberIds))));
    }

    /**
     * Fetches the group memberships a given member belongs to.
     */
    public Map<Integer, GroupMembershipRecord> getMemberships (int memberId)
    {
        List<GroupMembershipRecord> recs = findAll(
            GroupMembershipRecord.class, new Where(GroupMembershipRecord.MEMBER_ID, memberId));

        Map<Integer, GroupMembershipRecord> result = Maps.newHashMap();
        for (GroupMembershipRecord rec : recs) {
            result.put(rec.groupId, rec);
        }
        return result;
    }

    /**
     * Fetches the group memberships in which the given member has a certain rank.
     */
    public List<GroupMembershipRecord> getMemberships (int memberId, Rank rank)
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.MEMBER_ID, memberId,
                                 GroupMembershipRecord.RANK, rank));
    }

    /**
     * Fetches the full records of the groups a given member belongs to ordered from most populous
     * to least.
     *
     * @param limit if greater than zero, a limit on the number of groups to return.
     */
    public List<GroupRecord> getFullMemberships (int memberId, int limit)
    {
        return findAll(GroupRecord.class,
                       GroupRecord.GROUP_ID.join(GroupMembershipRecord.GROUP_ID),
                       new Where(GroupMembershipRecord.MEMBER_ID, memberId),
                       OrderBy.descending(GroupRecord.MEMBER_COUNT),
                       new Limit(0, (limit > 0) ? limit : Integer.MAX_VALUE));
    }

    /**
     * Load all groups with the official flag set.
     */
    public List<GroupRecord> getOfficialGroups ()
    {
        return findAll(GroupRecord.class, new Where(GroupRecord.OFFICIAL, true));
    }

    /**
     * Sets the home scene id for the given group.
     */
    public void setHomeSceneId (int groupId, int sceneId)
    {
        updatePartial(GroupRecord.getKey(groupId), GroupRecord.HOME_SCENE_ID, sceneId);
    }

    /**
     * Returns the home scene id for the given group.
     */
    public int getHomeSceneId (int groupId)
    {
        GroupRecord rec = load(GroupRecord.getKey(groupId));
        if (rec == null) {
            throw new DatabaseException("Group not found in getHomeSceneId! [" + groupId + "]");
        }

        return rec.homeSceneId;
    }

    /**
     * Deletes all the data associated with the supplied game.
     */
    public void purgeGame (final int gameId)
    {
        updatePartial(GroupRecord.class,
            new Where(GroupRecord.GAME_ID, gameId),
            new CacheInvalidator.TraverseWithFilter<GroupRecord>(GroupRecord.class) {
                protected boolean testForEviction (Serializable key, GroupRecord record) {
                    return record.gameId == gameId;
                }
            }, GroupRecord.GAME_ID, 0);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // note: if these members were the last manager of any groups, they are left high and dry;
        // given that we only purge permaguests currently, this is a non-issue
        deleteAll(GroupMembershipRecord.class,
            new Where(GroupMembershipRecord.MEMBER_ID.in(memberIds)));
        deleteAll(BrandShareRecord.class,
            new Where(BrandShareRecord.MEMBER_ID.in(memberIds)));
        _tagRepo.purgeMembers(memberIds);
    }

    /**
     * Gets all group ids with the given policy.
     */
    public List<Integer> getGroupIdsWithPolicy (Group.Policy policy)
    {
        return Lists.transform(findAllKeys(GroupRecord.class, false, new Where(
            GroupRecord.POLICY, policy)), Key.<GroupRecord>toInt());
    }

    protected GroupMembershipRecord loadMembership (int groupId, int memberId)
    {
        return load(GroupMembershipRecord.getKey(memberId, groupId));
    }

    protected void updateMemberCount (int groupId)
    {
        updatePartial(GroupRecord.getKey(groupId), GroupRecord.MEMBER_COUNT, countMembers(groupId));
    }

    /**
     * Return the where clause for a group select based on a given query
     *
     * TODO: Ideally this would not hide exclusive groups from their members/owners
     */
    protected List<QueryClause> buildSearchClauses (WordSearch search, int tagId, boolean support)
    {
        List<QueryClause> clauses = Lists.newArrayList();

        List<SQLExpression<?>> conditions = Lists.newArrayList();

        if (!support) {
            // if we're not support, skip exclusive groups in the result
            conditions.add(Ops.not(GroupRecord.POLICY.eq(Group.Policy.EXCLUSIVE)));
        }

        if (search != null) {
            List<SQLExpression<?>> wordBits =
                Lists.<SQLExpression<?>>newArrayList(search.fullTextMatch());
            SQLExpression<?> tagOp = search.tagExistsExpression();
            if (tagOp != null) {
                wordBits.add(tagOp);
            }
            conditions.add(Ops.or(wordBits));

        } else if (tagId > 0) {
            conditions.add(_tagRepo.getTagColumn(TagRecord.TAG_ID).eq(tagId));
            clauses.add(GroupRecord.GROUP_ID.join(_tagRepo.getTagColumn(TagRecord.TARGET_ID)));

        }
        if (conditions.size() > 0) {
            clauses.add(new Where(Ops.and(conditions)));
        }
        return clauses;
    }

    protected OrderBy buildOrderBy (byte sortBy, int tagId, WordSearch search)
    {
        // if no full text or tag search, return a subset of all records, order by query.sort
        if (sortBy == GroupQuery.SORT_BY_NAME) {
            return OrderBy.ascending(GroupRecord.NAME);

        } else if (sortBy == GroupQuery.SORT_BY_NUM_MEMBERS) {
            return OrderBy.descending(GroupRecord.MEMBER_COUNT).thenAscending(GroupRecord.NAME);

        } else if (sortBy == GroupQuery.SORT_BY_CREATED_DATE) {
            return OrderBy.ascending(GroupRecord.CREATION_DATE).
                thenDescending(GroupRecord.MEMBER_COUNT).thenAscending(GroupRecord.NAME);

        } else if (tagId > 0) {
            // for a tag search, define 'relevance' as member count
            return OrderBy.descending(GroupRecord.MEMBER_COUNT).thenAscending(GroupRecord.NAME);

        } else if (search != null) {
            SQLExpression<?> tagExistsExp = search.tagExistsExpression();
            if (tagExistsExp != null) {
                // the rank is (1 + fts rank), boosted by 25% if there's a tag match
                return OrderBy.descending(Ops.mul(
                    search.fullTextRank().plus(Exps.value(1.0)),
                    new Case<Double>(tagExistsExp, Exps.value(1.25), Exps.value(1.0)))).
                        thenDescending(GroupRecord.MEMBER_COUNT).
                        thenAscending(GroupRecord.NAME);

            } else {
                return OrderBy.descending(search.fullTextRank()).
                    thenDescending(GroupRecord.MEMBER_COUNT).thenAscending(GroupRecord.NAME);
            }

        } else {
            // SORT_BY_NEW_AND_POPULAR: subtract 8 members per day the group has been around
            long membersPerDay = (24 * 60 * 60) / 8;
            return OrderBy.descending(DateFuncs.epoch(GroupRecord.CREATION_DATE).
                                      div(membersPerDay).plus(GroupRecord.MEMBER_COUNT)).
                thenAscending(GroupRecord.NAME);
        }
    }

    /**
     * Fetches the membership roster of a given group for a given rank.
     * @param rank the rank to select, or null for all members
     */
    protected List<Integer> getMemberIds (int groupId, Rank rank)
    {
        SQLExpression<?> test = GroupMembershipRecord.GROUP_ID.eq(groupId);
        if (rank != null) {
            test = Ops.and(test, GroupMembershipRecord.RANK.eq(rank));
        }

        return Lists.transform(findAllKeys(GroupMembershipRecord.class, false, new Where(test)),
            Key.<GroupMembershipRecord>toInt());
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GroupRecord.class);
        classes.add(GroupMembershipRecord.class);
        classes.add(BrandShareRecord.class);
    }

    /** Used to manage our group tags. */
    protected TagRepository _tagRepo;

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
}
