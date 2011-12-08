//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import org.mortbay.jetty.EofException;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.resource.Resource;

import com.samskivert.util.StringUtil;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.admin.server.ABTestLogic;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.web.gwt.ABTestCard;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.server.VisitorCookie;

import static com.threerings.msoy.Log.log;

/**
 * Handles redirecting to our magic version numbered client for embedding and other fiddling that
 * needs doing for static data requests.
 */
public class MsoyDefaultServlet extends DefaultServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        String uri = req.getRequestURI();

        if ("/clients/world-client.swf".equals(uri)) {
            // TODO: handle this for more than just world-client.swf?
            rsp.setContentLength(0);
            rsp.sendRedirect("/clients/" + DeploymentConfig.version + "/world-client.swf");
            return;
        }

        // add the privacy header so we can set some cookies in an iframe
        MsoyHttpServer.addPrivacyHeader(rsp);

        if ("/index.html".equals(uri)) {
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
        if (VisitorCookie.shouldCreate(req)) {
            VisitorInfo info = VisitorCookie.createAndSet(rsp);
            // we can't get the anchor from the URL (AFAIK the client doens't even send it) so we
            // have to lump all non-/welcome-nor-/go landings into a single vector

            // note: the HTTP referrer field is spelled 'Referer', sigh (thanks Bruno).
            _memberLogic.noteNewVisitor(info, true, "page.default", req.getHeader("Referer"), 0);

            rsp.addCookie(new Cookie(CookieNames.NEED_GWT_VECTOR, "t"));;

            // DEBUG
            String path = StringUtil.deNull(req.getPathInfo());
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            // note: the HTTP referrer field is spelled 'Referer', sigh (thanks Bruno).
            log.info("VisitorInfo created", "info", info, "reason", "MsoyDefaultServlet", "path",
                path, "agent", req.getHeader("User-Agent"), "ref", req.getHeader("Referer"),
                "addr", req.getRemoteAddr());
        }

        if (CookieUtil.getCookie(req, CookieNames.WHO) == null) {
            // Give new users all the names and number of groups for tests designated as
            // occurring on landing. The client will compute the group that the user is
            // assigned to when the visitor id is calculated.
            StringBuilder cookie = new StringBuilder();
            for (ABTestCard test : _testLogic.getTestsWithLandingCookies()) {
                test.flatten(cookie);
            }
            if (cookie.length() > 0) {
                rsp.addCookie(new Cookie(CookieNames.LANDING_TEST, cookie.toString()));
                log.info("Sending landing cookie", "value", cookie);
            }
        }

        log.info("Hello there index.html");
    }

    // dependencies
    @Inject protected ABTestLogic _testLogic;
    @Inject protected MemberLogic _memberLogic;
}
