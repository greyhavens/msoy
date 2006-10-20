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
 * Defines "person page" services available to the GWT/AJAX web client.
 */
public interface GroupService extends RemoteService
{
    public List getGroups (WebCreds creds)
        throws ServiceException;
    
    public GroupDetail getGroupDetail (WebCreds creds, int groupId)
        throws ServiceException;

    public Group createGroup (WebCreds creds, Group group)
        throws ServiceException;
    
    public void updateGroup (WebCreds creds, Group group)
        throws ServiceException;
    
    public void leaveGroup (WebCreds creds, int groupId, int memberId)
        throws ServiceException;
}
