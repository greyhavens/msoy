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
import com.threerings.msoy.game.server.persist.GameDetailRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.item.server.persist.GameRepository;

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
            // pull out session token from the request header
            String token = CookieUtil.getCookieValue(req, WebCreds.credsCookie());
            if (token == null) {
                rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // make sure the user is authenticated, and pull out their record object
            MemberRecord member = _mhelper.getAuthedUser(token);
            if (member == null) {
                rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
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
    
                GameDetailRecord gdr = _mgameRepo.loadGameDetail(gameId);
                String gameName = _gameRepo.loadItem(gdr.listedItemId).name;
                outputInvitePage(rsp, gameId, gameName, member.memberId, acceptPath);

            } else if (req.getRequestURI().equals("/fbinvite/done")) {
                String ids = req.getParameter("ids[]");
                log.info("Facebook invite complete", "ids", ids);

                // TODO: try overriding doPost... according to facebook docs, the ids, if present,
                // should be a comma separated list of ids of invited friends, but it is in fact
                // only a single id. However, all of their sample code uses method = 'POST', so
                // maybe that's what is required

                // TODO: record the number of invites sent

                // just close the window
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
            .put("acceptPath", DeploymentConfig.serverURL + "#" + 
                Pages.makeToken(Pages.WORLD, acceptPath))
            .put("acceptLabel", "Come Play " + gameName)
            .put("action", DeploymentConfig.serverURL + "fbinvite/done")
            .put("message", "Come And Play")
            .put("apiKey", ServerConfig.config.getValue("facebook.api_key", ""))
            .put("formElemId", FORM_TAG_ID)
            .put("gameName", gameName)
            .put("actionText", "Select some Faceook friends to come play " + gameName + " with you")
            .put("receiverPath", RECEIVER_PATH)
            .build();

        // html and javascript code to show a facebook invitation page
        MarkupBuilder b = new MarkupBuilder();
        String js = "text/javascript";
        b.open("html", "xmlns", "http://www.w3.org/1999/xhtml", "xmlns:fb", FBMLNS).open("body");
        b.open("script", "type", js, "src", FBLOADERCODE).append().close();
        b.open("script", "type", js, "src", INVITE_JS).append().close();
        b.open("div", "id", FORM_TAG_ID).append().close();
        b.open("script", "type", js).append("\n//<!--\n");
        String dataName = "whirledInviteData";
        b.append("var ").append(dataName).append(" = {};\n");
        for (Map.Entry<String, String> kvPair : vars.entrySet()) {
            b.append(dataName).append(".").append(kvPair.getKey()).append(" = \"")
             .append(kvPair.getValue()).append("\";\n");
        }
        b.append("FBInit(").append(dataName).append(");\n");
        b.append("FBShowInvite(").append(dataName).append(");");
        b.append("\n//-->").close();

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

    // dependencies
    @Inject protected MemberHelper _mhelper;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GameRepository _gameRepo;

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
