//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.servlet.util.FriendlyException;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.FacebookLogic;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Handles Facebook callback requests.
 */
public class FacebookServlet extends HttpServlet
{
    /**
     * Credentials for a user authenticating from an iframed Facebook app. These are never sent
     * over the wire.
     */
    public static class FacebookAppCreds extends ExternalCreds
    {
        /** The Facebook user id of the user in question. */
        public String uid;

        /** The API key of the app via which the user is authenticating. */
        public String apiKey;

        /** The app secret of the app via which the user is authenticating. */
        public String appSecret;

        /** The session key of the viewing user (may be null). */
        public String sessionKey;

        @Override // from ExternalCreds
        public ExternalAuther getAuthSource () {
            return ExternalAuther.FACEBOOK;
        }

        @Override // from ExternalCreds
        public String getUserId () {
            return uid;
        }

        @Override // from ExternalCreds
        public String getPlaceholderAddress () {
            return uid + "@facebook.com";
        }

        @Override // from ExternalCreds
        public String getSessionKey () {
            return sessionKey;
        }
    }

    protected void doHead (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got HEAD request " + req.getRequestURL());
        dumpParameters(req);
    }

    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        if (DeploymentConfig.devDeployment) {
            log.info("Got GET request " + req.getRequestURL());
            dumpParameters(req);
        }

        // determine whether we're in game mode or Whirled mode
        try {
            AppInfo info = parseAppInfo(req);

            // make sure we have signed facebook data
            validateRequest(req, info.appSecret);

            // we should either have 'canvas_user' or 'user'
            FacebookAppCreds creds = new FacebookAppCreds();
            creds.uid = ParameterUtil.getParameter(req, FBKEY_PREFIX + "canvas_user", "");
            if (StringUtil.isBlank(creds.uid)) {
                creds.uid = ParameterUtil.getParameter(req, FBKEY_PREFIX + "user", "");
            }
            if (StringUtil.isBlank(creds.uid)) {
                // this the "way" iframed Facebook apps redirect to the logon page, awseome!
                throw new FriendlyException(
                    "<script type=\"text/javascript\">\n" +
                    "top.location.href = \"" + getLoginURL(info.apiKey) + "\";\n</script>");
            }
            creds.apiKey = info.apiKey;
            creds.appSecret = info.appSecret;
            creds.sessionKey = ParameterUtil.getParameter(req, FBKEY_PREFIX + "session_key", true);

            // create a new visitor info which will either be ignored or used shortly
            VisitorInfo vinfo = new VisitorInfo();

            // authenticate this member via their external FB creds (this will autocreate their
            // account if they don't already have one)
            MemberRecord mrec = _auther.authenticateSession(
                creds, vinfo, AffiliateCookie.fromWeb(req));

            // if the member has the same visitor id as the one we just made up, they were just
            // created and we need to note that this is an entry
            if (vinfo.id.equals(mrec.visitorId)) {
                _memberLogic.noteNewVisitor(
                    vinfo, true, StringUtil.deNull(req.getPathInfo()), req.getHeader("Referrer"));
            }

            // activate a session for them
            String authtok = _memberRepo.startOrJoinSession(mrec.memberId, FBAUTH_DAYS);
            SwizzleServlet.setCookie(req, rsp, authtok);

            // add the privacy header so we can set some cookies in an iframe
            MsoyHttpServer.addPrivacyHeader(rsp);

            // let our app know it is embedded in facebook
            Cookie cookie = new Cookie(CookieNames.EMBED, "fb");
            cookie.setPath("/");
            rsp.addCookie(cookie);

            // and send them to the appropriate page
            if (info.gameId != 0) {
                String action = info.chromeless ? "pc" : "p";
                rsp.sendRedirect("/#" + Pages.WORLD.makeToken("game", action, info.gameId));
            } else {
                rsp.sendRedirect("/#" + Pages.GAMES.makeToken("fb"));
            }

        } catch (ServiceException se) {
            sendResponse(rsp, se.getMessage());
        } catch (FriendlyException fe) {
            sendResponse(rsp, fe.getMessage());
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

    protected void sendResponse (HttpServletResponse rsp, String message)
        throws IOException
    {
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println(message);
        } finally {
            StreamUtil.close(out);
        }
    }

    protected void validateRequest (HttpServletRequest req, String secret)
        throws FriendlyException
    {
        String sig = ParameterUtil.getParameter(req, "fb_sig", true);
        if (StringUtil.isBlank(sig)) {
            throw new FriendlyException("Missing fb_sig parameter");
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
        if (!sig.equals(StringUtil.md5hex(sigdata + secret))) {
            throw new FriendlyException("Invalid fb_sig parameter");
        }
    }

    protected AppInfo parseAppInfo (HttpServletRequest req)
        throws FriendlyException
    {
        String path = req.getPathInfo();
        AppInfo info = new AppInfo();
        if (path == null || !path.startsWith(GAME_PATH)) {
            info.apiKey = ServerConfig.config.getValue("facebook.api_key", "");
            info.appSecret = ServerConfig.config.getValue("facebook.secret", "");
            info.gameId = ParameterUtil.getIntParameter(req, "game", 0, "Invalid game parameter");
            return info;
        }

        int gameId;
        try {
            gameId = Integer.parseInt(path.substring(GAME_PATH.length()));
        } catch (Exception e) {
            throw new FriendlyException("Invalid game URL: " + path);
        }

        GameInfoRecord ginfo = _mgameRepo.loadGame(gameId);
        if (ginfo == null) {
            throw new FriendlyException("Unknown game: " + gameId);
        }

        info.gameId = ginfo.gameId;

        FacebookInfo fbinfo = _mgameRepo.loadFacebookInfo(ginfo.gameId);
        if (fbinfo.apiKey == null) {
            throw new FriendlyException("Game missing Facebook info: " + ginfo.name);
        }

        info.apiKey = fbinfo.apiKey;
        info.appSecret = fbinfo.appSecret;
        info.chromeless = fbinfo.chromeless;
        return info;
    }

    protected static String getLoginURL (String key)
    {
        return "http://www.facebook.com/login.php?api_key=" + key + "&canvas=1&v=1.0";
    }

    protected static class AppInfo
    {
        public int gameId;
        public String apiKey;
        public String appSecret;
        public boolean chromeless;
    }

    @Inject protected FacebookLogic _faceLogic;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyAuthenticator _auther;
    @Inject protected MsoyGameRepository _mgameRepo;

    protected static final String FBKEY_PREFIX = "fb_sig_";
    protected static final int FBAUTH_DAYS = 2;
    protected static final String GAME_PATH = "/game/";
}
