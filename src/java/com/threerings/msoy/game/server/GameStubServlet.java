//
// $Id$

package com.threerings.msoy.game.server;

import java.io.IOException;
import java.io.PrintStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;

import static com.threerings.msoy.Log.log;

/**
 * Returns information to an externally-embedded game stub instructing it how to load the
 * client for a particular game hosted in Whirled.
 */
public class GameStubServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        int gameId;
        try {
            gameId = Integer.parseInt(req.getParameter("gameId"));
        } catch (NumberFormatException nfe) {
            log.info("Received bad gameStub request: " + req.getQueryString());
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // TODO: massage the referer to just be "bla.com"
        String site = StringUtil.deNull(req.getHeader("Referer"));

        String response;
//        if (needToSendError) {
//            response = "<error>" + someErrorMessage + "</error>";
//
//        } else {
        String SEP = "&amp;";
            response = "<url>http://" + req.getServerName() + ":" + req.getServerPort() +
                "/clients/" + DeploymentConfig.version + "/world-client.swf</url>" +
                "<params>" +
                    //"guest=t" + SEP +
                    "vec=gamestub:" + gameId + ":" + site + SEP +
                    "gameLobby=" + gameId +
                "</params>";
//        }

        sendResponse(rsp, "<r>" + response + "</r>");
    }

    protected void sendResponse (HttpServletResponse rsp, String response)
        throws IOException
    {
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println(response);
        } finally {
            StreamUtil.close(out);
        }
    }
}
