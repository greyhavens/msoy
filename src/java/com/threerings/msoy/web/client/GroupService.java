//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines group services available to the GWT/AJAX web client.
 */
public interface GroupService extends RemoteService
{
    /**
     * Fetch all the groups in the system. Will need to be a pager sooner or later.
     */
    public List getGroups (WebCreds creds)
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
    public Group createGroup (WebCreds creds, Group group)
        throws ServiceException;
    
    /**
     * Update the data for a group according to the supplied {@link Group} argument.
     */
    public void updateGroup (WebCreds creds, Group group)
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
}
