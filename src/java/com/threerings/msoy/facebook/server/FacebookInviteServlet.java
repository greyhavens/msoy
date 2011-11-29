//
// $Id$

package com.threerings.msoy.facebook.server;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.gwt.util.StringUtil;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.server.FacebookLogic.SessionInfo;
import com.threerings.msoy.facebook.server.persist.FacebookInfoRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ArgNames.FBParam;
import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.MarkupBuilder;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.MemberHelper;
import com.threerings.msoy.web.server.MsoyHttpServer;

import static com.threerings.msoy.Log.log;

/**
 * Handles requests to do facebook invites, which at the moment requires some backflips to get the
 * facebook XFBML elements stuck in the top-level html document.
 */
// FIXME(bruno): This needs to be upgraded to the Open Graph API
public class FacebookInviteServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        try {
            IDCard card = getIds(req, rsp);
            if (card.memberId == 0) {
                return;
            }

            if (req.getPathInfo().equals("/do")) {
                String acceptPath = req.getParameter("path");
                if (acceptPath == null) {
                    rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                int gameId;
                try {
                    String gameIdStr = req.getParameter(FBParam.GAME.name);
                    if (gameIdStr == null) {
                        rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    gameId = Integer.parseInt(gameIdStr);

                } catch (NumberFormatException nfe) {
                    rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                GameInfoRecord game = _mgameRepo.loadGame(gameId);
                String gameName = (game == null) ? null : game.name;
                if (gameName == null) {
                    rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                outputInvitePage(rsp, gameId, gameName, card, acceptPath);

            } else {
                handleSubmission(req, rsp, card);
            }

        } catch (Exception e) {
            log.warning("Failed to output facebook invite page.", e);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    @Override
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        IDCard card = getIds(req, rsp);
        if (card.memberId == 0) {
            return;
        }

        handleSubmission(req, rsp, card);
    }

    protected void handleSubmission (
        HttpServletRequest req, HttpServletResponse rsp, IDCard card)
        throws IOException
    {
        FacebookGame game = _fbLogic.parseGame(req);
        if (req.getPathInfo().equals("/done")) {
            // this was a FB connect popup invite
            trackSentInvites(req, card, game);
            outputCloseWindowPage(rsp);

        } else if (req.getPathInfo().equals("/ndone")) {

            // this was a portal invite, either to the app or a game challenge
            trackSentInvites(req, card, game);

            String canvas = _fbLogic.getCanvasUrl(ExternalSiteId.facebookApp(card.appId));
            if (game == null) {
                // head back to the facebook app main page
                MsoyHttpServer.sendTopRedirect(rsp, canvas);

            } else if (req.getParameter(FBParam.CHALLENGE.name) != null) {
                // head back to the app and do the final phase of the challenge flow
                MsoyHttpServer.sendTopRedirect(rsp, SharedNaviUtil.buildRequest(
                    SharedNaviUtil.buildRequest(canvas, game.getCanvasArgs()),
                    ArgNames.fbChallengeArgs()));

            } else {
                // head back to the app and just view the game
                MsoyHttpServer.sendTopRedirect(rsp,
                    SharedNaviUtil.buildRequest(canvas, game.getCanvasArgs()));
            }
        } else {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void outputInvitePage (
        HttpServletResponse rsp, int gameId, String gameName, IDCard card, String acceptPath)
        throws IOException
    {
        // sanitize the game name, just eliminate ", ', \, <, >
        // TODO: we could get complicated and try to support encoding here
        gameName = gameName.replace("\\", "").replace("\"", "").replace("<", "").replace(">", "");
        gameName = gameName.replace("'", "");

        FacebookInfoRecord fbinfo = _fbRepo.loadAppFacebookInfo(card.appId);

        // javascript variables - squirted below and referenced by the static script INVITE_JS
        Map<String, String> vars = new ImmutableMap.Builder<String, String>()
            .put("gameId", String.valueOf(gameId))
            .put("acceptPath", Pages.WORLD.makeFriendURL(
                card.memberId, Args.fromToken(acceptPath)))
            .put("acceptLabel", "Play " + gameName)
            .put("action", SharedNaviUtil.buildRequest(
                DeploymentConfig.serverURL + "fbinvite/done", FBParam.GAME.name, "" + gameId))
            .put("message", "I'm playing " + gameName + " on Whirled, join me!")
            .put("apiKey", StringUtil.getOr(fbinfo.apiKey, ""))
            .put("formElemId", FORM_TAG_ID)
            .put("gameName", gameName)
            .put("actionText", "Select some Faceook friends to play " + gameName + " with you.")
            .put("receiverPath", RECEIVER_PATH)
            .build();

        // html and javascript code to show a facebook invitation page
        MarkupBuilder b = new MarkupBuilder();
        String js = "text/javascript";
        b.open("html", "xmlns", "http://www.w3.org/1999/xhtml", "xmlns:fb", FBMLNS);
        b.open("body").append("\n");
        b.open("script", "type", js, "src", FBLOADERCODE).append().close().append("\n");
        b.open("script", "type", js, "src", INVITE_JS).append().close().append("\n");
        b.open("div", "id", FORM_TAG_ID).append().close().append("\n");
        b.open("script", "type", js).append("\n//<!--\n");
        String dataName = "whirledInviteData";
        b.append("var ").append(dataName).append(" = {};\n");
        for (Map.Entry<String, String> kvPair : vars.entrySet()) {
            b.append(dataName).append(".").append(kvPair.getKey()).append(" = \"")
             .append(kvPair.getValue()).append("\";\n");
        }
        b.append("FBInit(").append(dataName).append(");\n");
        b.append("FBShowInvite(").append(dataName).append(");");
        b.append("\n//-->").close().append("\n");

        // send
        rsp.getOutputStream().println(b.finish());
        StreamUtil.close(rsp.getOutputStream());
    }

    protected void outputCloseWindowPage (HttpServletResponse rsp)
        throws IOException
    {
        // html and javascript code to close the window
        MarkupBuilder b = new MarkupBuilder();
        String js = "text/javascript";
        b.open("html", "xmlns", "http://www.w3.org/1999/xhtml").open("body");
        b.open("script", "type", js).append("\n//<!--\n");
        b.append("window.close();");
        b.append("\n//-->").close();

        // send
        rsp.getOutputStream().println(b.finish());
        StreamUtil.close(rsp.getOutputStream());
    }

    protected IDCard getIds (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        // pull out session token from the request header
        String token = CookieUtil.getCookieValue(req, WebCreds.credsCookie());
        if (token == null) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return INVALID;
        }

        // make sure the user is authenticated, and pull out their record object
        MemberRecord member = _mhelper.getAuthedUser(token);
        if (member == null) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return INVALID;
        }

        String appIdStr = req.getParameter(FBParam.APP_ID.name);
        if (appIdStr == null) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return INVALID;
        }
        int appId;
        try {
            appId = Integer.parseInt(appIdStr);
        } catch (NumberFormatException ex) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return INVALID;
        }
        SessionInfo sinf = null;
        try {
            sinf = _fbLogic.loadSessionInfo(ExternalSiteId.facebookApp(appId), member);
        } catch (ServiceException ex) {
        }
        return new IDCard(appId, member.memberId, sinf == null ? 0L : sinf.fbid);
    }

    protected void trackSentInvites (HttpServletRequest req, IDCard sender, FacebookGame game)
    {
        if (DeploymentConfig.devDeployment) {
            MsoyHttpServer.dumpParameters(req);
        }

        // http://wiki.developers.facebook.com/index.php/Fb:request-form#POST_Variables
        String[] ids = req.getParameterValues("ids[]");
        log.info("Facebook invite complete", "ids", ids, "method", req.getMethod());

        // null means they pressed skip (not all that relevant for a FB connect app)
        if (ids == null) {
            return;
        }

        // fire off to Kontagent
        _tracker.trackInviteSent(sender.appId, sender.facebookID,
            req.getParameter(ArgNames.FBParam.TRACKING.name), ids);

        // TODO: record requests to mochi games too
        int gameId = (game != null && game.type == FacebookGame.Type.WHIRLED) ? game.getIntId() : 0;

        // report an invite sent for each id
        for (String id : ids) {
            _logger.gameInviteSent(gameId, sender.memberId, id, "facebook");
        }
    }

    /**
     * Pair of ids that can parsed once at the beginning of a request and passed around.
     */
    protected static class IDCard
    {
        public int memberId;
        public long facebookID;
        public int appId;

        public IDCard (int appId, int memberId, long fbUID)
        {
            this.appId = appId;
            this.memberId = memberId;
            this.facebookID = fbUID;
        }
    }

    // dependencies
    @Inject protected FacebookLogic _fbLogic;
    @Inject protected FacebookRepository _fbRepo;
    @Inject protected KontagentLogic _tracker;
    @Inject protected MemberHelper _mhelper;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _logger;
    @Inject protected MsoyGameRepository _mgameRepo;

    /** The facebook namespace url. */
    protected static final String FBMLNS = "http://www.facebook.com/2008/fbml";

    /** The javascript to initiate the facebook dynamic loading. */
    protected static final String FBLOADERCODE =
        "http://static.ak.connect.facebook.com/js/api_lib/v0.4/xxxxxFeatureLoader.js.php";

    /** Our static invite script, we give it all the relevant data. */
    protected static final String INVITE_JS = "/js/fbinvite.js";

    /** The id of our div tag where the facebook invite form goes. */
    protected static final String FORM_TAG_ID = "fbinviteform";

    /** The path to our cross domain file. */
    protected static final String RECEIVER_PATH = "/fbconnect.html";

    /** ID pair returned when the required parameters are not found. */
    protected static final IDCard INVALID = new IDCard(0, 0, 0L);
}
