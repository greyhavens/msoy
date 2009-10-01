//
// $Id$

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
import com.samskivert.util.StringUtil;
import com.samskivert.velocity.VelocityUtil;

import com.threerings.msoy.facebook.server.FacebookLogic;
import com.threerings.msoy.facebook.server.persist.FacebookInfoRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.web.gwt.ArgNames;

import static com.threerings.msoy.Log.log;

/**
 * Uses velocity to insert application parameters into specific files mapped by the http server.
 * This class does exactly what was needed for facebook.js, but could be extended to do more if and
 * when the requirement arises.
 */
@Singleton
public class AppInserterServlet extends DefaultServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        // TODO: if no app parameter is given, use the FB connect app
        String appIdStr = req.getParameter(ArgNames.APP);
        int appId = appIdStr != null ? Integer.parseInt(appIdStr) : 0;
        appId = appId == 0 ? _facebookLogic.getDefaultGamesSite().getFacebookAppId() : appId;
        FacebookInfoRecord fbInfo = _facebookRepository.loadAppFacebookInfo(appId);

        if (fbInfo == null || StringUtil.isBlank(fbInfo.apiKey)) {
            log.warning("Missing app or api key", "app", appIdStr);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        try {
            VelocityContext ctx = new VelocityContext();
            ctx.put("apiKey", fbInfo.apiKey);
            PrintWriter pout = new PrintWriter(rsp.getOutputStream());
            VelocityEngine ve = VelocityUtil.createEngine();
            ve.mergeTemplate("rsrc" + req.getRequestURI() + ".tmpl", "UTF-8", ctx, pout);
            StreamUtil.close(pout);

        } catch (Exception ex) {
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Inject FacebookLogic _facebookLogic;
    @Inject FacebookRepository _facebookRepository;
}
