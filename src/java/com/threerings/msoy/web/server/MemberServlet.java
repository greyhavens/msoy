//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.Neighborhood;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Provides the server implementation of {@link MemberService}.
 */
public class MemberServlet extends RemoteServiceServlet
    implements MemberService
{
    // from MemberService
    public MemberName getName (int memberId)
        throws ServiceException
    {
        ServletWaiter<MemberName> waiter =
            new ServletWaiter<MemberName>("getName[" + memberId + "]");
        MsoyServer.memberMan.getName(memberId, waiter);
        return waiter.waitForResult();
    }

    // from MemberService
    public Integer getMemberHomeId (int memberId)
        throws ServiceException
    {
        ServletWaiter<Integer> waiter =new ServletWaiter<Integer>("getHomeId[" + memberId + "]");
        MsoyServer.memberMan.getHomeId(MsoySceneModel.OWNER_TYPE_MEMBER, memberId, waiter);
        return waiter.waitForResult();
    }

    // from MemberService
    public Integer getGroupHomeId (int groupId)
        throws ServiceException
    {
        ServletWaiter<Integer> waiter =new ServletWaiter<Integer>("getHomeId[" + groupId + "]");
        MsoyServer.memberMan.getHomeId(MsoySceneModel.OWNER_TYPE_GROUP, groupId, waiter);
        return waiter.waitForResult();
    }

    // from MemberService
    public void inviteFriend (WebCreds creds, int friendId)
        throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>("inviteFriend[" + friendId + "]");
        MsoyServer.memberMan.alterFriend(creds.memberId, friendId, true, waiter);
        waiter.waitForResult();
    }

    // from MemberService
    public void acceptFriend (WebCreds creds, int friendId)
        throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>("acceptFriend[" + friendId + "]");
        MsoyServer.memberMan.alterFriend(creds.memberId, friendId, true, waiter);
        waiter.waitForResult();
    }

    // from MemberService
    public void declineFriend (WebCreds creds, int friendId)
        throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>("declineFriend[" + friendId + "]");
        MsoyServer.memberMan.alterFriend(creds.memberId, friendId, false, waiter);
        waiter.waitForResult();
    }
    
    // from MemberService
    public Neighborhood getNeighborhood (WebCreds creds, int memberId)
        throws ServiceException
    {
        ServletWaiter<Neighborhood> waiter =
            new ServletWaiter<Neighborhood>("getNeighborhood[" + memberId + "]");
        MsoyServer.memberMan.getNeighborhood(memberId, waiter);
        return waiter.waitForResult();
    }
}
