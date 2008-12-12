//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.TagHistory;

/**
 * Defines group services available to the GWT/AJAX web client.
 */
public interface GroupService extends RemoteService
{
    /** Delivers the response to {@link #getGroupMembers}. */
    public static class MembersResult implements IsSerializable
    {
        /** The group's name and id. */
        public GroupName name;

        /**
         * The members of this group.
         */
        public List<GroupMemberCard> members;
    }

    /** Delivers the response to {@link #getGroupInfo}. */
    public static class GroupInfo implements IsSerializable
    {
        /** The name of the group in question. */
        public GroupName name;

        /** The requester's rank in this group (possibly non-member). */
        public byte rank;
    }

    /** Information needed to fetch a list of groups for the main groups page. */
    public static class GroupQuery implements IsSerializable
    {
        public static final byte SORT_BY_NEW_AND_POPULAR = 1;
        public static final byte SORT_BY_NAME = 2;
        public static final byte SORT_BY_NUM_MEMBERS = 3;
        public static final byte SORT_BY_CREATED_DATE = 4;

        /** The current sort method */
        public byte sort = SORT_BY_NEW_AND_POPULAR;

        /** String to search group name & description for */
        public String search;

        /** Tag to search for; preempted by searchString */
        public String tag;

        public boolean equals (GroupQuery query)
        {
            if (query == null) {
                return false;
            }
            if ((tag == query.tag || (tag != null && tag.equals(query.tag)))
                && (search == query.search || (search != null && search.equals(query.search)))
                && (sort == query.sort)) {
                return true;
            }
            return false;
        }
    }

    /** Delivers the response to {@link #getGroups}. */
    public static class GroupsResult implements IsSerializable
    {
        /** The total number of groups available, will be 0 if the count was not requested. */
        public int totalCount;

        /** The requested slice of the total groups list. */
        public List<GroupCard> groups;
    }

    /** Delivers the response to {@link #getMedals}. */
    public static class MedalsResult implements IsSerializable
    {
        /** The name of the group, used in the UI. */
        public GroupName groupName;

        /** The list of Medals for this group.  We currently do all paging on the client. */
        public List<Medal> medals;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/groupsvc";

    /**
     * Loads the information displayed on the Galaxy page.
     */
    GalaxyData getGalaxyData ()
        throws ServiceException;

    /**
     * Gets a subset of the list of all groups.
     */
    GroupsResult getGroups (int offset, int count, GroupQuery query, boolean needCount)
        throws ServiceException;

    /**
     * Returns information on the specified group.
     */
    GroupInfo getGroupInfo (int groupId)
        throws ServiceException;

    /**
     * Look up a group by id and return the id of its home scene.
     */
    Integer getGroupHomeId (int groupId)
        throws ServiceException;

    /**
     * Fetch a list of {@link GroupMembership} records, one for each group of which memberId is a
     * member. If canInvite is true, only include groups to which the member can invite.
     */
    List<GroupMembership> getMembershipGroups (int memberId, boolean canInvite)
        throws ServiceException;

    /**
     * Construct a {@link GroupDetail} object for one given group.
     */
    GroupDetail getGroupDetail (int groupId)
        throws ServiceException;

    /**
     * Returns a list of all the members for the specified group.
     */
    MembersResult getGroupMembers (int groupId)
        throws ServiceException;

    /**
     * Transfers a room owned by the caller to the given group.  Only allowed by managers.
     */
    void transferRoom (int groupId, int sceneId)
        throws ServiceException;

    /**
     * Create a new group in the system, with data supplied in the {@link Group} argument.
     */
    Group createGroup (Group group, GroupExtras extras)
        throws ServiceException;

    /**
     * Update the data for a group according to the supplied {@link Group} argument.
     */
    void updateGroup (Group group, GroupExtras extras)
        throws ServiceException;

    /**
     * Sever the membership connection between a group and a member.
     */
    void leaveGroup (int groupId, int memberId)
        throws ServiceException;

    /**
     * Requests that the caller be added as a member of the specified group.
     */
    void joinGroup (int groupId)
        throws ServiceException;

    /**
     * Update the rank of a group member.
     */
    void updateMemberRank (int groupId, int memberId, byte newRank)
        throws ServiceException;

    /**
     * Update a tag on a group.
     */
    TagHistory tagGroup (int groupId, String tag, boolean set)
        throws ServiceException;

    /**
     * Gets the tags recently used by the user.
     */
    List<TagHistory> getRecentTags ()
        throws ServiceException;

    /**
     * Gets the tags on the indicated Group.
     */
    List<String> getTags (int groupId)
        throws ServiceException;

    /**
     * Fetch a list of {@link MyGroupCard} records, one for each group of which the caller is a
     * member.
     */
    List<MyGroupCard> getMyGroups (byte sortMethod)
        throws ServiceException;

    /**
     * Fetch a list of {@link GroupMembership} records, one for each group of which the caller
     * is a manager, and which are tied to the given gameId or to no gameId.
     */
    List<GroupMembership> getGameGroups (int gameId)
        throws ServiceException;

    /**
     * Updates the data for a Medal, or creates it if the Medal's medalId is 0 and the name is
     * unique within the set of medals with the same groupId.
     */
    void updateMedal (Medal medal)
        throws ServiceException;

    /**
     * Retreives the set of Medals for this group.
     */
    MedalsResult getMedals (int groupId)
        throws ServiceException;
}
