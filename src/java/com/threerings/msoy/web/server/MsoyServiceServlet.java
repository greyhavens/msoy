//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.servlet.util.CookieUtil;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

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
     * Returns the member record for the member making this service request, or null if their
     * session has expired, or they are not authenticated.
     */
    protected MemberRecord getAuthedUser ()
        throws ServiceException
    {
        return _mhelper.getAuthedUser(
            CookieUtil.getCookieValue(getThreadLocalRequest(), WebCreds.CREDS_COOKIE));
    }

    /**
     * Returns the member record for the member making this service request.
     *
     * @exception ServiceException thrown if the session has expired or is otherwise invalid.
     */
    protected MemberRecord requireAuthedUser ()
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        if (mrec == null) {
            throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
        }
        return mrec;
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

    @Override // from RemoteServiceServlet
    protected void doUnexpectedFailure (Throwable e)
    {
        HttpServletRequest req = getThreadLocalRequest();
        String path = req.getServletPath();
        if (e instanceof org.mortbay.jetty.EofException) {
            log.info("Servlet response stream unexpectedly closed", "servlet", path);
        } else if (e instanceof IllegalStateException && "STREAM".equals(e.getMessage())) {
            log.info("Servlet response stream unavailable", "servlet", path);
        } else {
            log.warning("Servlet service failure", "servlet", path,
                        (e instanceof UnexpectedException) ? e.getCause() : e);
        }

        // send a generic failure message with 500 status
        try {
            HttpServletResponse rsp = getThreadLocalResponse();
            rsp.setContentType("text/plain");
            rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            rsp.getWriter().write("We are experiencing technical difficults. Eet broke!");
        } catch (IOException ioe) {
            log.warning("Failed writing faiure response", ioe);
        }
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
