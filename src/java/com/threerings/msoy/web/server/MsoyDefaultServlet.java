//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.samskivert.servlet.util.CookieUtil;

import org.mortbay.jetty.EofException;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.resource.Resource;

import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.server.VisitorCookie;

import static com.threerings.msoy.Log.log;

/**
 *Handles redirecting to our magic version numbered client for embedding and other fiddling that
 * needs doing for static data requests.
 */
public class MsoyDefaultServlet extends DefaultServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        checkCookies(req, rsp); // we want to check cookies even before we do the redirect

        // TODO: handle this for more than just world-client.swf?
        if (req.getRequestURI().equals("/clients/world-client.swf")) {
            rsp.setContentLength(0);
            rsp.sendRedirect("/clients/" + DeploymentConfig.version + "/world-client.swf");
            return;
        }

        if (req.getRequestURI().equals("/")) {
            doPreMainPageGet(req, rsp);
        }
        try {
            super.doGet(req, rsp);
        } catch (EofException eofe) {
            // not a problem, they just closed their end of the connection
        } catch (Exception e) {
            log.warning("Failed to serve defaultness", "uri", req.getRequestURI(), "error", e);
        }
    }

    @Override
    protected void sendDirectory (
        HttpServletRequest req, HttpServletResponse rsp, Resource resource, boolean parent)
        throws IOException
    {
        if ("/clients/".equals(req.getPathInfo())) {
            // we allow directory listings for /clients/ so that Jamie's scripts that run guest
            // clients against production Whirled work
            super.sendDirectory(req, rsp, resource, parent);
        } else {
            // everyone else gets to talk to the hand
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    protected void doPreMainPageGet (HttpServletRequest req, HttpServletResponse rsp)
    {
        // if this user appears to be brand new, create a visitor info for them
        VisitorInfo info = null;
        if (VisitorCookie.shouldCreate(req)) {
            VisitorCookie.set(rsp, info = new VisitorInfo());
            _eventLog.visitorInfoCreated(info, true);
        }

        if (CookieUtil.getCookie(req, CookieNames.WHO) == null) {
            // Give new users all the names and number of groups for tests designated as
            // occurring on landing. The client will compute the group that the user is
            // assigned to when the visitor id is calculated.
            StringBuilder cookieValue = new StringBuilder();
            for (ABTestRecord test : _abTestRepo.loadTestsWithLandingCookies()) {
                test.toCard().flatten(cookieValue);
            }
            rsp.addCookie(new Cookie(CookieNames.LANDING_TEST, cookieValue.toString()));
            log.info("Sending landing cookie", "value", cookieValue);
        }
    }

    protected void checkCookies (HttpServletRequest req, HttpServletResponse rsp)
    {
        // this is a fiddly interaction - the original HTTP Referrer is only available on the very
        // first page access, because once we start redirecting through GWT pages, it'll get
        // overwritten. But we don't have the player's visitor ID available yet. So we squirrel
        // away the referrer in a cookie, and let GWT handle it once it's ready.
        HttpReferrerCookie.check(req, rsp);
    }

    // dependencies
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected ABTestRepository _abTestRepo;
}
