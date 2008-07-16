//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides services used by all remote service servlets.
 */
public class MsoyServiceServlet extends RemoteServiceServlet
{
    /**
     * A convenience method to record that a user took an action, and potentially award them flow
     * for doing so.
     */
    protected void logUserAction (UserActionDetails info)
        throws PersistenceException
    {
        MemberFlowRecord flowRec = _memberRepo.getFlowRepository().logUserAction(info);
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

    /** Provides useful member related services. */
    @Inject protected MemberHelper _mhelper;

    /** Provides access to persistent member information. */
    @Inject protected MemberRepository _memberRepo;

    /** Used to log interesting events for later grindage. */
    @Inject protected MsoyEventLogger _eventLog;
}
