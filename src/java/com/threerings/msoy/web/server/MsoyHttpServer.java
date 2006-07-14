//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.IOException;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;

import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Handles HTTP requests made of the Msoy server by the AJAX client and other
 * entities.
 */
public class MsoyHttpServer extends HttpServer
{
    /**
     * Creates and prepares our HTTP server for operation but does not yet
     * start listening on the HTTP port.
     */
    public MsoyHttpServer ()
    {
        // TODO: tone down the default verbose logging

        // wire up our GWT servlets
        HttpContext context = getContext("/user");
        ServletHandler handler= new ServletHandler();
        handler.addServlet("user", "/*", WebUserServlet.class.getName());
        context.addHandler(handler);

        // wire up serving of static content (for testing)
        context = getContext("/");
        context.setResourceBase(
            new File(ServerConfig.serverRoot, "pages").getPath());
        context.addHandler(new ResourceHandler());
    }

    /**
     * Initializes our HTTP server and begins listening for connections.
     */
    public void init ()
        throws Exception
    {
        // listen for connections on our preferred port
        addListener(":" + ServerConfig.getHttpPort());
        start();
        log.info("Listening for HTTP connections on port " +
                 ServerConfig.getHttpPort());
    }
}
