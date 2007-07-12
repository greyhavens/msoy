//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides services used by all remote service servlets.
 */
public class MsoyServiceServlet extends RemoteServiceServlet
{
    /**
     * Returns the member record for the supplied ident, or null if the ident represents an expired
     * session or is null.
     */
    public static MemberRecord getAuthedUser (WebIdent ident)
        throws ServiceException
    {
        Integer memberId = (ident == null) ? null : _members.get(ident.token);
        if (memberId != null && memberId == ident.memberId) {
            try {
                return MsoyServer.memberRepo.loadMember(memberId);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to load member [id=" + memberId + "].", pe);
                throw new ServiceException(ServiceException.INTERNAL_ERROR);
            }
        }
        return null;
    }

    /**
     * Looks up the member information associated with the supplied session authentication
     * information.
     *
     * @exception ServiceException thrown if the session has expired or is otherwise invalid.
     */
    public static MemberRecord requireAuthedUser (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);
        if (mrec == null) {
            throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
        }
        return mrec;
    }
    
    /**
     * Returns the member id of the client that provided the supplied ident or -1 if the ident is
     * null. Throws a session expired exception if the ident is expired.
     */
    protected int getMemberId (WebIdent ident)
        throws ServiceException
    {
        if (ident == null) {
            return -1;
        }
        Integer memberId = _members.get(ident.token);
        if (memberId != null) {
            return memberId;
        }
        throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
    }

    /**
     * Called when a user logs on or refreshes their credentials to map the user's record by their
     * session token.
     */
    protected void mapUser (WebCreds creds, MemberRecord record)
    {
        _members.put(creds.token, record.memberId);
    }

    /**
     * A convenience method to record that a user took an action, and potentially award them flow
     * for doing so.
     */
    protected void logUserAction (MemberRecord memrec, UserAction action, String details)
        throws PersistenceException
    {
        MemberFlowRecord flowRec = MsoyServer.memberRepo.getFlowRepository().logUserAction(
            memrec.memberId, action, details);
        if (flowRec != null) {
            MemberManager.queueFlowUpdated(flowRec);
        }
    }

    /** Contains a mapping of authenticated members. */
    protected static Map<String,Integer> _members =
        Collections.synchronizedMap(new HashMap<String,Integer>());
}
