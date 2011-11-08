//
// $Id$

package com.threerings.msoy.facebook.server;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.StringUtil;

import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.util.ParameterUtil;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.apps.server.persist.AppInfoRecord;
import com.threerings.msoy.apps.server.persist.AppRepository;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookTemplate;
import com.threerings.msoy.facebook.server.persist.FacebookInfoRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.ClientMode;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.Embedding;
import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.AffiliateCookie;
import com.threerings.msoy.web.server.MsoyHttpServer;
import com.threerings.msoy.web.server.SwizzleServlet;

import static com.threerings.msoy.Log.log;

/**
 * Handles Facebook callback requests.
 */
public class FacebookCallbackServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doHead (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        log.info("Got HEAD request " + req.getRequestURL());
        MsoyHttpServer.dumpParameters(req);
    }

    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        if (DeploymentConfig.devDeployment) {
            log.info("Got GET request " + req.getRequestURL());
            MsoyHttpServer.dumpParameters(req);
        }

        try {
            tryGet(req, rsp);

        } catch (ServiceException se) {
            log.warning("Error in Facebook callback", se);
            // TODO: we won't need these extra dumps once everything is working well
            MsoyHttpServer.dumpParameters(req);
            MsoyHttpServer.dumpCookies(req);
            MsoyHttpServer.dumpHeaders(req);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void tryGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, ServiceException
    {
        // we want to preserve these values across all redirects, stash them here
        String trackingId = StringUtil.deNull(FrameParam.TRACKING.get(req));
        String newInstall = StringUtil.deNull(FrameParam.NEW_INSTALL.get(req));
        int affiliate = getAffiliate(req);

        // if we don't have a signature, then we must be swizzling
        if (ConnParam.SIG.get(req) == null) {
            String session = FrameParam.SESSION.get(req);
            String canvas = FrameParam.CANVAS.get(req);
            String token = FrameParam.TOKEN.get(req);
            if (session == null || canvas == null || token == null) {
                throw new ServiceException("Swizzle parameters not found [" +
                    session + ", " + canvas + ", " + token + "]?");
            }

            // double check the session
            if (_memberRepo.loadMemberForSession(session) == null) {
                throw new ServiceException("We're a swizzlin' an invalid session, yeehaw");
            }

            log.info("Swizzling", "session", session, "token", token, "canvas", canvas);

            // IE won't give the cookie back on the third request without this
            MsoyHttpServer.addPrivacyHeader(rsp);

            // set the cookie
            SwizzleServlet.setCookie(req, rsp, session);

            // redirect back to the application with the token tacked on
            String redirect = SharedNaviUtil.buildRequest(FacebookLogic.getCanvasUrl(canvas),
                FrameParam.TOKEN.name, token, FrameParam.NEW_INSTALL.name, newInstall,
                FrameParam.TRACKING.name, trackingId);

            // include the affiliate if we were given one
            redirect = addAffiliate(redirect, affiliate);

            rsp.sendRedirect(redirect);
            return;
        }

        // determine whether we're in game mode or Whirled mode
        ReqInfo info = parseReqInfo(req);

        // make sure we have signed facebook data
        validateSignature(req, info.fb.appSecret);

        // ping should only POST
        if (info.ping) {
            throw new ServiceException();
        }

        // parse the credentials and authenticate (may create a new FB connected user account)
        FacebookAppCreds creds = new FacebookAppCreds();
        String session = activateSession(info, req, creds);

        // if the user has not authorized our application
        if (session == null) {
            // redirect to app login page and bail (parameters aren't retained so don't bother)
            log.info("Redirecting to login", "key", info.fb.apiKey);
            MsoyHttpServer.sendTopRedirect(rsp, info.getLoginURL());
            return;
        }

        // set up the token to redirect to - either the pre-processed one after we've swizzled in
        // the session cookie, or the one from the original request; NOTE: the TOKEN parameter is
        // double encoded, but we are careful to avoid confusion and not give it any % characters
        String token = StringUtil.getOr(FrameParam.TOKEN.get(req), info.getDestinationToken());

        // is the session already set up?
        if (session.equals(CookieUtil.getCookieValue(req, WebCreds.credsCookie()))) {
            // now we can attach some encoded % characters, now that facebook is finished double
            // encoding the parameters on the way to the callback (I doubt they'll ever fix that)
            token = StringUtil.encode(info.attachCreds(token, creds));

            // track it
            // TODO: Kontagent tracking for API games?
            Integer appId = info.siteId.getFacebookAppId();
            if (appId != null) {
                _tracker.trackUsage(
                    appId, Long.parseLong(creds.uid), trackingId, !StringUtil.isBlank(newInstall));
            }

            log.info("Redirecting to token", "key", info.fb.apiKey, "token", token);

            // TODO: probably don't need this anymore
            // add the privacy header (for IE) so we can set some cookies in an iframe
            MsoyHttpServer.addPrivacyHeader(rsp);

            // and send them to the appropriate page
            rsp.sendRedirect("/#" + token);
            return;
        }

        // otherwise redirect the top frame back to this page with the already-processed tokens
        log.info("Initiating swizzle", "session", session, "token", token,
            "canvas", info.fb.canvasName);

        MsoyHttpServer.sendTopRedirect(rsp, addAffiliate(SharedNaviUtil.buildRequest(
            req.getRequestURI(), FrameParam.SESSION.name, session, FrameParam.TOKEN.name, token,
            FrameParam.CANVAS.name, info.fb.canvasName, FrameParam.TRACKING.name, trackingId,
            FrameParam.NEW_INSTALL.name, newInstall), affiliate));
    }

    /**
     * Activates a session for an existing facebook user or creates a new account and returns the
     * authentication token. Returns null if the user has not authorized the application. Fills
     * in the given credentials.
     */
    protected String activateSession (ReqInfo info, HttpServletRequest req, FacebookAppCreds creds)
        throws ServiceException
    {
        creds.sessionKey = req.getParameter(ConnParam.SESSION_KEY.name);
        if (creds.sessionKey == null) {
            return null;
        }

        // we should either have 'canvas_user' or 'user'
        creds.uid = StringUtil.getOr(ConnParam.CANVAS_USER.get(req), ConnParam.USER.get(req));
        creds.apiKey = info.fb.apiKey;
        creds.appSecret = info.fb.appSecret;
        creds.site = info.siteId;

        // create a new visitor info which will either be ignored or used shortly
        VisitorInfo vinfo = new VisitorInfo();

        // authenticate this member via their external FB creds (this will autocreate their
        // account if they don't already have one)
        MemberRecord mrec = _auther.authenticateSession(
            creds, vinfo, AffiliateCookie.fromCreds(info.affiliate), info.app.appId);

        // if the member has the same visitor id as the one we just made up, they were just
        // created and we need to note that this is an entry
        if (vinfo.id.equals(mrec.visitorId)) {
            // note: the HTTP referrer field is spelled 'Referer', sigh (thanks Bruno).
            _memberLogic.noteNewVisitor(
                vinfo, true, info.vector, req.getHeader("Referer"), mrec.memberId);

            // DEBUG
            log.info("VisitorInfo created", "info", vinfo, "reason", "FacebookCallbackServlet",
                "vector", info.vector, "ref", req.getHeader("Referer"),
                "memberId", mrec.memberId);

        }

        // activate a session for them
        return _memberRepo.startOrJoinSession(mrec.memberId);
    }

    @Override // from HttpServlet
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        if (DeploymentConfig.devDeployment) {
            log.info("Got POST request " + req.getRequestURL());
            MsoyHttpServer.dumpParameters(req);
        }

        try {
            tryPost(req, rsp);

        } catch (ServiceException se) {
            log.warning("Error in Facebook POST callback", se);
            // TODO: we won't need these extra dumps once everything is working well
            MsoyHttpServer.dumpParameters(req);
            MsoyHttpServer.dumpCookies(req);
            MsoyHttpServer.dumpHeaders(req);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void tryPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, ServiceException
    {
        ReqInfo info = parseReqInfo(req);
        validateSignature(req, info.fb.appSecret);

        if (!info.ping) {
            throw new ServiceException();
        }

        boolean added;
        String truth = "1";
        if (truth.equals(ConnParam.INSTALL.get(req)) ||
            truth.equals(ConnParam.AUTH.get(req))) {
            added = true;
        } else {
            added = false;
            if (!truth.equals(ConnParam.UNINSTALL.get(req))) {
                log.warning("Ping parameter not set, assuming removal");
                MsoyHttpServer.dumpParameters(req);
            }
        }

        // NOTE: we currently do not track application additions here, because we don't have access
        // to the tracking id parameter - instead adds are tracked specially by attaching
        // NEW_INSTALL to the parameters for the login redirect and checking it later
        long uid = Long.parseLong(ConnParam.USER.get(req));
        if (!added) {
            Integer appId = info.siteId.getFacebookAppId();
            if (appId != null) {
                _tracker.trackApplicationRemoved(appId, uid);
            }
        }
    }

    /**
     * Just checks the fb_sig parameter agrees with the fb_sig_ parameters according to the
     * Facebook documentation.
     */
    protected void validateSignature (HttpServletRequest req, String secret)
        throws ServiceException
    {
        String sig = ConnParam.SIG.get(req);
        if (StringUtil.isBlank(sig)) {
            throw new ServiceException("Missing sig parameter");
        }

        // obtain a list of all fb_sig_ keys and sort them alphabetically by key
        List<String> params = Lists.newArrayList();
        for (String pname : ParameterUtil.getParameterNames(req)) {
            String signedName = ConnParam.extractSignedName(pname);
            if (signedName != null) {
                params.add(signedName + "=" + req.getParameterValues(pname)[0]);
            }
        }
        Collections.sort(params);

        // concatenate them all together (no separator) and MD5 this plus our secret key
        String sigdata = StringUtil.join(params.toArray(new String[params.size()]), "");
        if (!sig.equals(StringUtil.md5hex(sigdata + secret))) {
            throw new ServiceException("Invalid sig parameter");
        }
    }

    /**
     * Determines what has been requested ant associated application parameters. There are 2 basic
     * modes for our intial facebook entry: the main app or a specific game.
     */
    protected ReqInfo parseReqInfo (HttpServletRequest req)
        throws ServiceException
    {
        String path = StringUtil.deNull(req.getPathInfo());
        ReqInfo info = new ReqInfo();

        if (path.startsWith(APP_PATH)) {
            // this is a request from an app that we host
            info.app = requireApp(path, APP_PATH);
            info.siteId = ExternalSiteId.facebookApp(info.app.appId);
            info.fb = validate(_facebookRepo.loadAppFacebookInfo(info.app.appId));
            info.game = _faceLogic.parseGame(req);
            info.vector = StringUtil.getOr(FrameParam.VECTOR.get(req),
                FacebookTemplate.toEntryVector("app" + String.valueOf(info.app.appId), ""));
            info.trackingId = FrameParam.TRACKING.get(req);

        } else if (path.startsWith(PING_PATH)) {
            // this is a ping for application add/remove
            info.app = requireApp(path, PING_PATH);
            info.siteId = ExternalSiteId.facebookApp(info.app.appId);
            info.fb = validate(_facebookRepo.loadAppFacebookInfo(info.app.appId));
            info.ping = true;

        } else if (path.startsWith(GAME_PATH)) {
            // this is a request from an integrated game's fb app
            int gameId = requireInt(path.substring(GAME_PATH.length()));
            GameInfoRecord ginfo = _mgameRepo.loadGame(gameId);
            if (ginfo == null) {
                throw new ServiceException("Unknown game: " + gameId);
            }
            info.siteId = ExternalSiteId.facebookGame(ginfo.gameId);
            info.fb = validate(_facebookRepo.loadGameFacebookInfo(ginfo.gameId));
            info.game = new FacebookGame(ginfo.gameId);
            info.vector = FacebookTemplate.toEntryVector("proxygame", "" + ginfo.gameId);

        } else {
            // this is an old skool request for the Whirled Games
            // TODO: remove after production transitions to the above code
            info.siteId = _faceLogic.getDefaultGamesSite();
            info.app = _appRepo.loadAppInfo(info.siteId.getFacebookAppId());
            info.fb = _facebookRepo.loadAppFacebookInfo(info.siteId.getFacebookAppId());
            info.game = _faceLogic.parseGame(req);
            info.vector = StringUtil.getOr(FrameParam.VECTOR.get(req),
                FacebookTemplate.toEntryVector("app", ""));
            info.trackingId = FrameParam.TRACKING.get(req);
        }

        info.affiliate = getAffiliate(req);
        return info;
    }

    protected AppInfoRecord requireApp (String path, String prefix)
        throws ServiceException
    {
        AppInfoRecord appInfo = _appRepo.loadAppInfo(requireInt(path.substring(prefix.length())));
        if (appInfo == null) {
            throw new ServiceException("Unknown app requested: " + path);
        }
        return appInfo;
    }

    protected static FacebookInfoRecord validate (FacebookInfoRecord fbinfo)
        throws ServiceException
    {
        if (StringUtil.isBlank(fbinfo.apiKey)) {
            throw new ServiceException("No api key [app=" +
                fbinfo.appId + ", game=" + fbinfo.gameId + "]");
        }
        if (StringUtil.isBlank(fbinfo.appSecret)) {
            throw new ServiceException("No secret [app=" +
                fbinfo.appId + ", game=" + fbinfo.gameId + "]");
        }
        if (StringUtil.isBlank(fbinfo.canvasName)) {
            throw new ServiceException("No canvas name [app=" +
                fbinfo.appId + ", game=" + fbinfo.gameId + "]");
        }
        return fbinfo;
    }

    protected static int requireInt (String str)
        throws ServiceException
    {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            throw new ServiceException("Invalid integer: " + str);
        }
    }

    protected static int getAffiliate (HttpServletRequest req)
    {
        String affiliate = req.getParameter(CookieNames.AFFILIATE);
        return affiliate == null ? 0 : Integer.parseInt(affiliate);
    }

    protected static String addAffiliate (String url, int affiliate)
    {
        if (affiliate != 0) {
            url = SharedNaviUtil.buildRequest(url,
                CookieNames.AFFILIATE, String.valueOf(affiliate));
        }
        return url;
    }

    protected static class ReqInfo
    {
        public AppInfoRecord app;
        public FacebookInfoRecord fb;
        public FacebookGame game;
        public String vector;
        public boolean ping;
        public String trackingId;
        public ExternalSiteId siteId;
        public int affiliate;

        /**
         * Gets the GWT token that the user should be redirected to in the whirled application.
         * Some creds information may be assembled and passed into a game application.
         */
        public String getDestinationToken ()
        {
            // TODO: what mode should integrated games get... set to unspecified for now
            Embedding embedding = app != null ?
                new Embedding(app.clientMode, app.appId) :
                new Embedding(ClientMode.UNSPECIFIED, 0);
            Args embed = embedding.compose();

            // and send them to the appropriate page
            if (app.clientMode == ClientMode.FB_ROOMS) {
                // go to the home room
                return Pages.WORLD.makeToken("h", embed);

            } else if (game != null) {
                if (fb.chromeless) {
                    // chromeless games go directly into the game
                    return Pages.WORLD.makeToken("fbgame", game.getIntId());

                } else {
                    // other games are "viewed"
                    return Pages.GAMES.makeToken(game.getViewArgs(), embed);
                }
            } else {
                return Pages.GAMES.makeToken(embed);
            }
        }

        /**
         * Attaches the facebook uid and and session key to the token, if appropriate. Otherwise,
         * returns the token unmodified.
         */
        public String attachCreds (String token, FacebookAppCreds creds)
        {
            if (game == null || !fb.chromeless) {
                return token;
            }

            // TODO: verify the credentials are for this game
            return Pages.fromHistory(token).makeToken(
                Args.fromHistory(token), creds.uid, creds.sessionKey);
        }

        public String getLoginURL ()
        {
            // pass in an installed flag so we know when the user has arrived for the first time
            String nextUrl = SharedNaviUtil.buildRequest(
                FacebookLogic.getCanvasUrl(fb.canvasName), FrameParam.NEW_INSTALL.name, "y");

            // preserve the tracking id after login
            if (!StringUtil.isBlank(trackingId)) {
                nextUrl = SharedNaviUtil.buildRequest(nextUrl,
                    FrameParam.TRACKING.name, trackingId);
            }

            // preserve the affiliate
            nextUrl = addAffiliate(nextUrl, affiliate);

            // assemble the url with all the parameters
            return SharedNaviUtil.buildRequest("http://www.facebook.com/login.php",
                "api_key", fb.apiKey, "canvas", "1", "v", "1.0",
                "next", StringUtil.encode(nextUrl));
        }
    }

    /**
     * Parameters given to us by facebook connect, normally when someone is interacting with our
     * app from facebook.com.
     */
    protected enum ConnParam
    {
        USER("user"),
        CANVAS_USER("canvas_user"),
        ADDED("added"),
        SESSION_KEY("session_key"),
        INSTALL("install"),
        AUTH("authorize"),
        UNINSTALL("uninstall"),
        TIME("time"),
        SIG("fb_sig", false);

        /** The name of the parameter. */
        public String name;

        /**
         * If the given name is a signed parameter name, return the part to use for the signature,
         * otherwise null.
         */
        public static String extractSignedName (String pname)
        {
            return pname.startsWith(SIGNED_PREFIX) ?
                pname.substring(SIGNED_PREFIX.length()) : null;
        }

        /**
         * Shortcut to get the value of this parameter from a servlet request.
         */
        public String get (HttpServletRequest req)
        {
            return req.getParameter(name);
        }

        /** Prefix used for parameter signing. */
        protected static final String SIGNED_PREFIX = "fb_sig_";

        ConnParam (String name) {
            this(name, true);
        }

        ConnParam (String name, boolean signed) {
            this.name = signed ? (SIGNED_PREFIX + name) : name;
        }
    }

    /**
     * Parameters that we pass to our own frame, either indirectly through canvas page links (e.g.
     * app.facebook.com/whired/?tr=...) or directly through redirects. Some parameters are copied
     * from gwt-accessible parameters and others are purely internal.
     */
    protected enum FrameParam
    {
        SESSION("session"),
        CANVAS("canvas"),
        TOKEN("token"),
        TRACKING(ArgNames.FBParam.TRACKING),
        NEW_INSTALL("newuser"),
        VECTOR(ArgNames.VECTOR);

        /** The name of this parameter. */
        public String name;

        /**
         * Shortcut to get the value of this parameter from a servlet request.
         */
        public String get (HttpServletRequest req)
        {
            return req.getParameter(name);
        }

        FrameParam (String name) {
            this.name = name;
        }

        FrameParam (ArgNames.FBParam fbparam) {
            this.name = fbparam.name;
        }
    }

    @Inject protected AppRepository _appRepo;
    @Inject protected FacebookLogic _faceLogic;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected KontagentLogic _tracker;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyAuthenticator _auther;
    @Inject protected MsoyGameRepository _mgameRepo;

    protected static final String GAME_PATH = "/game/";
    protected static final String APP_PATH = "/app/";
    protected static final String PING_PATH = "/ping/";
}
