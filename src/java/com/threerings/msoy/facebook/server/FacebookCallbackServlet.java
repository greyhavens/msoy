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

import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookTemplateCard;

import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceException;
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
        String trackingId = StringUtil.deNull(req.getParameter(TRACKING));
        String newInstall = StringUtil.deNull(req.getParameter(NEW_INSTALL));

        // if we don't have a signature, then we must be swizzling
        if (req.getParameter(FB_SIG) == null) {
            String session = req.getParameter(SESSION);
            String canvas = req.getParameter(CANVAS);
            String token = req.getParameter(TOKEN);
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
            rsp.sendRedirect(SharedNaviUtil.buildRequest(FacebookLogic.getCanvasUrl(canvas),
                TOKEN, token, NEW_INSTALL, newInstall, TRACKING, trackingId));
            return;
        }

        // determine whether we're in game mode or Whirled mode
        ReqInfo info = parseReqInfo(req);

        // make sure we have signed facebook data
        validateSignature(req, info.appSecret);

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
            log.info("Redirecting to login", "key", info.apiKey);
            MsoyHttpServer.sendTopRedirect(rsp, info.getLoginURL());
            return;
        }

        // set up the token to redirect to - either the pre-processed one after we've swizzled in
        // the session cookie, or the one from the original request; NOTE: the TOKEN parameter is
        // double encoded, but we are careful to avoid confusion and not give it any % characters
        String token = StringUtil.getOr(req.getParameter(TOKEN), info.getDestinationToken());

        // is the session already set up?
        if (session.equals(CookieUtil.getCookieValue(req, WebCreds.credsCookie()))) {
            // now we can attach some encoded % characters, now that facebook is finished double
            // encoding the parameters on the way to the callback (I doubt they'll ever fix that)
            token = StringUtil.encode(info.attachCreds(token, creds));

            // track it
            // TODO: Kontagent tracking for API games?
            if (info.mainApp) {
                _tracker.trackUsage(
                    Long.parseLong(creds.uid), trackingId, !StringUtil.isBlank(newInstall));
            }

            log.info("Redirecting to token", "key", info.apiKey, "token", token);

            // TODO: probably don't need this anymore
            // add the privacy header (for IE) so we can set some cookies in an iframe
            MsoyHttpServer.addPrivacyHeader(rsp);

            // and send them to the appropriate page
            rsp.sendRedirect("/#" + token);
            return;
        }

        // otherwise redirect the top frame back to this page with the already-processed tokens
        log.info("Initiating swizzle", "session", session, "token", token,
            "canvas", info.canvasName);

        MsoyHttpServer.sendTopRedirect(rsp, SharedNaviUtil.buildRequest(
            req.getRequestURI(), SESSION, session, TOKEN, token, CANVAS, info.canvasName,
            TRACKING, trackingId, NEW_INSTALL, newInstall));
    }

    /**
     * Activates a session for an existing facebook user or creates a new account and returns the
     * authentication token. Returns null if the user has not authorized the application. Fills
     * in the given credentials.
     */
    protected String activateSession (ReqInfo info, HttpServletRequest req, FacebookAppCreds creds)
        throws ServiceException
    {
        creds.sessionKey = req.getParameter(FB_SESSION_KEY);
        if (creds.sessionKey == null) {
            return null;
        }

        // we should either have 'canvas_user' or 'user'
        creds.uid = StringUtil.getOr(req.getParameter(FB_CANVAS_USER),
            req.getParameter(FB_USER));
        creds.apiKey = info.apiKey;
        creds.appSecret = info.appSecret;

        // create a new visitor info which will either be ignored or used shortly
        VisitorInfo vinfo = new VisitorInfo();

        // authenticate this member via their external FB creds (this will autocreate their
        // account if they don't already have one)
        MemberRecord mrec = _auther.authenticateSession(
            creds, vinfo, AffiliateCookie.fromWeb(req));

        // if the member has the same visitor id as the one we just made up, they were just
        // created and we need to note that this is an entry
        if (vinfo.id.equals(mrec.visitorId)) {
            _memberLogic.noteNewVisitor(vinfo, true, info.vector, req.getHeader("Referrer"));
        }

        // activate a session for them
        return _memberRepo.startOrJoinSession(mrec.memberId, FBAUTH_DAYS);
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
        validateSignature(req, info.appSecret);

        if (!info.ping) {
            throw new ServiceException();
        }

        boolean added;
        String truth = "1";
        if (truth.equals(req.getParameter(FB_INSTALL)) ||
            truth.equals(req.getParameter(FB_AUTH))) {
            added = true;
        } else {
            added = false;
            if (!truth.equals(req.getParameter(FB_UNINSTALL))) {
                log.warning("Ping parameter not set, assuming removal");
                MsoyHttpServer.dumpParameters(req);
            }
        }

        // NOTE: we currently do not track application additions here, because we don't have access
        // to the tracking id parameter - instead adds are tracked specially by attaching
        // NEW_INSTALL to the parameters for the login redirect and checking it later
        long uid = Long.parseLong(req.getParameter(FB_USER));
        if (!added) {
            _tracker.trackApplicationRemoved(uid);
        }
    }

    /**
     * Just checks the fb_sig parameter agress with the fb_sig_ parameters according to the
     * Facebook documentation.
     */
    protected void validateSignature (HttpServletRequest req, String secret)
        throws ServiceException
    {
        String sig = req.getParameter("fb_sig");
        if (StringUtil.isBlank(sig)) {
            throw new ServiceException("Missing fb_sig parameter");
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
            throw new ServiceException("Invalid fb_sig parameter");
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

        if (!path.startsWith(GAME_PATH)) {
            // fill in the FB creds for this deployment
            info.apiKey = ServerConfig.config.getValue("facebook.api_key", "");
            info.appSecret = ServerConfig.config.getValue("facebook.secret", "");
            info.canvasName = ServerConfig.config.getValue("facebook.canvas_name", "");
            info.mainApp = true;

            if (path.startsWith(PING_PATH)) {
                // pings don't have game and vector data, just return
                info.ping = true;
                return info;
            }

            // this is a normal request for Whirled Games, parse params for redirect
            info.game = _faceLogic.parseGame(req);
            info.vector = req.getParameter(ArgNames.VECTOR);
            if (info.vector == null) {
                info.vector = FacebookTemplateCard.toEntryVector("app", "");
            }
            info.challenge = req.getParameter(CHALLENGE) != null;
            info.trackingId = req.getParameter(TRACKING);
            return info;
        }

        // this is a request from an integrated game's app, fill in game stuff
        int gameId;
        try {
            gameId = Integer.parseInt(path.substring(GAME_PATH.length()));
        } catch (Exception e) {
            throw new ServiceException("Invalid game URL: " + path);
        }

        GameInfoRecord ginfo = _mgameRepo.loadGame(gameId);
        if (ginfo == null) {
            throw new ServiceException("Unknown game: " + gameId);
        }

        info.game = new FacebookGame(ginfo.gameId);

        FacebookInfo fbinfo = _mgameRepo.loadFacebookInfo(ginfo.gameId);
        if (fbinfo.apiKey == null) {
            throw new ServiceException("Game missing Facebook info: " + ginfo.name);
        }

        info.apiKey = fbinfo.apiKey;
        info.appSecret = fbinfo.appSecret;
        info.canvasName = fbinfo.canvasName;
        info.chromeless = fbinfo.chromeless;
        info.vector = FacebookTemplateCard.toEntryVector("proxygame", "" + ginfo.gameId);
        return info;
    }

    protected static class ReqInfo
    {
        public FacebookGame game;
        public String apiKey;
        public String appSecret;
        public String canvasName;
        public boolean chromeless;
        public String vector;
        public boolean challenge;
        public boolean ping;
        public String trackingId;
        public boolean mainApp;

        /**
         * Gets the GWT token that the user should be redirected to in the whirled application.
         * Some creds information may be assembled and passed into a game application.
         */
        public String getDestinationToken ()
        {
            Args embed = ArgNames.Embedding.compose(ArgNames.Embedding.FACEBOOK);

            // and send them to the appropriate page
            if (game != null) {
                if (chromeless) {
                    // chromeless games go directly into the game
                    return Pages.WORLD.makeToken("fbgame", game.getIntId());

                } else if (challenge) {
                    // completion of the challenge flow (ideally this would just be done in gwt
                    // but Facebook request submissions work like forms and we therefore route them
                    // via whirled.com/fbinvite/ndone, which then needs to complete the flow by
                    // redirecting to the main canvas)
                    return Pages.FACEBOOK.makeToken(game.getChallengeArgs(),
                        ArgNames.FB_CHALLENGE_FEED, embed);

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
            if (game == null || !chromeless) {
                return token;
            }

            return Pages.fromHistory(token).makeToken(
                Args.fromHistory(token), creds.uid, creds.sessionKey);
        }

        protected String getLoginURL ()
        {
            // pass in an installed flag so we know when the user has arrived for the first time
            String nextUrl = SharedNaviUtil.buildRequest(
                FacebookLogic.getCanvasUrl(canvasName), NEW_INSTALL, "y");

            // preserve the tracking id after login
            if (!StringUtil.isBlank(trackingId)) {
                nextUrl = SharedNaviUtil.buildRequest(nextUrl, TRACKING, trackingId);
            }

            // assemble the url with all the parameters
            return SharedNaviUtil.buildRequest("http://www.facebook.com/login.php",
                "api_key", apiKey, "canvas", "1", "v", "1.0", "next", StringUtil.encode(nextUrl));
        }
    }

    @Inject protected FacebookLogic _faceLogic;
    @Inject protected KontagentLogic _tracker;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyAuthenticator _auther;
    @Inject protected MsoyGameRepository _mgameRepo;

    protected static final String FB_SIG = "fb_sig";
    protected static final String FBKEY_PREFIX = FB_SIG + "_";
    protected static final String FB_USER = FBKEY_PREFIX + "user";
    protected static final String FB_CANVAS_USER = FBKEY_PREFIX + "canvas_user";
    protected static final String FB_ADDED = FBKEY_PREFIX + "added";
    protected static final String FB_SESSION_KEY = FBKEY_PREFIX + "session_key";
    protected static final String FB_INSTALL = FBKEY_PREFIX + "install";
    protected static final String FB_AUTH = FBKEY_PREFIX + "authorize";
    protected static final String FB_UNINSTALL = FBKEY_PREFIX + "uninstall";
    protected static final String FB_TIME = FBKEY_PREFIX + "time";
    protected static final int FBAUTH_DAYS = 2;
    protected static final String GAME_PATH = "/game/";
    protected static final String PING_PATH = "/ping/";
    protected static final String SESSION = "session";
    protected static final String CANVAS = "canvas";
    protected static final String TOKEN = "token";
    protected static final String TRACKING = ArgNames.FB_PARAM_TRACKING;
    protected static final String CHALLENGE = ArgNames.FB_PARAM_CHALLENGE;
    protected static final String NEW_INSTALL = "newuser";
}
