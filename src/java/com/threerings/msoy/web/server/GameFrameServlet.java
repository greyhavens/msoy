//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.ServerMessages;

/**
 * Handles a request to generate a page for playing a game, embedded-style.
 */
public class GameFrameServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        PrintWriter writer = rsp.getWriter();
        try {
            writer.print(_serverMsgs.getBundle("server").get("m.game_frame", 
                DeploymentConfig.serverURL, DeploymentConfig.version,
                req.getParameter("gameId"), req.getParameter("aff")));
            writer.flush();
        } finally {
            StreamUtil.close(writer);
        }
    }

    @Inject protected ServerMessages _serverMsgs;
}
