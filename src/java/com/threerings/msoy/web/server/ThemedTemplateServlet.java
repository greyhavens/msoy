//
// $Id: AppInserterServlet.java 18229 2009-10-01 18:19:09Z jamie $

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.mortbay.jetty.servlet.DefaultServlet;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.StreamUtil;
import com.samskivert.velocity.VelocityUtil;

import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.server.ThemeLogic;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.web.gwt.ArgNames;

@Singleton
public class ThemedTemplateServlet extends DefaultServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        String themeIdStr = req.getParameter(ArgNames.THEME);
        int themeId = (themeIdStr != null) ? Integer.parseInt(themeIdStr) : 0;
        ThemeRecord themeRec = (themeId != 0) ? _themeRepo.loadTheme(themeId) : null;

        Theme theme = (themeRec != null) ? themeRec.toTheme(null) : Theme.DEFAULT_THEME;

        VelocityContext ctx = new VelocityContext();
        ctx.put("logoUrl", theme.getLogo().getMediaPath());
        ctx.put("navUrl", theme.getNavButton().getMediaPath());
        ctx.put("navSelUrl", theme.getNavSelButton().getMediaPath());
        ctx.put("backgroundColor", hexColor(theme.backgroundColor));

        try {
            PrintWriter pout = new PrintWriter(rsp.getOutputStream());
            VelocityEngine ve = VelocityUtil.createEngine();
            String URI = req.getRequestURI();
            ve.mergeTemplate("rsrc" + URI + ".tmpl", "UTF-8", ctx, pout);
            StreamUtil.close(pout);

        } catch (Exception ex) {
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected static String hexColor (int rgb)
    {
        String str = ("000000" + Integer.toHexString(rgb));
        return str.substring(str.length() - 6);
    }

    @Inject ThemeRepository _themeRepo;
    @Inject ThemeLogic _themeLogic;

}
