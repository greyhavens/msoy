//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.data.MemberGName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides the server implementation of {@link MemberService}.
 */
public class MemberServlet extends RemoteServiceServlet
    implements MemberService
{
    public MemberGName getName (int memberId)
        throws ServiceException
    {
        ServletWaiter<MemberGName> waiter =
            new ServletWaiter<MemberGName>("getName[" + memberId + "]");
        MsoyServer.memberMan.getName(memberId, waiter);
        return waiter.waitForResult();
    }

    // TODO: wire up
    public void acceptFriend (WebCreds creds, int memberId)
        throws ServiceException
    {
//            ServletWaiter<Void> waiter =
//                new ServletWaiter<Void>("acceptFriend[" + friendId + "]");
//            MsoyServer.memberMan.alterFriend(caller, friendId, true, waiter);
//        return waiter.waitForResult();
    }

    // TODO: wire up
    public void declineFriend (WebCreds creds, int memberId)
        throws ServiceException
    {
//        ServletWaiter<Void> waiter =
//            new ServletWaiter<Void>("acceptFriend[" + friendId + "]");
//        MsoyServer.memberMan.alterFriend(caller, friendId, false, waiter);
//    return waiter.waitForResult();
    }
}
