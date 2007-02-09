//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;
import java.util.Collection;

import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.TagHistory;

/**
 * Defines group services available to the GWT/AJAX web client.
 */
public interface GroupService extends RemoteService
{
    /** 
     * Get the list of all groups.
     */
    public List getGroupsList (WebCreds creds) throws ServiceException;

    /**
     * performs a search against the name, blurb and charter fields.
     */
    public List searchGroups (WebCreds creds, String searchString)
        throws ServiceException;

    /**
     * return all groups that are tagged with the given tag.
     */
    public List searchForTag (WebCreds creds, String tag)
        throws ServiceException;
    
    /**
     * Look up a group by id and return the id of its home scene.
     */
    public Integer getGroupHomeId (WebCreds creds, int groupId)
        throws ServiceException;

    /**
     * Fetch a list of {@link GroupMembership} records, one for each group memberId
     * is a member of. If canInvite is true, only include groups to which the member
     * can invite.
     */
    public List getMembershipGroups (WebCreds creds, int memberId, boolean canInvite)
        throws ServiceException;

    /**
     * Construct a {@link GroupDetail} object for one given group.
     */
    public GroupDetail getGroupDetail (WebCreds creds, int groupId)
        throws ServiceException;

    /**
     * Create a new group in the system, with data supplied in the {@link Group} argument.
     */
    public Group createGroup (WebCreds creds, Group group, GroupExtras extras)
        throws ServiceException;
    
    /**
     * Update the data for a group according to the supplied {@link Group} argument.
     */
    public void updateGroup (WebCreds creds, Group group, GroupExtras extras)
        throws ServiceException;
    
    /**
     * Sever the membership connection between a group and a member.
     */
    public void leaveGroup (WebCreds creds, int groupId, int memberId)
        throws ServiceException;

    /**
     * Create a new membership connection between a group and a member.
     */
    public void joinGroup (WebCreds creds, int groupId, int memberId)
        throws ServiceException;

    /**
     * Update the rank of a group member.
     */
    public void updateMemberRank (WebCreds creds, int groupId, int memberId, byte newRank)
        throws ServiceException;

    /**
     * Update a tag on a group.
     */
    public TagHistory tagGroup (WebCreds creds, int groupId, String tag, boolean set) 
        throws ServiceException;

    /** 
     * Gets the tags recently used by the user.
     */
    public Collection getRecentTags (WebCreds creds)
        throws ServiceException;

    /**
     * Gets the tags on the indicated Group.
     */
    public Collection getTags (WebCreds creds, int groupId)
        throws ServiceException;

    /**
     * Gets the popular tags for groups.  The TagRepository method getPopularTags is not actually
     * using its row parameter... if that changes in the future, this should be changed to make
     * the rows returned configurable in the client.  Also, for some crazy reason, TagRepository is
     * not sorting its results, so we're doing it on the client
     */
    public List getPopularTags (WebCreds creds, int rows)
        throws ServiceException;
}
