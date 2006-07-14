//
// $Id$

package com.threerings.msoy.server;

import org.mortbay.http.HttpServer;

import java.io.IOException;

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
    }

    /**
     * Initializes our HTTP server and begins listening for connections.
     */
    public void init ()
        throws IOException
    {
        // listen for connections on our preferred port
        addListener(":" + ServerConfig.getHttpPort());
        log.info("Listening for HTTP connections on port " +
                 ServerConfig.getHttpPort());
    }
}
