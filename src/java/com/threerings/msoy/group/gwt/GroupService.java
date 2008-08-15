//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.GroupName;

import com.threerings.msoy.room.gwt.RoomInfo;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;

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

    /** Delivers the respose to {@link #getGroupRooms}. */
    public static class RoomsResult implements IsSerializable
    {
        /**
         * The rooms of this group.
         */
        public List<RoomInfo> groupRooms;

        /**
         * The rooms owned by the caller.
         */
        public List<RoomInfo> callerRooms;
    }

    /** Delivers the response to {@link #getGroupInfo}. */
    public static class GroupInfo implements IsSerializable
    {
        /** The name of the group in question. */
        public GroupName name;

        /** The requester's rank in this group (possibly non-member). */
        public byte rank;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/groupsvc";

    /**
     * Loads the information displayed on the Galaxy page.
     */
    GalaxyData getGalaxyData ()
        throws ServiceException;

    /**
     * Get the list of all groups.
     */
    List<GroupCard> getGroupsList ()
        throws ServiceException;

    /**
     * Performs a search against the name, blurb and charter fields.
     */
    List<GroupCard> searchGroups (String searchString)
        throws ServiceException;

    /**
     * Return all groups that are tagged with the given tag.
     */
    List<GroupCard> searchForTag (String tag)
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
     * Returns a list of all the rooms owned by a specific group.
     */
    RoomsResult getGroupRooms (int groupId)
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

}
