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
 * /welcome?aff=<affiliate>&page=page_tokens_and_args
 */
public class WelcomeServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String affiliate = req.getParameter("aff");
        if (!StringUtil.isBlank(affiliate)) {
            AffiliateCookie.set(rsp, affiliate);
        }

        String page = StringUtil.deNull(req.getParameter("page"));
        rsp.sendRedirect("/#" + page);
    }
}
