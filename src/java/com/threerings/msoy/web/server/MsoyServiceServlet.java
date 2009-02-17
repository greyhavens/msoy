//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
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
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;

import com.threerings.presents.dobj.RootDObjectManager;

import static com.threerings.msoy.Log.log;

/**
 * Provides services used by all remote service servlets.
 */
public class MsoyServiceServlet extends RemoteServiceServlet
{
    @Override // from RemoteServiceServlet
    protected void onBeforeRequestDeserialized (String payload)
    {
        super.onBeforeRequestDeserialized(payload);
        if (PROFILING_ENABLED) {
            RPCRequest req = RPC.decodeRequest(payload, this.getClass(), this);
            _profiler.enter(req.getMethod().getName());
        }
    }

    @Override // from RemoteServiceServlet
    protected void onAfterResponseSerialized (String payload)
    {
        if (PROFILING_ENABLED) {
            _profiler.exitAndClear(null);
        }
        super.onAfterResponseSerialized(payload);
    }

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
        if (CHECK_REFERRER) {
            String referer = getThreadLocalRequest().getHeader("Referer");
            if (referer == null || !referer.startsWith(DeploymentConfig.serverURL)) {
                log.info("Rejected request that contained invalid referer.", "referer", referer);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        return mrec;
    }

    /**
     * Returns the member record for the member making this request, requiring that they have a
     * validated email address.
     */
    protected MemberRecord requireValidatedUser ()
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        if (!memrec.isValidated()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }
        return memrec;
    }

    /**
     * Returns the member record for the member making this request, requiring that they
     * be <b>support</b> or higher.
     */
    protected MemberRecord requireSupportUser ()
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        if (!memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }
        return memrec;
    }

    /**
     * Returns the member record for the member making this request, requiring that they
     * be <b>admin</b> or higher.
     */
    protected MemberRecord requireAdminUser ()
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }
        return memrec;
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
     * Posts a runnable to be executed on the dobjmgr thread and returns immediately.
     */
    protected void postDObjectAction (final Runnable runnable)
    {
        _omgr.postRunnable(runnable);
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

        String errmsg;
        if (e instanceof IncompatibleRemoteServiceException) {
            log.info("Rejecting out of date client", "servlet", path);
            errmsg = "This application is out of date, please click the refresh button on " +
                "your browser.";
        } else {
            log.warning("Servlet service failure", "servlet", path,
                        (e instanceof UnexpectedException) ? e.getCause() : e);
            errmsg = "We are experiencing technical difficults. Eet broke!";
        }

        // send a generic failure message with 500 status
        try {
            HttpServletResponse rsp = getThreadLocalResponse();
            rsp.setContentType("text/plain");
            rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            rsp.getWriter().write(errmsg);
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

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MemberHelper _mhelper;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected RPCProfiler _profiler;

    /** Whether or not RPC profiling is enabled. */
    protected static final boolean PROFILING_ENABLED = true;

    /** Whether or not to skip our referrer check. The GWT shell always reports a null referrer, so
     * we need to skip the check when we're testing with the shell. */
    protected static final boolean CHECK_REFERRER = !Boolean.getBoolean("skip_check_referrer");
}
