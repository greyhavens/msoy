//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.logging.Level;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.samskivert.io.PersistenceException;
import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.facebook.api.FacebookException;
import com.facebook.api.FacebookRestClient;
import com.facebook.api.ProfileField;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.ServiceException;

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
        // dumpParameters(req);

        Tuple<Integer,String> creds = processAuth(req, rsp);
        if (creds == null) {
            log.info("Admitting guest user.");
            for (String name : new String[] { "creds", "fbid" }) {
                Cookie cookie = new Cookie(name, "x");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                CookieUtil.widenDomain(req, cookie);
                rsp.addCookie(cookie);
            }

        } else {
            log.info("Admitting authenticated user " + creds + ".");
            Cookie cookie = new Cookie("creds", creds.right);
            cookie.setPath("/");
            cookie.setMaxAge(-1);
            rsp.addCookie(cookie);

            cookie = new Cookie("fbid", String.valueOf(creds.left));
            cookie.setPath("/");
            cookie.setMaxAge(-1);
            rsp.addCookie(cookie);
        }

        rsp.sendRedirect("/facebook.html");
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got POST request " + req.getRequestURL());
        dumpParameters(req);
    }

    protected Tuple<Integer,String> processAuth (HttpServletRequest req, HttpServletResponse rsp)
    {
        // see if they're a user of our Whirled app
        int fbUserId = 0;
        try {
            fbUserId = ParameterUtil.getIntParameter(req, "fb_sig_user", 0, "");
        } catch (Exception e) {
            // no problem, they'll just not be logged in
        }

        // if they're not an app user, clear their auth cookies and send them in as a guest
        if (fbUserId == 0) {
            return null;
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
            return new Tuple<Integer,String>(fbUserId, sessionCreds);
        }

        // we need to talk to facebook; create a client (which will require a valid session key)
        FacebookRestClient fbclient = getFacebookClient(req, fbUserId);
        if (fbclient == null) {
            return null;
        }

        boolean isAppUser = false;
        try {
            // validate their facebook credentials; this will throw an exception if the supplied
            // user id or session key are invalid; we also care if they have the app added
            isAppUser = fbclient.users_isAppAdded();
        } catch (Exception e) {
            log.warning("Facebook isAppAdded() failed [fbid=" + fbUserId + ", error=" + e + "].");
            return null;
        }

        try {
            // if this Facebook account is already mapped to a Whirled account, log them in
            int memberId = MsoyServer.memberRepo.lookupExternalAccount(
                ExternalMapRecord.FACEBOOK, String.valueOf(fbUserId));
            if (memberId != 0) {
                sessionCreds = MsoyServer.memberRepo.startOrJoinSession(memberId, FB_SESSION_DAYS);
                return new Tuple<Integer,String>(fbUserId, sessionCreds);
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to authenticate [id=" + fbUserId + "].", pe);
            return null;
        }

        // if they have not added the app, just send them in as a guest
        if (!isAppUser) {
            return null;
        }

        Document info = null;
        try {
            // look up information from this user's facebook profile
            ArrayIntSet ids = new ArrayIntSet();
            ids.add(fbUserId);
            EnumSet<ProfileField> fields = EnumSet.of(
                ProfileField.FIRST_NAME, ProfileField.LAST_NAME, ProfileField.SEX,
                ProfileField.BIRTHDAY);
            info = fbclient.users_getInfo(ids, fields);

        } catch (Exception e) {
            log.warning("Facebook getInfo() failed [fbid=" + fbUserId + ", error=" + e + "].");
            return null;
        }

        try {
            Node first = findNode(info, "users_getInfo_response.user.first_name");
            String name = (first == null) ? "" : first.getTextContent();
            String email = fbUserId + "@facebook.com";
            String password = ""; // TODO?

            // create a Whirled account for this Facebook user
            log.info("Creating Facebook account [name=" + name + ", id=" + fbUserId + "].");
            MemberRecord mrec = MsoyServer.author.createAccount(email, password, name, true, 0);
            MsoyServer.memberRepo.mapExternalAccount(
                ExternalMapRecord.FACEBOOK, String.valueOf(fbUserId), mrec.memberId);

            ProfileRecord prec = createProfile(info);
            prec.memberId = mrec.memberId;
            MsoyServer.profileRepo.storeProfile(prec);

            sessionCreds = MsoyServer.memberRepo.startOrJoinSession(mrec.memberId, FB_SESSION_DAYS);
            return new Tuple<Integer,String>(fbUserId, sessionCreds);

        } catch (ServiceException se) {
            log.warning("Failed to create account [id=" + fbUserId +
                        ", error=" + se.getMessage() + "].");
            return null;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to create account [id=" + fbUserId + "].", pe);
            return null;
        }
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

    protected ProfileRecord createProfile (Document info)
    {
        ProfileRecord prec = new ProfileRecord();

        try {
            Node first = findNode(info, "users_getInfo_response.user.first_name");
            Node last = findNode(info, "users_getInfo_response.user.last_name");
            if (first != null && last != null) {
                prec.realName = first.getTextContent() + " " + last.getTextContent();
            }

            Node bday = findNode(info, "users_getInfo_response.user.birthday");
            if (bday != null) {
                try {
                    Date date = _bfmt.parse(bday.getTextContent());
                    if (date != null) {
                        prec.birthday = new java.sql.Date(date.getTime());
                    }
                } catch (ParseException pe) {
                    // TEMP: let's see what sort of weird birthdays we find
                    log.info("Failed to parse birthday [text=" + bday.getTextContent() +
                             ", error=" + pe + "].");
                }
            }

            Node sex = findNode(info, "users_getInfo_response.user.sex");
            if (sex != null) {
                prec.isMale = "male".equalsIgnoreCase(sex.getTextContent());
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create profile.", e);
            FacebookRestClient.printDom(info, "");
        }

        return prec;
    }

    protected Node findNode (Node node, String path)
    {
        int didx = path.indexOf(".");
        String name = (didx == -1) ? path : path.substring(0, didx);
        NodeList children = node.getChildNodes();
        for (int ii = 0, ll = children.getLength(); ii < ll; ii++) {
            Node candidate = children.item(ii);
            if (name.equals(candidate.getNodeName())) {
                if (didx == -1) {
                    return candidate;
                } else {
                    return findNode(candidate, path.substring(didx+1));
                }
            }
        }
        return null;
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

    /** Used to parse Facebook profile birthdays. */
    protected static SimpleDateFormat _bfmt = new SimpleDateFormat("MMMM dd, yyyy");

    /** The number of days for which to preserve the Facebook auto granted session token. */
    protected static final int FB_SESSION_DAYS = 1;
}
