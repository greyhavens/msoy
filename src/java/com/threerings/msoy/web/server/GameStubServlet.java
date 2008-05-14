//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.io.StreamUtil;
import com.threerings.msoy.web.client.DeploymentConfig;

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
        int clientVersion;
        try {
            gameId = Integer.parseInt(req.getParameter("gameId"));
            clientVersion = Integer.parseInt(req.getParameter("v"));
        } catch (NumberFormatException nfe) {
            log.info("Received bad gameStub request: " + req.getQueryString());
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        sendResponse(rsp,
            "<r>" +
                "<url>http://" + req.getServerName() + ":" + req.getServerPort() + "/clients/" + DeploymentConfig.version + "/world-client.swf</url>" +
                "<flashVars guest='true' gameLobby='" + gameId + "'/>" +
            "</r>");
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
