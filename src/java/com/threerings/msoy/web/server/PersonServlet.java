//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.data.PersonLayout;
import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides the server implementation of {@link PersonService}.
 */
public class PersonServlet extends RemoteServiceServlet
    implements PersonService
{
    // from interface PersonService
    public ArrayList loadBlurbs (int memberId)
        throws ServiceException
    {
        // load up their bits
        ServletWaiter<ArrayList<Object>> waiter =
            new ServletWaiter<ArrayList<Object>>(
                "loadPersonPage[" + memberId + "]");
        MsoyServer.ppageMan.loadPersonPage(memberId, waiter);
        return waiter.waitForResult();
    }
}
