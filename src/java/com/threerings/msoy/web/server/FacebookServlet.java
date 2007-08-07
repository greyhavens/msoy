//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.threerings.msoy.Log.log;

/**
 * Handles Facebook callback requests.
 */
public class FacebookServlet extends HttpServlet
{
    protected void doHead (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got HEAD request " + req.getRequestURL());
//         dumpParameters(req);
    }

    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got GET request " + req.getRequestURL());
        // TODO: swizzle their cookie if possible; auto creating an account for them if necessary
        rsp.sendRedirect("/facebook.html");
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got POST request " + req.getRequestURL());
//         dumpParameters(req);
    }

    protected void dumpParameters (HttpServletRequest req)
    {
        Enumeration iter = req.getParameterNames();
        while (iter.hasMoreElements()) {
            String pname = (String)iter.nextElement();
            for (String value : req.getParameterValues(pname)) {
                log.info("  " + pname + " -> " + value);
            }
        }
    }
}
