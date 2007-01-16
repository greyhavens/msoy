//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.SimpleCacheKey;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.util.IntListUtil;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.web.data.Group;

/**
 * Manages the persistent store of group data.
 */
public class GroupRepository extends DepotRepository
{
    public GroupRepository (ConnectionProvider conprov)
    {
        super(conprov);
        _ctx.addCacheListener(GroupRecord.class, new CacheListener<GroupRecord>() {
            public void entryModified (CacheKey key, GroupRecord entry) {
                // invalidate group names on any group record modifications
                _ctx.cacheInvalidate(_groupNamePrefixKey);
            }
        });
    }

    /**
     * Fetches all groups whose name starts with the given character.
     */
    public Collection<GroupRecord> findGroups (String startingCharacter)
        throws PersistenceException
    {
        return findAll(GroupRecord.class,
            new Where(new Equals(new LiteralExp("substring(name,1,1)"), startingCharacter)));
    }

    /**
     * Searches all groups for the search string against the indexed blurb, charter and name 
     * fields.  Results are returned in order of relevance.
     */
    public Collection<GroupRecord> searchGroups (String searchString) 
        throws PersistenceException
    {
        // for now, always operate with boolean searching enabled, without query expansion
        return findAll(GroupRecord.class, 
            new Where(new Match(searchString, Match.Mode.BOOLEAN, false, "name", "blurb", 
            "charter")));
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

    public GroupMembershipRecord joinGroup (int groupId, int memberId, byte rank)
        throws PersistenceException
    {
        GroupMembershipRecord record = new GroupMembershipRecord();
        record.groupId = groupId;
        record.memberId = memberId;
        record.rank = rank;
        insert(record);
        updateMemberCount(groupId);
        return record;
    }
    
    /**
     * Sets the rank of a member of a group.
     */
    public void setRank (int groupId, int memberId, byte newRank)
        throws PersistenceException
    {
        Key<GroupMembershipRecord> key = new Key<GroupMembershipRecord>(
                GroupMembershipRecord.class,
                GroupMembershipRecord.GROUP_ID, groupId,
                GroupMembershipRecord.MEMBER_ID, memberId);
        int rows = updatePartial(GroupMembershipRecord.class, key, key,
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
        Key<GroupMembershipRecord> key = new Key<GroupMembershipRecord>(
                GroupMembershipRecord.class,
                GroupMembershipRecord.GROUP_ID, groupId,
                GroupMembershipRecord.MEMBER_ID, memberId);
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
            load(GroupMembershipCount.class, GroupMembershipRecord.GROUP_ID, groupId,
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
    public Collection<GroupMembershipRecord> getMembers (int groupId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.GROUP_ID, groupId));
    }

    /**
     * Fetches the group memberships a given member belongs to.
     */
    public Collection<GroupMembershipRecord> getMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.MEMBER_ID, memberId));
    }

    /**
     * Fetches a list of the characters that start group names.  This method returns a 
     * List&lt;String&gt; instead of List&lt;Character&gt; because in GWT, java.lang.Character
     * is not Comparable.
     */
    public List<String> getCharacters () 
        throws PersistenceException
    {
        // force the creation of a GroupRecord table if necessary
        _ctx.getMarshaller(GroupRecord.class);
    
        return _ctx.invoke(new CollectionQuery<List<String>>(_groupNamePrefixKey) {
            public List<String> invoke (Connection conn)
                throws SQLException
            {
                String query = "select substring(name,1,1) as letter from GroupRecord " +
                    "where policy=" + Group.POLICY_PUBLIC + " group by letter";
                ArrayList<String> characters = new ArrayList<String>();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        characters.add(rs.getString(1));
                    }
                    return characters;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }

            // from Query
            public List<String> transformCacheHit (CacheKey key, List<String> value) {
                // no need to clone this result
                return value;
            }
        });
    }

    protected void updateMemberCount (int groupId) 
        throws PersistenceException
    {
        updateLiteral(GroupRecord.class, groupId, "memberCount", 
            "(select count(*) from GroupMembershipRecord where groupId=" + groupId + ")");
    }

    protected CacheKey _groupNamePrefixKey = new SimpleCacheKey(GROUP_NAME_PREFIX);

    protected static final String GROUP_NAME_PREFIX = "GroupNamePrefix";
}
