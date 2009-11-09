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
        String appIdStr = req.getParameter(ArgNames.THEME);
        int appId = (appIdStr != null) ? Integer.parseInt(appIdStr) : 0;
        ThemeRecord themeRec = _themeRepo.loadTheme(appId);

        VelocityContext ctx = new VelocityContext();

        if (themeRec != null) {
            ctx.put("logoUrl", themeRec.toLogo().getMediaPath());
            ctx.put("backgroundColor", themeRec.backgroundColor);
        } else {
            ctx.put("logoUrl", DEFAULT_LOGO_URL);
            ctx.put("backgroundColor", DEFAULT_BACKGROUND_COLOR);
        }

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

    @Inject ThemeRepository _themeRepo;
    @Inject ThemeLogic _themeLogic;

    protected static final String DEFAULT_LOGO_URL = "/images/header/header_logo.png";
    protected static final int DEFAULT_BACKGROUND_COLOR = 0xFFFFFF;
}
