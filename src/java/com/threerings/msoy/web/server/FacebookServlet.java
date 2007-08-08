//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

import com.samskivert.io.PersistenceException;
import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.facebook.api.FacebookException;
import com.facebook.api.FacebookRestClient;
import com.facebook.api.ProfileField;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.ExternalMapRecord;

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

        // see if they're a user of our Whirled app
        int fbUserId = 0;
        try {
            fbUserId = ParameterUtil.getIntParameter(req, "fb_sig_user", 0, "");
        } catch (Exception e) {
            // no problem, they'll just not be logged in
        }

        // if they're not an app user, clear their auth cookies and send them in as a guest
        if (fbUserId == 0) {
            sendAsGuest(rsp);
            return;
        }

        // if they are an app user and they already have a Whirled session cookie, send them along
        String sessionCreds = CookieUtil.getCookieValue(req, "creds");
        int sessionUserId = 0;
        try {
            sessionUserId = Integer.parseInt(CookieUtil.getCookieValue(req, "fbid"));
        } catch (Exception e) {
            // no problem
        }
        if (sessionUserId == fbUserId && !StringUtil.isBlank(sessionCreds)) {
            log.info("User already has valid creds [fbid=" + sessionUserId +
                     ", creds=" + sessionCreds + "].");
            rsp.sendRedirect("/facebook.html");
            return;
        }

        // we need to talk to facebook; create a client (which will require a valid session key)
        FacebookRestClient fbclient = getFacebookClient(req, fbUserId);
        if (fbclient == null) {
            sendAsGuest(rsp);
            return;
        }

        boolean isAppUser = false;
        try {
            // validate their facebook credentials; this will throw an exception if the supplied
            // user id or session key are invalid; we also care if they have the app added
            isAppUser = fbclient.users_isAppAdded();

        } catch (FacebookException fbe) {
            log.warning("Facebook isAppAdded() failed [fbid=" + fbUserId + ", error=" + fbe + "].");
            sendAsGuest(rsp);
            return;
        }

        try {
            // if this Facebook account is already mapped to a Whirled account, log them in
            int memberId = MsoyServer.memberRepo.lookupExternalAccount(
                ExternalMapRecord.FACEBOOK, String.valueOf(fbUserId));
            if (memberId != 0) {
                sessionCreds = MsoyServer.memberRepo.startOrJoinSession(memberId, FB_SESSION_DAYS);
                sendAsUser(rsp, fbUserId, sessionCreds);
                return;
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to authenticate [id=" + fbUserId + "].", pe);
            sendAsGuest(rsp);
            return;
        }

        // if they have not added the app, just send them in as a guest
        if (!isAppUser) {
            sendAsGuest(rsp);
            return;
        }

        try {
            // look up various information from this user's facebook profile
            ArrayIntSet ids = new ArrayIntSet();
            ids.add(fbUserId);
            EnumSet<ProfileField> fields = EnumSet.of(
                ProfileField.FIRST_NAME, ProfileField.LAST_NAME);
            Document info = fbclient.users_getInfo(ids, fields);
            // TOOD: extract profile information

        } catch (FacebookException fbe) {
            log.warning("Facebook getInfo() failed [fbid=" + fbUserId + ", error=" + fbe + "].");
            sendAsGuest(rsp);
            return;
        }

//         try {
//             // otherwise create them a Whirled account
//             MemberRecord mrec = MsoyServer.author.createAccount(
//                 fbUserId + "@facebook.com", password, "", true, 0);

//             ProfileRecord prec = new ProfileRecord();
//             prec.memberId = mrec.memberId;
//             prec.birthday = new java.sql.Date(birthday.getTime());
//             prec.realName = "";
//             MsoyServer.profileRepo.storeProfile(prec);

//             MsoyServer.memberRepo.noteExternalAccount(
//                 ExternalMapRecord.FACEBOOK, String.valueOf(fbUserId), mrec.memberId);

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "Failed to authenticate [id=" + fbUserId + "].", pe);
//             sendAsGuest(rsp);
//             return;
//         }

        rsp.sendRedirect("/facebook.html");
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got POST request " + req.getRequestURL());
        dumpParameters(req);
    }

    protected void sendAsGuest (HttpServletResponse rsp)
        throws IOException
    {
        CookieUtil.clearCookie(rsp, "creds");
        CookieUtil.clearCookie(rsp, "fbid");
        rsp.sendRedirect("/facebook.html");
    }

    protected void sendAsUser (HttpServletResponse rsp, int fbUserId, String creds)
        throws IOException
    {
        Cookie cookie = new Cookie("creds", creds);
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        rsp.addCookie(cookie);
        cookie = new Cookie("fbid", String.valueOf(fbUserId));
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        rsp.addCookie(cookie);
        rsp.sendRedirect("/facebook.html");
    }

    protected void dumpParameters (HttpServletRequest req)
    {
        Enumeration iter = req.getParameterNames();
        while (iter.hasMoreElements()) {
            String pname = (String)iter.nextElement();
            for (String value : req.getParameterValues(pname)) {
                log.info("  " + pname + " -> " + value);
            }
        }
    }

    protected FacebookRestClient getFacebookClient (HttpServletRequest req, int userId)
    {
        String apiKey = ServerConfig.config.getValue("facebook.api_key", (String)null);
        String secret = ServerConfig.config.getValue("facebook.secret", (String)null);
        if (StringUtil.isBlank(apiKey)) {
            log.warning("Missing facebook.api_key server configuration.");
            return null;
        }
        if (StringUtil.isBlank(secret)) {
            log.warning("Missing facebook.secret server configuration.");
            return null;
        }
        String sessionKey = req.getParameter("fb_sig_session_key");
        if (StringUtil.isBlank(sessionKey)) {
            log.warning("Missing session key [req=" + req.getRequestURL() + "].");
            return null;
        }
        return new FacebookRestClient(apiKey, secret, sessionKey, userId);
    }

    /** The number of days for which to preserve the Facebook auto granted session token. */
    protected static final int FB_SESSION_DAYS = 1;
}
