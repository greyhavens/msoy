//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides the server implementation of {@link PersonService}.
 */
public class PersonServlet extends MsoyServiceServlet
    implements PersonService
{
    // from interface PersonService
    public ArrayList loadBlurbs (final int memberId)
        throws ServiceException
    {
        final ServletWaiter<ArrayList<Object>> waiter =
            new ServletWaiter<ArrayList<Object>>("loadPersonPage[" + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.ppageMan.loadPersonPage(memberId, waiter);
            }
        });
        return waiter.waitForResult();
    }
}
