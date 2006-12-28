//
// $Id$

package com.threerings.msoy.web.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link WebUserService}.
 */
public class WebUserServlet extends MsoyServiceServlet
    implements WebUserService
{
    // from interface WebUserService
    public WebCreds login (String username, String password, int expireDays)
        throws ServiceException
    {
        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
        MemberRecord mrec = auth.authenticateSession(username, password);

        try {
            // if they made it through that gauntlet, create or update their session token
            WebCreds creds = new WebCreds();
            creds.memberId = mrec.memberId;
            creds.token = MsoyServer.memberRepo.startOrJoinSession(mrec.memberId, expireDays);
            mapUser(creds, mrec);
            return creds;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to start session for [username=" + username + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    // from interface WebUserService
    public WebCreds validateSession (String authtok, int expireDays)
        throws ServiceException
    {
        // refresh the token associated with their authentication session
        try {
            MemberRecord mrec = MsoyServer.memberRepo.refreshSession(authtok, expireDays);
            if (mrec == null) {
                return null;
            }

            WebCreds creds = new WebCreds();
            creds.token = authtok;
            creds.memberId = mrec.memberId;
            mapUser(creds, mrec);
            return creds;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to refresh session [tok=" + authtok + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }
}
