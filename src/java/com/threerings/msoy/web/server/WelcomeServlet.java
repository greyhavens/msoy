//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.util.StringUtil;

/**
 * Handles requests to assign an affiliate cookie to a user:
 * /welcome/[affiliate]/[page_tokens_and_args]
 */
public class WelcomeServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String path = StringUtil.deNull(req.getPathInfo());
        // the path will now either be "", "/<affiliate>", or "/<affiliate>/<token>"
        if (path.startsWith("/")) {
            int nextSlash = path.indexOf("/", 1);
            String affiliate;
            if (nextSlash == -1) {
                affiliate = path.substring(1);
                path = "";
            } else {
                affiliate = path.substring(1, nextSlash);
                path = path.substring(nextSlash + 1);
            }
            // Set up the affiliate for this welcomed user.
            if (!StringUtil.isBlank(affiliate)) {
                AffiliateCookie.set(rsp, affiliate);
            }
        }

        rsp.sendRedirect("/#" + path);
    }
}
