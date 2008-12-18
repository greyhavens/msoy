//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.EpochSeconds;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.Arithmetic;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals.FullTextMatch;
import com.samskivert.depot.operator.Conditionals.GreaterThanEquals;
import com.samskivert.depot.operator.Logic.And;
import com.samskivert.depot.operator.Logic.Not;
import com.samskivert.depot.operator.SQLOperator;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagRepository;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService.GroupQuery;

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

    @Inject public GroupRepository (PersistenceContext ctx)
    {
        super(ctx);

        _tagRepo = new TagRepository(_ctx) {
            protected TagRecord createTagRecord () {
                return new GroupTagRecord();
            }
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
        return getGroups(0, count, new GroupQuery());
    }

    /**
     * Returns a list of all public and inv-only groups, sorted by "new & popular"
     *
     * @param query Limit, search and sort options. query.searchString and query.tag are mutually
     * exclusive. If either is set, results will be ordered by relevance instead of query.sort.
     */
    public List<GroupRecord> getGroups (int offset, int count, GroupQuery query)
    {
        // if there is a search string, do a full text match, order by relevance
        if (query.search != null) {
            // for now, always operate with boolean searching enabled, without query expansion
            return findAll(GroupRecord.class, getGroupWhere(query),
                new Limit(offset, count));

        // if there is a tag, fetch groups with GroupTagRecords for that tag, order by groupId
        } else if (query.tag != null) {
            return findAll(GroupRecord.class, getGroupWhere(query), new Join(GroupRecord.class,
                    GroupRecord.GROUP_ID, GroupTagRecord.class, GroupTagRecord.TARGET_ID),
                new Limit(offset, count));
        }

        // if no full text or tag search, return a subset of all records, order by query.sort
        OrderBy orderBy;
        if (query.sort == GroupQuery.SORT_BY_NAME) {
            orderBy = OrderBy.ascending(GroupRecord.NAME_C);
        } else if (query.sort == GroupQuery.SORT_BY_NUM_MEMBERS) {
            orderBy = OrderBy.descending(GroupRecord.MEMBER_COUNT_C);
        } else if (query.sort == GroupQuery.SORT_BY_CREATED_DATE) {
            orderBy = OrderBy.ascending(GroupRecord.CREATION_DATE_C);
        } else {
            // SORT_BY_NEW_AND_POPULAR: subtract 2 members per day the group has been around
            long membersPerDay = (24 * 60 * 60) / 2;
            long nowSeconds = System.currentTimeMillis() / 1000;
            orderBy = OrderBy.descending(
                new Arithmetic.Sub(GroupRecord.MEMBER_COUNT_C,
                    new Arithmetic.Div(
                        new Arithmetic.Sub(new ValueExp(nowSeconds),
                            new EpochSeconds(GroupRecord.CREATION_DATE_C)), membersPerDay))
                );
        }

        return findAll(
            GroupRecord.class, getGroupWhere(query), new Limit(offset, count), orderBy);
    }

    /**
     * Returns the total count of visible groups for a given query (which corresponds to the
     * number of groups available via calls to {@link #getGroups}).
     */
    public int getGroupCount (GroupQuery query)
    {
        // if there is a search string, return count of full text search
        if (query.search != null) {
            return load(CountRecord.class, new FromOverride(GroupRecord.class),
                getGroupWhere(query)).count;

        // if there is a tag, return count of GroupTagRecords for that tag
        } else if (query.tag != null) {
            return load(CountRecord.class, new FromOverride(GroupRecord.class),
                new Join(GroupRecord.class, GroupRecord.GROUP_ID,
                    GroupTagRecord.class, GroupTagRecord.TARGET_ID),
                    getGroupWhere(query)).count;

        // if no full text or tag search, return count of all public records
        } else {
            return load(CountRecord.class, new FromOverride(GroupRecord.class),
                getGroupWhere(query)).count;
        }
    }

    /**
     * Fetches a single group, by id. Returns null if there's no such group.
     */
    public GroupRecord loadGroup (int groupId)
    {
        return load(GroupRecord.class, groupId);
    }

    /**
     * Returns the single group with the given unique name, or null if none is found.
     */
    public GroupRecord loadGroupByName (String name)
    {
        return load(GroupRecord.class, new Where(new Equals(GroupRecord.NAME_C, name)));
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
        List<GroupName> result = loadGroupNames(Collections.singleton(groupId));
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Looks up groups' names by id.
     */
    public List<GroupName> loadGroupNames (Set<Integer> groupIds)
    {
        List<GroupName> names = Lists.newArrayList();
        if (groupIds.size() > 0) {
            for (GroupNameRecord gnr : loadAll(GroupNameRecord.class, groupIds)) {
                names.add(gnr.toGroupName());
            }
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

        int sceneId = _sceneRepo.createBlankRoom(
            MsoySceneModel.OWNER_TYPE_GROUP, record.groupId, record.name, null, true);
        updateGroup(record.groupId, GroupRecord.HOME_SCENE_ID, sceneId);

        return record.groupId;
    }

    /**
     * Updates the specified group record with field/value pairs, e.g.
     *     updateGroup(groupId,
     *                 GroupRecord.CHARTER, newCharter,
     *                 GroupRecord.POLICY, Group.EXCLUSIVE);
     */
    public void updateGroup (int groupId, Object... fieldValues)
    {
        int rows = updatePartial(GroupRecord.class, groupId, fieldValues);
        if (rows == 0) {
            throw new DatabaseException("Couldn't find group for update [id=" + groupId + "]");
        }
    }

    /**
     * Updates the specified group record with supplied field/value mapping.
     */
    public void updateGroup (int groupId, Map<String, Object> updates)
    {
        int rows = updatePartial(GroupRecord.class, groupId, updates);
        if (rows == 0) {
            throw new DatabaseException("Couldn't find group for update [id=" + groupId + "]");
        }
    }

    /**
     * Deletes the specified group from the repository. This assumes that the group has no members
     * and thus does not remove any {@link GroupMembershipRecord} rows. It also assumes the caller
     * will take care of deleting the group's scenes.
     */
    public void deleteGroup (int groupId)
    {
        delete(GroupRecord.class, groupId);
    }

    /**
     * Makes a given person a member of a given group.
     */
    public void joinGroup (int groupId, int memberId, byte rank)
    {
        GroupMembershipRecord record = new GroupMembershipRecord();
        record.groupId = groupId;
        record.memberId = memberId;
        record.rank = rank;
        record.rankAssigned = new Timestamp(System.currentTimeMillis());
        insert(record);
        updateMemberCount(groupId);

        _eventLog.groupJoined(memberId, groupId);
    }

    /**
     * Sets the rank of a member of a group.
     */
    public void setRank (int groupId, int memberId, byte newRank)
    {
        Key<GroupMembershipRecord> key = GroupMembershipRecord.getKey(memberId, groupId);
        int rows = updatePartial(
            GroupMembershipRecord.class, key, key,
            GroupMembershipRecord.RANK, newRank,
            GroupMembershipRecord.RANK_ASSIGNED, new Timestamp(System.currentTimeMillis()));
        if (rows == 0) {
            throw new DatabaseException(
                "Couldn't find group membership to modify [groupId=" + groupId +
                "memberId=" + memberId + "]");
        } else {
            _eventLog.groupRankChange(memberId, groupId, newRank);
        }
    }

    /**
     * Returns the rank of the specified member in the specified group or {@link
     * GroupMembership#RANK_NON_MEMBER} if they are a non-member.
     */
    public byte getRank (int groupId, int memberId)
    {
        GroupMembershipRecord gmr = loadMembership(groupId, memberId);
        return (gmr == null) ? GroupMembership.RANK_NON_MEMBER : gmr.rank;
    }

    /**
     * Fetches the membership details for a given group and member. If the member is not a member
     * of the specified group, a tuple will be provided with non-member as their rank and the start
     * of the epoch for their rank assigned time.
     */
    public Tuple<Byte, Long> getMembership (int groupId, int memberId)
    {
        GroupMembershipRecord gmr = loadMembership(groupId, memberId);
        return gmr == null ? Tuple.newTuple(GroupMembership.RANK_NON_MEMBER, 0L) :
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
        List<GroupMembershipRecord> records = getMemberships(memberId);
        IntMap<GroupMembershipRecord> rmap = IntMaps.newHashIntMap();
        for (GroupMembershipRecord record : records) {
            rmap.put(record.groupId, record);
        }

        // potentially filter exclusive groups and resolve the group names
        IntMap<GroupName> groupNames = IntMaps.newHashIntMap();
        for (GroupRecord group : loadGroups(rmap.keySet())) {
            if (filter == null || filter.apply(new Tuple<GroupRecord,GroupMembershipRecord>(
                                                   group, rmap.get(group.groupId)))) {
                groupNames.put(group.groupId, group.toGroupName());
            }
        }

        // convert the persistent membership records into runtime records
        List<GroupMembership> groups = Lists.newArrayList();
        for (GroupMembershipRecord record : records) {
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
        List<GroupMembershipRecord> records = getMemberships(memberId);
        IntMap<GroupMembershipRecord> rmap = IntMaps.newHashIntMap();
        for (GroupMembershipRecord record : records) {
            rmap.put(record.groupId, record);
        }

        // potentially filter exclusive groups and resolve the group names
        List<GroupCard> groups = Lists.newArrayList();
        for (GroupRecord group : loadGroups(rmap.keySet())) {
            if (group.policy != Group.POLICY_EXCLUSIVE || includeExclusive) {
                groups.add(group.toGroupCard());
            }
        }
        return groups;
    }

    /**
     * Remove a given person as member of a given group. This method returns false if there was no
     * membership to cancel.
     */
    public boolean leaveGroup (int groupId, int memberId)
    {
        int rows = delete(GroupMembershipRecord.class,
                          GroupMembershipRecord.getKey(memberId, groupId));
        updateMemberCount(groupId);
        _eventLog.groupLeft(memberId, groupId);
        return rows > 0;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public int countMembers (int groupId)
    {
        return load(CountRecord.class,
                    new FromOverride(GroupMembershipRecord.class),
                    new Where(GroupMembershipRecord.GROUP_ID_C, groupId)).count;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public List<GroupMembershipRecord> getMembers (int groupId)
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.GROUP_ID_C, groupId));
    }

    /**
     * Fetches the membership roster of a given group for a given member rank.
     */
    public List<GroupMembershipRecord> getMembers (int groupId, byte rank)
    {
        return getMembers(groupId, rank, false);
    }

    /**
     * Fetches the membership roster of a given group for a given rank or minimum rank.
     * @param minRank If true, return any member with a rank greater than or equal to the rank param
     */
    public List<GroupMembershipRecord> getMembers (int groupId, byte rank, boolean minRank)
    {
        Where where;
        if (minRank) {
            where = new Where(new And(
                new Equals(GroupMembershipRecord.GROUP_ID_C, groupId),
                new GreaterThanEquals(GroupMembershipRecord.RANK_C, rank)));
        }
        else {
            where = new Where(new And(
                new Equals(GroupMembershipRecord.GROUP_ID_C, groupId),
                new Equals(GroupMembershipRecord.RANK_C, rank)));
        }
        return findAll(GroupMembershipRecord.class, where);
    }

    /**
     * Fetches the group memberships a given member belongs to.
     */
    public List<GroupMembershipRecord> getMemberships (int memberId)
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Fetches the full records of the groups a given member belongs to.
     */
    public List<GroupRecord> getFullMemberships (int memberId)
    {
        return findAll(GroupRecord.class,
                       new Join(GroupRecord.GROUP_ID_C, GroupMembershipRecord.GROUP_ID_C),
                       new Where(GroupMembershipRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Load all groups with the official flag set.
     */
    public List<GroupRecord> getOfficialGroups ()
    {
        return findAll(GroupRecord.class, new Where(new Equals(GroupRecord.OFFICIAL_C,
            new LiteralExp("true"))));
    }
    
    /**
     * Sets the home scene id for the given group.
     */
    public void setHomeSceneId (int groupId, int sceneId)
    {
        updatePartial(GroupRecord.class, groupId, GroupRecord.HOME_SCENE_ID, sceneId);
    }

    /**
     * Returns the home scene id for the given group.
     */
    public int getHomeSceneId (int groupId)
    {
        GroupRecord rec = load(GroupRecord.class, groupId);
        if (rec == null) {
            throw new DatabaseException("Group not found in getHomeSceneId! [" + groupId + "]");
        }

        return rec.homeSceneId;
    }

    protected GroupMembershipRecord loadMembership (int groupId, int memberId)
    {
        return load(GroupMembershipRecord.class,
            GroupMembershipRecord.GROUP_ID, groupId, GroupMembershipRecord.MEMBER_ID, memberId);
    }

    protected void updateMemberCount (int groupId)
    {
        Map<String, SQLExpression> fieldMap = Maps.newHashMap();
        fieldMap.put(GroupRecord.MEMBER_COUNT,
                     new SelectClause<CountRecord>(
                         CountRecord.class,
                         new String[] { CountRecord.COUNT },
                         new FromOverride(GroupMembershipRecord.class),
                         new Where(GroupMembershipRecord.GROUP_ID_C, groupId)));
        updateLiteral(GroupRecord.class, groupId, fieldMap);
    }

    /**
     * Return the where clause for a group select based on a given query
     */
    protected Where getGroupWhere (GroupQuery query)
    {
        SQLOperator publicOnly = new Not(new Equals(GroupRecord.POLICY_C, Group.POLICY_EXCLUSIVE));
        if (query.search != null) {
            return new Where(new And(publicOnly, new FullTextMatch(GroupRecord.class,
                GroupRecord.FTS_NBC, query.search)));
        } else if (query.tag != null) {
            TagNameRecord tnr = _tagRepo.getTag(query.tag);
            if (tnr == null) {
                return new Where(new LiteralExp("false"));
            } else {
                return new Where(new And(publicOnly, new Equals(
                    new ColumnExp(GroupTagRecord.class, GroupTagRecord.TAG_ID), tnr.tagId)));
            }
        } else {
            return new Where(publicOnly);
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GroupRecord.class);
        classes.add(GroupMembershipRecord.class);
    }

    /** Used to manage our group tags. */
    protected TagRepository _tagRepo;

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MsoyEventLogger _eventLog;
}
