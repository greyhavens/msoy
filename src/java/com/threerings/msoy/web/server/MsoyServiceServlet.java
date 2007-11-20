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

import com.threerings.msoy.web.data.ServiceCodes;
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
        if (ident == null) {
            return null;
        }

        // if we don't have a session token -> member id mapping, then...
        Integer memberId = _members.get(ident.token);
        if (memberId == null) {
            // ...try looking up this session token, they may have originally authenticated with
            // another server and then started talking to us
            try {
                MemberRecord mrec = MsoyServer.memberRepo.loadMemberForSession(ident.token);
                if (mrec == null || mrec.memberId != ident.memberId) {
                    return null;
                }
                mapUser(ident.token, mrec);
                return mrec;
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to load session [tok=" + ident.token + "].", pe);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        // otherwise we already have a valid session token -> member id mapping, so use it
        if (memberId == ident.memberId) {
            try {
                return MsoyServer.memberRepo.loadMember(memberId);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to load member [id=" + memberId + "].", pe);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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

    /**
     * Called when a user logs on or refreshes their credentials to map the user's record by their
     * session token.
     */
    protected static void mapUser (String ident, MemberRecord record)
    {
        _members.put(ident, record.memberId);
    }

    /**
     * Returns null if mrec is null {@link MemberRecord#who} otherwise.
     */
    protected static String who (MemberRecord mrec)
    {
        return (mrec == null) ? null : mrec.who();
    }

    /** Contains a mapping of authenticated members. */
    protected static Map<String,Integer> _members = Collections.synchronizedMap(
        new HashMap<String,Integer>());
}
