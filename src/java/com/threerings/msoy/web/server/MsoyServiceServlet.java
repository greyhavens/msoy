//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.inject.Inject;
import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.presents.dobj.RootDObjectManager;

import static com.threerings.msoy.Log.log;

/**
 * Provides services used by all remote service servlets.
 */
public class MsoyServiceServlet extends RemoteServiceServlet
{
    /**
     * Returns the member record for the member making this service request, or null if their
     * session has expired, or they are not authenticated.
     */
    protected MemberRecord getAuthedUser ()
        throws ServiceException
    {
        return _mhelper.getAuthedUser(
            CookieUtil.getCookieValue(getThreadLocalRequest(), WebCreds.credsCookie()));
    }

    /**
     * Returns the visitor ID of the player making this service request, either by asking
     * the MemberHelper (for registered players), or by reading directly from the cookie
     * (for guests). Returns null if it can't find one.
     */
    protected String getVisitorTracker ()
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        if (mrec != null) {
            return mrec.visitorId;
        }

        VisitorInfo info = VisitorCookie.get(getThreadLocalRequest());
        return (info != null) ? info.id : null;
    }

    /**
     * Returns the member record for the member making this service request.
     *
     * @exception ServiceException thrown if the session has expired or is otherwise invalid.
     */
    protected MemberRecord requireAuthedUser ()
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser();
        if (mrec == null) {
            throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
        }

        // User SWFs can send HTTP requests posing as the GWT client and potentially cause 50 kinds
        // of havoc. These requests come from Flash with the same cookies and User-Agent as a legit
        // browser request. However, there is no Referer, and Flash 9 doesn't allow you to spoof
        // one. So check that the Referrer is present, and for good measure, make sure it's coming
        // from on-site.
        String referer = getThreadLocalRequest().getHeader("Referer");
        if (referer == null || !referer.startsWith(DeploymentConfig.serverURL)) {
            log.info("Rejected request that contained invalid referer.", "referer", referer);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return mrec;
    }

    /**
     * Posts a runnable to be executed on the dobjmgr thread and returns immediately.
     */
    protected void postDObjectAction (final Runnable runnable)
    {
        _omgr.postRunnable(runnable);
    }

    /**
     * Posts a runnable to be executed on the dobjmgr thread and blocks waiting for the result.
     */
    protected <T> T runDObjectAction (final String name, final DOAction<T> action)
        throws ServiceException
    {
        final ServletWaiter<T> waiter = new ServletWaiter<T>(name);
        _omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    waiter.postSuccess(action.run());
                } catch (final Exception e) {
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
            return; // no need to write a 500 response, they closed us
        } else if (e instanceof IllegalStateException && "STREAM".equals(e.getMessage())) {
            log.info("Servlet response stream unavailable", "servlet", path);
            return; // no need to write a 500 response, our output stream is hosed
        }

        log.warning("Servlet service failure", "servlet", path,
                    (e instanceof UnexpectedException) ? e.getCause() : e);

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
    protected static String who (final MemberRecord mrec)
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
