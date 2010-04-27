//
// $Id: StatusServlet.java 16345 2009-04-30 22:59:38Z ray $

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.threerings.msoy.server.FunnelByDate;
import com.threerings.msoy.server.FunnelByVector;
import com.threerings.msoy.server.JSONReporter;

/**
 * Exports Panopticon-style JSON that's generated locally.
 */
public class JSONServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String path = req.getPathInfo();
        JSONReporter reporter;

        if (FUNNEL_BY_DATE_PATH.equals(path)) {
            reporter = _funnelByDate;
        } else if (FUNNEL_BY_VECTOR_PATH.equals(path)) {
            reporter = _funnelByVector;
        } else {
            throw new IllegalArgumentException("Unknown path: " + req.getPathInfo());
        }
        rsp.setContentType("application/json");

        PrintStream out = new PrintStream(rsp.getOutputStream());
        // If the requester wants this padded, do so:
        // http://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/
        String jsonCallback = req.getParameter("jsoncallback");
        if (jsonCallback != null) {
            out.append(jsonCallback + "(");
        }
        out.append(reporter.buildJSONReport());
        // End the padding if we started it above
        if (jsonCallback != null) {
            out.append(")");
        }
        out.flush();
        StreamUtil.close(out);
    }

    @Inject protected FunnelByDate _funnelByDate;
    @Inject protected FunnelByVector _funnelByVector;

    protected static final String FUNNEL_BY_DATE_PATH = "/funnel/date";
    protected static final String FUNNEL_BY_VECTOR_PATH = "/funnel/vector";
}
