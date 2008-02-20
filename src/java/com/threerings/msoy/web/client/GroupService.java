//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;
import java.util.Collection;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupDetail;
import com.threerings.msoy.group.data.GroupExtras;

import com.threerings.msoy.web.data.GalaxyData;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines group services available to the GWT/AJAX web client.
 */
public interface GroupService extends RemoteService
{
    /**
     * Loads the information displayed on the Galaxy page.
     */
    public GalaxyData getGalaxyData (WebIdent ident)
        throws ServiceException;

    /** 
     * Get the list of all groups.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.GroupCard>
     */
    public List getGroupsList (WebIdent ident)
        throws ServiceException;

    /**
     * Performs a search against the name, blurb and charter fields.
     *
     * @gwt.typeArgs <com.threerings.msoy.group.data.Group>
     */
    public List searchGroups (WebIdent ident, String searchString)
        throws ServiceException;

    /**
     * Return all groups that are tagged with the given tag.
     *
     * @gwt.typeArgs <com.threerings.msoy.group.data.Group>
     */
    public List searchForTag (WebIdent ident, String tag)
        throws ServiceException;
    
    /**
     * Look up a group by id and return the id of its home scene.
     */
    public Integer getGroupHomeId (WebIdent ident, int groupId)
        throws ServiceException;

    /**
     * Fetch a list of {@link GroupMembership} records, one for each group of which memberId is a
     * member. If canInvite is true, only include groups to which the member can invite.
     *
     * @gwt.typeArgs <com.threerings.msoy.group.data.GroupMembership>
     */
    public List getMembershipGroups (WebIdent ident, int memberId, boolean canInvite)
        throws ServiceException;

    /**
     * Construct a {@link GroupDetail} object for one given group.
     */
    public GroupDetail getGroupDetail (WebIdent ident, int groupId)
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
     * Create a new membership connection between a group and a member.
     */
    public void joinGroup (WebIdent ident, int groupId, int memberId)
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
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.TagHistory>
     */
    public Collection getRecentTags (WebIdent ident)
        throws ServiceException;

    /**
     * Gets the tags on the indicated Group.
     *
     * @gwt.typeArgs <java.lang.String>
     */
    public Collection getTags (WebIdent ident, int groupId)
        throws ServiceException;
}
