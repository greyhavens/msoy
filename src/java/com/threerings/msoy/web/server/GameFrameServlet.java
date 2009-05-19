//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintWriter;

import java.text.MessageFormat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.io.StreamUtil;

import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Handles a request to generate a page for playing a game, embedded-style.
 *
 * Non-optional parameters:
 *   gameId - The id of the game to serve
 *   aff    - The id of the affiliate for whom this page is being generated.
 */
public class GameFrameServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        PrintWriter writer = rsp.getWriter();
        try {
            writer.print(MessageFormat.format(BODY,
                DeploymentConfig.serverURL, DeploymentConfig.version,
                req.getParameter("gameId"), req.getParameter("aff")));
            writer.flush();
        } finally {
            StreamUtil.close(writer);
        }
    }

    /** The body we'll output.
     * {0} == server url
     * {1} == client version
     * {2} == gameId
     * {3} == affiliate
     */
    protected static final String BODY = "<body><object width=''100%'' height=''575'' classid=''clsid:d27cdb6e-ae6d-11cf-96b8-444553540000'' codebase=''http://active.macromedia.com/flash7/cabs/swflash.cab#version=10,0,0,0'' allowScriptAccess=''always''><param name=''movie'' value=''{0}clients/{1}/world-client.swf''/><param name=''wmode'' value=''opaque''/><param name=''FlashVars'' value=''gameId={2}&vec=e.whirled.games.{2}&aff={3}''/><embed width=''100%'' height=''575'' flashvars=''gameId={2}&vec=e.whirled.games.{2}&aff={3}'' src=''{0}clients/{1}/world-client.swf'' allowScriptAccess=''always'' wmode=''opaque'' pluginspace=''http://www.macromedia.com/go/getflashplayer'' ntype=''application/x-shockwave-flash''></embed></object></body>";
}
