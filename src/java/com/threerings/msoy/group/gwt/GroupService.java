//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;
import java.util.Collection;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;

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
        public List<Room> groupRooms;

        /**
         * The rooms owned by the caller.
         */
        public List<Room> callerRooms;
    }

    /** Contains information about one of our rooms. */
    public static class Room implements IsSerializable
    {
        /** The room's scene id. */
        public int sceneId;

        /** The room's name. */
        public String name;

        /** The room's decor thumbnail image. */
        public MediaDesc decor;
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
    public GalaxyData getGalaxyData (WebIdent ident)
        throws ServiceException;

    /**
     * Get the list of all groups.
     */
    public List<GroupCard> getGroupsList (WebIdent ident)
        throws ServiceException;

    /**
     * Performs a search against the name, blurb and charter fields.
     */
    public List<GroupCard> searchGroups (WebIdent ident, String searchString)
        throws ServiceException;

    /**
     * Return all groups that are tagged with the given tag.
     */
    public List<GroupCard> searchForTag (WebIdent ident, String tag)
        throws ServiceException;

    /**
     * Returns information on the specified group.
     */
    public GroupInfo getGroupInfo (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Look up a group by id and return the id of its home scene.
     */
    public Integer getGroupHomeId (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Fetch a list of {@link GroupMembership} records, one for each group of which memberId is a
     * member. If canInvite is true, only include groups to which the member can invite.
     */
    public List<GroupMembership> getMembershipGroups (
        WebIdent ident, int memberId, boolean canInvite)
        throws ServiceException;

    /**
     * Construct a {@link GroupDetail} object for one given group.
     */
    public GroupDetail getGroupDetail (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Returns a list of all the members for the specified group.
     */
    public MembersResult getGroupMembers (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Returns a list of all the rooms owned by a specific group.
     */
    public RoomsResult getGroupRooms (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Transfers a room owned by the caller to the given group.  Only allowed by managers.
     */
    public void transferRoom (WebIdent ident, int groupId, int sceneId)
        throws ServiceException;

    /**
     * Create a new group in the system, with data supplied in the {@link Group} argument.
     */
    public Group createGroup (WebIdent ident, Group group, GroupExtras extras)
        throws ServiceException;

    /**
     * Update the data for a group according to the supplied {@link Group} argument.
     */
    public void updateGroup (WebIdent ident, Group group, GroupExtras extras)
        throws ServiceException;

    /**
     * Sever the membership connection between a group and a member.
     */
    public void leaveGroup (WebIdent ident, int groupId, int memberId)
        throws ServiceException;

    /**
     * Requests that the caller be added as a member of the specified group.
     */
    public void joinGroup (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Update the rank of a group member.
     */
    public void updateMemberRank (WebIdent ident, int groupId, int memberId, byte newRank)
        throws ServiceException;

    /**
     * Update a tag on a group.
     */
    public TagHistory tagGroup (WebIdent ident, int groupId, String tag, boolean set)
        throws ServiceException;

    /**
     * Gets the tags recently used by the user.
     */
    public Collection<TagHistory> getRecentTags (WebIdent ident)
        throws ServiceException;

    /**
     * Gets the tags on the indicated Group.
     */
    public Collection<String> getTags (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Fetch a list of {@link MyGroupCard} records, one for each group of which the caller is a
     * member.
     */
    public List<MyGroupCard> getMyGroups (WebIdent ident, byte sortMethod)
        throws ServiceException;

}
