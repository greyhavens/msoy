//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface GroupServiceAsync
{
    /**
     * The asynchronous version of {@link GroupService#Groups}
     */
    public void getGroups (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.loadGroup} 
     */
    public void getGroupDetail (WebCreds creds, int groupId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.updateGroup} 
     */
    public void updateGroup (WebCreds creds, Group _group, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.createGroup} 
     */
    public void createGroup (WebCreds creds, Group _group, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.removeMember} 
     */
    public void leaveGroup (WebCreds creds, int groupId, int memberId, AsyncCallback callback);
}
