//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.data.ServiceException;

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
     * Posts a runnable to be executed on the dobjmgr thread and returns immediately.
     */
    protected void postDObjectAction (Runnable runnable)
    {
        _omgr.postRunnable(runnable);
    }

    /**
     * Posts a runnable to be executed on the dobjmgr thread and blocks waiting for the result.
     */
    protected <T> T runDObjectAction (String name, final DOAction<T> action)
        throws ServiceException
    {
        final ServletWaiter<T> waiter = new ServletWaiter<T>(name);
        _omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    waiter.postSuccess(action.run());
                } catch (Exception e) {
                    waiter.postFailure(e);
                }
            }
        });
        return waiter.waitForResult();
    }

    /**
     * Returns null if mrec is null {@link MemberRecord#who} otherwise.
     */
    protected static String who (MemberRecord mrec)
    {
        return (mrec == null) ? null : mrec.who();
    }

    /** Used by {@link #runDObjectAction}. */
    protected static interface DOAction<T> {
        /** Invoked on the dobjmgr thread. */
        public T run () throws Exception;
    }

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MemberHelper _mhelper;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _eventLog;
}
