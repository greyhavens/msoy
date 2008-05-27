//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
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
     * session, a guest or is null.
     */
    public static MemberRecord getAuthedUser (WebIdent ident)
        throws ServiceException
    {
        if (ident == null || MemberName.isGuest(ident.memberId)) {
            return null;
        }

        // if we don't have a session token -> member id mapping, then...
        Integer memberId = ServletUtil.getMemberId(ident.token);
        if (memberId == null) {
            // ...try looking up this session token, they may have originally authenticated with
            // another server and then started talking to us
            try {
                MemberRecord mrec = MsoyServer.memberRepo.loadMemberForSession(ident.token);
                if (mrec == null || mrec.memberId != ident.memberId) {
                    return null;
                }
                ServletUtil.mapMemberId(ident.token, mrec.memberId);
                return mrec;
            } catch (PersistenceException pe) {
                log.warning("Failed to load session [tok=" + ident.token + "].", pe);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        // otherwise we already have a valid session token -> member id mapping, so use it
        if (memberId == ident.memberId) {
            try {
                return MsoyServer.memberRepo.loadMember(memberId);
            } catch (PersistenceException pe) {
                log.warning("Failed to load member [id=" + memberId + "].", pe);
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
     * Initializes this servlet.
     */
    public void init (MsoyEventLogger eventLog)
    {
        _eventLog = eventLog;
    }

    /**
     * A convenience method to record that a user took an action, and potentially award them flow
     * for doing so.
     */
    protected void logUserAction (UserActionDetails info)
        throws PersistenceException
    {
        MemberFlowRecord flowRec = MsoyServer.memberRepo.getFlowRepository().logUserAction(info);
        if (flowRec != null) {
            MemberNodeActions.flowUpdated(flowRec);
        }
    }

    /**
     * Returns null if mrec is null {@link MemberRecord#who} otherwise.
     */
    protected static String who (MemberRecord mrec)
    {
        return (mrec == null) ? null : mrec.who();
    }

    /** Used to log interesting events for later grindage. */
    protected MsoyEventLogger _eventLog;
}
