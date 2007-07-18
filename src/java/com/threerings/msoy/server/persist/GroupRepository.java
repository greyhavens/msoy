//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.SimpleCacheKey;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.samskivert.util.IntListUtil;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.web.data.Group;

/**
 * Manages the persistent store of group data.
 */
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

    public GroupRepository (ConnectionProvider conprov)
    {
        super(conprov);

        // TEMP
        _ctx.registerMigration(GroupRecord.class, new EntityMigration.Retype(14, "creationDate"));
        // END TEMP


        _tagRepo = new TagRepository(_ctx) {
            protected TagRecord createTagRecord () {
                return new GroupTagRecord();
            }
            protected TagHistoryRecord createTagHistoryRecord () {
                return new GroupTagHistoryRecord();
            }
        };
    }

    public TagRepository getTagRepository ()
    {
        return _tagRepo;
    }

    /**
     * Returns a list of all public and inv-only groups, sorted by size, then by creation time.
     */
    public List<GroupRecord> getGroupsList ()
        throws PersistenceException
    {
        return findAll(GroupRecord.class,
            new Where(new Not(new Equals(GroupRecord.POLICY_C, Group.POLICY_EXCLUSIVE))),
            new OrderBy(
                new SQLExpression[] { GroupRecord.MEMBER_COUNT_C, GroupRecord.CREATION_DATE_C },
                new OrderBy.Order[] { OrderBy.Order.DESC, OrderBy.Order.ASC }));
    }


    /**
     * Searches all public and inv-only groups for the search string against the indexed blurb,
     * charter and name fields.  Results are returned in order of relevance.
     */
    public List<GroupRecord> searchGroups (String searchString)
        throws PersistenceException
    {
        // for now, always operate with boolean searching enabled, without query expansion
        return findAll(GroupRecord.class, new Where(new And(new Not(new Equals(GroupRecord.POLICY_C,
            Group.POLICY_EXCLUSIVE)), new Match(searchString, Match.Mode.BOOLEAN, false,
            GroupRecord.NAME_C, GroupRecord.BLURB_C, GroupRecord.CHARTER_C))));
    }

    /**
     * Searches all groups for the specified tag.  Tagging is not supported on exclusive groups
     */
    public List<GroupRecord> searchForTag (String tag)
        throws PersistenceException
    {
        ArrayList<Integer> groupIds = new ArrayList<Integer>();
        for (GroupTagRecord tagRec : findAll(GroupTagRecord.class,
            new Where(GroupTagRecord.TAG_ID_C, _tagRepo.getOrCreateTag(tag).tagId))) {
            groupIds.add(tagRec.targetId);
        }
        return findAll(GroupRecord.class, new Where(new In(
            GroupRecord.class, GroupRecord.GROUP_ID, groupIds)));
    }

    /**
     * Fetches a single group, by id. Returns null if there's no such group.
     */
    public GroupRecord loadGroup (int groupId)
        throws PersistenceException
    {
        return load(GroupRecord.class, groupId);
    }

    /**
     * Fetches multiple groups by id.
     */
    public List<GroupRecord> loadGroups (int[] groupIds)
        throws PersistenceException
    {
        if (groupIds.length == 0) {
            return Collections.emptyList();
        }
        Comparable[] idArr = IntListUtil.box(groupIds);
        return findAll(GroupRecord.class,
                       new Where(new In(GroupRecord.class, GroupRecord.GROUP_ID, idArr)));
    }

    /**
     * Creates a new group, defined by a {@link GroupRecord}. The key of the record must
     * be null -- it will be filled in through the insertion, and returned.  A blank room is
     * also created that is owned by the group.
     */
    public int createGroup (GroupRecord record)
        throws PersistenceException
    {
        if (record.groupId != 0) {
            throw new PersistenceException(
                "Group record must have a null id for creation " + record);
        }
        record.creationDate = new Date(System.currentTimeMillis());
        insert(record);

        int sceneId = MsoyServer.sceneRepo.createBlankRoom(MsoySceneModel.OWNER_TYPE_GROUP,
            record.groupId, /* TODO */ "Group " + record.name + "'s room", null, true);
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
        throws PersistenceException
    {
        int rows = updatePartial(GroupRecord.class, groupId, fieldValues);
        if (rows == 0) {
            throw new PersistenceException("Couldn't find group for update [id=" + groupId + "]");
        }
    }

    /**
     * Updates the specified group record with supplied field/value mapping.
     */
    public void updateGroup (int groupId, Map<String, Object> updates)
        throws PersistenceException
    {
        int rows = updatePartial(GroupRecord.class, groupId, updates);
        if (rows == 0) {
            throw new PersistenceException("Couldn't find group for update [id=" + groupId + "]");
        }
    }

    /**
     * Deletes the specified group from the repository.  This is generally only done when the
     * last member of a group leaves.
     */
    public void deleteGroup (GroupRecord group)
        throws PersistenceException
    {
        delete(group);
    }

    /**
     * Makes a given person a member of a given group.
     */
    public void joinGroup (int groupId, int memberId, byte rank)
        throws PersistenceException
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
    public void setRank (int groupId, int memberId, byte newRank)
        throws PersistenceException
    {
        Key key = GroupMembershipRecord.getKey(memberId, groupId);
        int rows = updatePartial(
            GroupMembershipRecord.class, key, key,
            GroupMembershipRecord.RANK, newRank,
            GroupMembershipRecord.RANK_ASSIGNED, new Timestamp(System.currentTimeMillis()));
        if (rows == 0) {
            throw new PersistenceException(
                "Couldn't find group membership to modify [groupId=" + groupId +
                "memberId=" + memberId + "]");
        }
    }

    /**
     * Fetches the membership details for a given group and member, or null.
     *
     */
    public GroupMembershipRecord getMembership(int groupId, int memberId)
        throws PersistenceException
    {
        return load(GroupMembershipRecord.class,
                    GroupMembershipRecord.GROUP_ID, groupId,
                    GroupMembershipRecord.MEMBER_ID, memberId);
    }

    /**
     * Remove a given person as member of a given group. This method returns
     * false if there was no membership to cancel.
     */
    public boolean leaveGroup (int groupId, int memberId)
        throws PersistenceException
    {
        Key key = GroupMembershipRecord.getKey(memberId, groupId);
        int rows = deleteAll(GroupMembershipRecord.class, key, key);
        updateMemberCount(groupId);
        return rows > 0;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public int countMembers (int groupId)
        throws PersistenceException
    {
        GroupMembershipCount count =
            load(GroupMembershipCount.class, new Where(GroupMembershipRecord.GROUP_ID_C, groupId),
                 new FieldOverride(GroupMembershipCount.COUNT, "count(*)"),
                 new FromOverride(GroupMembershipRecord.class));
        if (count == null) {
            throw new PersistenceException("Group not found [groupId=" + groupId + "]");
        }
        return count.count;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public List<GroupMembershipRecord> getMembers (int groupId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.GROUP_ID_C, groupId));
    }

    /**
     * Fetches the group memberships a given member belongs to.
     */
    public List<GroupMembershipRecord> getMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Fetches the full records of the groups a given member belongs to.
     */
    public List<GroupRecord> getFullMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(GroupRecord.class,
                       new Join(GroupRecord.GROUP_ID_C, GroupMembershipRecord.GROUP_ID_C),
                       new Where(GroupMembershipRecord.MEMBER_ID_C, memberId));
    }

    protected void updateMemberCount (int groupId)
        throws PersistenceException
    {
        updateLiteral(GroupRecord.class, groupId, "memberCount",
            "(select count(*) from GroupMembershipRecord where groupId=" + groupId + ")");
    }

    /** Used to manage our group tags. */
    protected TagRepository _tagRepo;
}
