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

        int version = 0;
        try {
            version = Integer.parseInt(req.getParameter("v"));
        } catch (NumberFormatException nfe) {
            // no worries
        }

        String response;
//        if (needToSendError) {
//            response = "<error>" + someErrorMessage + "</error>";
//
//        } else {
            response = "<url>http://" + req.getServerName() + ":" + req.getServerPort() +
                "/clients/" + DeploymentConfig.version + "/world-client.swf</url>" +
                "<params>" + getParams(req, gameId, version) + "</params>";
//        }

        sendResponse(rsp, "<r>" + response + "</r>");
    }

    /**
     * Get the parameters that should be passed to the whirled client by the stub.
     */
    protected String getParams (HttpServletRequest req, int gameId, int version)
    {
        final String SEP = "&amp;";
        String params = "gameLobby=" + gameId;

        // return a "vector" for old gamestubs (new stubs make the vector themselves)
        if (version == 0) {
            String site = StringUtil.deNull(req.getParameter("aff"));
            params += SEP + "vec=e." + StringUtil.encode(site) + ".games." + gameId;
        }

        return params;
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
