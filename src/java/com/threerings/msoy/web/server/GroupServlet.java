//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class GroupServlet extends RemoteServiceServlet
    implements GroupService
{
    // from interface GroupService
    public GroupDetail getGroupDetail (WebCreds creds, int groupId)
        throws ServiceException
    {
        ServletWaiter<GroupDetail> waiter =
            new ServletWaiter<GroupDetail>("getGroupDetail[" + groupId + "]");
        MsoyServer.memberMan.getGroupDetail(groupId, waiter);
        return waiter.waitForResult();
    }

    // from interface GroupService
    public List<Group> getGroups (WebCreds creds)
        throws ServiceException
    {
        ServletWaiter<List<Group>> waiter = new ServletWaiter<List<Group>>("getGroups[]");
        MsoyServer.memberMan.getGroups(waiter);
        return waiter.waitForResult();
    }

    // from interface GroupService
    public Group createGroup (WebCreds creds, Group group) throws ServiceException
    {
        ServletWaiter<Group> waiter = new ServletWaiter<Group>("createGroup[" + group + "]");
        group.creationDate = new Date(System.currentTimeMillis());
        group.creatorId = creds.memberId;
        MsoyServer.memberMan.createGroup(group, waiter);
        return waiter.waitForResult();
    }

    // from interface GroupService
    public void updateGroup (WebCreds creds, Group group) throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>("createGroup[" + group + "]");
        MsoyServer.memberMan.updateGroup(group.groupId, group.name, group.charter,
                                         group.logo, group.policy, waiter);
        waiter.waitForResult();
    }

    public void leaveGroup (WebCreds creds, int groupId, int memberId) throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>(
                "createGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.memberMan.leaveGroup(groupId, memberId, waiter);
        waiter.waitForResult();

    }
}
