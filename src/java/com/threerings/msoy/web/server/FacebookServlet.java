//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.schema.FriendsGetResponse;
// import com.google.code.facebookapi.schema.User;
// import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.threerings.msoy.server.FacebookLogic;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ExternalAuther;

import static com.threerings.msoy.Log.log;

/**
 * Handles Facebook callback requests.
 */
public class FacebookServlet extends HttpServlet
{
    protected void doHead (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got HEAD request " + req.getRequestURL());
        dumpParameters(req);
    }

    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got GET request " + req.getRequestURL());
        dumpParameters(req);

        // make sure we have signed facebook data
        if (!validateRequest(req)) {
            rsp.sendRedirect(getLoginURL());
            return;
        }

        // we should either have 'canvas_user' or 'user'
        String fbuid = ParameterUtil.getParameter(req, FBKEY_PREFIX + "canvas_user", "");
        if (StringUtil.isBlank(fbuid)) {
            fbuid = ParameterUtil.getParameter(req, FBKEY_PREFIX + "user", "");
        }
        if (StringUtil.isBlank(fbuid)) {
            rsp.sendRedirect(getLoginURL());
            return;
        }

        // see if this external user already has an account
        int memberId = _memberRepo.lookupExternalAccount(ExternalAuther.FACEBOOK, fbuid);
        if (memberId != 0) {
            // if so, activate a session for them and send them the contents of index.html (we
            // can't redirect here because we're in an iframe and that would redirect the whole
            // enchilada)
            String authtok = _memberRepo.startOrJoinSession(memberId, FBAUTH_DAYS);
            SwizzleServlet.setCookie(req, rsp, authtok);
            rsp.sendRedirect("/facebook.html");
            return;
        }

        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println("Hey, you need newness!");
        } finally {
            StreamUtil.close(out);
        }
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got POST request " + req.getRequestURL());
        dumpParameters(req);
    }

    protected void dumpParameters (HttpServletRequest req)
    {
        for (String pname : ParameterUtil.getParameterNames(req)) {
            for (String value : req.getParameterValues(pname)) {
                log.info("  " + pname + " -> " + value);
            }
        }
    }

    protected boolean validateRequest (HttpServletRequest req)
    {
        String sig = ParameterUtil.getParameter(req, "fb_sig", true);
        if (StringUtil.isBlank(sig)) {
            return false;
        }

        // obtain a list of all fb_sig_ keys and sort them alphabetically by key
        List<String> params = Lists.newArrayList();
        for (String pname : ParameterUtil.getParameterNames(req)) {
            if (pname.startsWith(FBKEY_PREFIX)) {
                params.add(pname.substring(FBKEY_PREFIX.length()) + "=" +
                           req.getParameterValues(pname)[0]);
            }
        }
        Collections.sort(params);

        // concatenate them all together (no separator) and MD5 this plus our secret key
        String sigdata = StringUtil.join(params.toArray(new String[params.size()]), "");
        String secret = ServerConfig.config.getValue("facebook.secret", "");
        return sig.equals(StringUtil.md5hex(sigdata + secret));
    }

    protected static String getLoginURL ()
    {
        return "http://www.facebook.com/login.php?api_key=" +
            ServerConfig.config.getValue("facebook.api_key", "") + "&v=1.0";
    }

    @Inject protected MemberRepository _memberRepo;
    @Inject protected FacebookLogic _faceLogic;

    protected static final String FBKEY_PREFIX = "fb_sig_";
    protected static final int FBAUTH_DAYS = 2;
}
