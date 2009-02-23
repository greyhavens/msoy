//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebUserService;

import static com.threerings.msoy.Log.log;

/**
 * Swizzles a session token into a browser cookie to effect a logon from a Flash embed. URLs are of
 * the following form: /swizzle/SESSION_TOKEN/page_token_and_args
 */
public class SwizzleServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String path = StringUtil.deNull(req.getPathInfo());
        String token = null;
        if (path.startsWith("/")) {
            int nextSlash = path.indexOf("/", 1);
            if (nextSlash == -1) {
                path = "";
            } else {
                token = path.substring(1, nextSlash);
                path = path.substring(nextSlash + 1);
            }
        }
        log.info("Swizzling!", "token", token, "path", path);
        if (token != null && !StringUtil.isBlank(token)) {
            Cookie cookie = new Cookie(WebCreds.credsCookie(), token);
            cookie.setMaxAge(WebUserService.SESSION_DAYS * 24*60*60);
            cookie.setPath("/");
            rsp.addCookie(cookie);
        }
        rsp.sendRedirect("/#" + path);
    }
}
