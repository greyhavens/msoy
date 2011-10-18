//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mortbay.jetty.servlet.DefaultServlet;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.web.gwt.ArgNames;

/**
 * Serves a themed group's custom CSS.
 */
@Singleton
public class CustomCssServlet extends DefaultServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        String themeIdStr = req.getParameter(ArgNames.THEME);
        int themeId = (themeIdStr != null) ? Integer.parseInt(themeIdStr) : 0;
        ThemeRecord themeRec = (themeId != 0) ? _themeRepo.loadTheme(themeId) : null;
        MediaDesc cssMedia = (themeRec != null) ? themeRec.toCssMedia() : null;

        if (cssMedia != null) {
            // 302 redirect to the CSS
            rsp.sendRedirect(cssMedia.getMediaPath());
            System.out.println("======= Redirecting to: " + cssMedia.getMediaPath());
        } else {
            // Send an empty file
            rsp.getOutputStream().close();
        }
    }

    @Inject protected ThemeRepository _themeRepo;

}
