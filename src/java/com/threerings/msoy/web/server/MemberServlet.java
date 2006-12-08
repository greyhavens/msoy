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
    public Integer getMemberHomeId (WebCreds creds, int memberId)
        throws ServiceException
    {
        ServletWaiter<Integer> waiter =new ServletWaiter<Integer>("getHomeId[" + memberId + "]");
        MsoyServer.memberMan.getHomeId(MsoySceneModel.OWNER_TYPE_MEMBER, memberId, waiter);
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
    public String serializeNeighborhood (WebCreds creds, int memberId)
        throws ServiceException
    {
        ServletWaiter<String> waiter =
            new ServletWaiter<String>("serializeNeighborhood[" + memberId + "]");
        MsoyServer.memberMan.serializeNeighborhood(memberId, waiter);
        return waiter.waitForResult();
    }
}
