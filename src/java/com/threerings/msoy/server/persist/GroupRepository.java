//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;

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
     * Fetch a single group, by id. Returns null if there's no such group.
     */
    public GroupRecord loadGroup (int groupId)
        throws PersistenceException
    {
        return load(GroupRecord.class, groupId);
    }

    /**
     * Create a new group, defined by a {@link GroupRecord}. The key of the record must
     * be null -- it will be filled in through the insertion, and returned.
     */
    public int createGroup (GroupRecord record)
        throws PersistenceException
    {
        if (record.groupId != 0) {
            throw new PersistenceException(
                "Group record must have a null id for creation " + record);
        }
        insert(record);
        return record.groupId;
    }

    /**
     * Modify the specified group record with field/value pairs, e.g.
     *     modifyGroup(groupId,
     *                 GroupRecord.CHARTER, newCharter,
     *                 GroupRecord.POLICY, Group.EXCLUSIVE);
     */
    public void modifyGroup (int groupId, Object... fieldValues)
        throws PersistenceException
    {
        int rows = updatePartial(GroupRecord.class, groupId, fieldValues);
        if (rows == 0) {
            throw new PersistenceException(
                "Couldn't find group to modify [groupId=" + groupId + "]");
        }
    }
    
    /**
     * Make a given person a member of a given group.
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
     * Set the rank of a member of a group.
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
     * Fetch the membership details for a given group and member, or null.
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
     * Fetch the membership roster of a given group.
     */
    public Collection<GroupMembershipRecord> getMembers (int groupId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Key(GroupMembershipRecord.GROUP_ID, groupId));
    }

    /**
     * Fetch the groups a given member belongs to.
     */
    public Collection<GroupMembershipRecord> getGroups (int memberId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Key(GroupMembershipRecord.MEMBER_ID, memberId));
    }
}
