//
// $Id$

package com.threerings.msoy.game.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.GameTraceLogEnumerationRecord;
import com.threerings.msoy.game.server.persist.GameTraceLogRecord;

import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.MemberHelper;

/**
 * Exports trace logs for server-side game components.
 *
 * TODO: This must be restricted to the developer(s?) of the game. Does that entail reading
 *       the source and listed items and checking against creatorId in those? We'll find out.
 */
public class GameTraceLogServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
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

            String gameIdStr = req.getParameter("gameId");
            if (gameIdStr == null) {
                rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            int gameId;
            try {
                gameId = Integer.parseInt(gameIdStr);
            } catch (NumberFormatException nfe) {
                rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String logIdStr = req.getParameter("logId");
            if (logIdStr == null) {
                enumerateLogs(rsp, gameId);
                return;
            }
            int logId;
            try {
                logId = Integer.parseInt(logIdStr);
                exportLog(rsp, logId, gameId);
            } catch (NumberFormatException nfe) {
                rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

        } catch (Exception e) {
            log.warning("Failed to export game trace log.", e);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    protected void exportLog (HttpServletResponse rsp, int logId, int gameId)
        throws IOException
    {
        GameTraceLogRecord record = _mgameRepo.loadTraceLog(logId);
        if (record == null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (record.gameId != gameId) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        rsp.getOutputStream().println(record.logData);
        StreamUtil.close(rsp.getOutputStream());
    }

    protected void enumerateLogs (HttpServletResponse rsp, int gameId)
        throws IOException
    {
        rsp.setContentType("text/html");
        rsp.getOutputStream().println("Logs for game [id=" + gameId + "]: ");
        for (GameTraceLogEnumerationRecord record : _mgameRepo.enumerateTraceLogs(gameId)) {
            rsp.getOutputStream().print(
                "<a href='/gamelogs?gameId=" + gameId + "&logId=" + record.logId + "'>" +
                record.logId + "</a>&nbsp;");
        }
        StreamUtil.close(rsp.getOutputStream());
    }

    // our dependencies
    @Inject protected MemberHelper _mhelper;
    @Inject protected MsoyGameRepository _mgameRepo;
}
