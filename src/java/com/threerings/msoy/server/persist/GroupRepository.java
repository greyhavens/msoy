//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntSet;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Manages the persistent store of group data.
 */
public class GroupRepository extends DepotRepository
{
    public GroupRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Fetches all groups. This will be a pager soon.
     */
    public Collection<GroupRecord> findGroups ()
        throws PersistenceException
    {
        return findAll(GroupRecord.class);
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
    public Collection<GroupRecord> loadGroups (int[] groupIds)
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
        insert(record);

        int sceneId = MsoyServer.sceneRepo.createBlankRoom(MsoySceneModel.OWNER_TYPE_GROUP, 
            record.groupId, /* TODO */ "Group " + record.name + "'s room");
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
            throw new PersistenceException(
                "Couldn't find group to modify [groupId=" + groupId + "]");
        }
    }
    
    /**
     * Makes a given person a member of a given group.
     */

    public GroupMembershipRecord joinGroup (int groupId, int memberId, byte rank)
        throws PersistenceException
    {
        GroupMembershipRecord record = new GroupMembershipRecord();
        record.groupId = groupId;
        record.memberId = memberId;
        record.rank = rank;
        insert(record);
        return record;
    }
    
    /**
     * Sets the rank of a member of a group.
     */
    public void setRank (int groupId, int memberId, byte newRank)
        throws PersistenceException
    {
        int rows = updatePartial(GroupMembershipRecord.class,
                                 new Key(GroupMembershipRecord.GROUP_ID, groupId,
                                         GroupMembershipRecord.MEMBER_ID, memberId),
                                 GroupMembershipRecord.RANK, newRank);
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
                    new Key(GroupMembershipRecord.GROUP_ID, groupId,
                            GroupMembershipRecord.MEMBER_ID, memberId));
    }

    /**
     * Remove a given person as member of a given group. This method returns
     * false if there was no membership to cancel.
     */
    public boolean leaveGroup (int groupId, int memberId)
        throws PersistenceException
    {
        int rows = deleteAll(GroupMembershipRecord.class,
                             new Key(GroupMembershipRecord.GROUP_ID, groupId,
                                     GroupMembershipRecord.MEMBER_ID, memberId));
        return rows > 0;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public int countMembers (int groupId)
        throws PersistenceException
    {
        GroupMembershipCount count =
            load(GroupMembershipCount.class,
                 new FieldOverride(GroupMembershipCount.COUNT, "count(*)"),
                 new Key(GroupMembershipRecord.GROUP_ID, groupId),
                 new FromOverride(GroupMembershipRecord.class));
        if (count == null) {
            throw new PersistenceException("Group not found [groupId=" + groupId + "]");
        }
        return count.count;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public Collection<GroupMembershipRecord> getMembers (int groupId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Key(GroupMembershipRecord.GROUP_ID, groupId));
    }

    /**
     * Fetches the group memberships a given member belongs to.
     */
    public Collection<GroupMembershipRecord> getMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Key(GroupMembershipRecord.MEMBER_ID, memberId));
    }
}
