//
// $Id$

package com.threerings.msoy.web.server;

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

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.gwt.MarkupBuilder;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Handles requests to do facebook invites, which at the moment requires some backflips to get the
 * facebook XFBML elements stuck in the top-level html document.
 */
public class FacebookInviteServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        try {
            int memberId = getMemberId(req, rsp);
            if (memberId == 0) {
                return;
            }

            if (req.getRequestURI().equals("/fbinvite/do")) {
                String acceptPath = req.getParameter("path");
                if (acceptPath == null) {
                    rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
    
                int gameId;
                try {
                    String gameIdStr = req.getParameter("gameId");
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
                outputInvitePage(rsp, gameId, gameName, memberId, acceptPath);

            } else if (req.getRequestURI().equals("/fbinvite/done")) {
                logSentInvites(req, memberId);
                outputCloseWindowPage(rsp);

            } else {
                rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
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
        int memberId = getMemberId(req, rsp);
        if (memberId == 0) {
            return;
        }

        if (req.getRequestURI().equals("/fbinvite/done")) {
            logSentInvites(req, memberId);
            outputCloseWindowPage(rsp);

        } else {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void outputInvitePage (
        HttpServletResponse rsp, int gameId, String gameName, int memberId, String acceptPath)
        throws IOException
    {
        // sanitize the game name, just eliminate ", ', \, <, >
        // TODO: we could get complicated and try to support encoding here
        gameName = gameName.replace("\\", "").replace("\"", "").replace("<", "").replace(">", "");
        gameName = gameName.replace("'", "");

        // javascript variables - squirted below and referenced by the static script INVITE_JS
        Map<String, String> vars = new ImmutableMap.Builder<String, String>()
            .put("gameId", String.valueOf(gameId))
            .put("acceptPath", Pages.WORLD.makeFriendURL(memberId, acceptPath))
            .put("acceptLabel", "Play " + gameName)
            .put("action", DeploymentConfig.serverURL + "fbinvite/done?gameId=" + gameId)
            .put("message", "I'm playing " + gameName + " on Whirled, join me!")
            .put("apiKey", ServerConfig.config.getValue("facebook.api_key", ""))
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

    protected int getMemberId (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        // pull out session token from the request header
        String token = CookieUtil.getCookieValue(req, WebCreds.credsCookie());
        if (token == null) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return 0;
        }

        // make sure the user is authenticated, and pull out their record object
        MemberRecord member = _mhelper.getAuthedUser(token);
        if (member == null) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return 0;
        }

        return member.memberId;
    }

    protected void logSentInvites (HttpServletRequest req, int memberId)
    {
        String ids = req.getParameter("ids[]");
        log.info("Facebook invite complete", "ids", ids, "method", req.getMethod());

        // null means they pressed skip (not all that relevant for a FB connect app)
        if (ids == null) {
            return;
        }

        int gameId = 0;
        try {
            gameId = Integer.parseInt(req.getParameter("gameId"));
        } catch (Exception e) {
            log.warning("Failed to get game id of sent invites", e);
        }

        // report an invite sent for each id
        // TODO: why is facebook not giving us the promised comma-separated list of users ids?
        // http://wiki.developers.facebook.com/index.php/Fb:request-form#POST_Variables
        if (ids.length() > 0) {
            for (int pos, npos = 0; npos != -1; ) {
                pos = npos;
                npos = ids.indexOf(',', pos);
                String recip = npos == -1 ? ids.substring(pos) : ids.substring(pos, npos);
                _logger.gameInviteSent(gameId, memberId, recip, "facebook");
                if (npos != -1) {
                    ++npos;
                }
            }
        }
    }

    // dependencies
    @Inject protected MemberHelper _mhelper;
    @Inject protected MsoyEventLogger _logger;
    @Inject protected MsoyGameRepository _mgameRepo;

    /** The facebook namespace url. */
    protected static final String FBMLNS = "http://www.facebook.com/2008/fbml";

    /** The javascript to initiate the facebook dynamic loading. */
    protected static final String FBLOADERCODE =
        "http://static.ak.connect.facebook.com/js/api_lib/v0.4/FeatureLoader.js.php";

    /** Our static invite script, we give it all the relevant data. */
    protected static final String INVITE_JS = "/js/fbinvite.js"; 

    /** The id of our div tag where the facebook invite form goes. */
    protected static final String FORM_TAG_ID = "fbinviteform";

    /** The path to our cross domain file. */
    protected static final String RECEIVER_PATH = "/fbconnect.html";
}
