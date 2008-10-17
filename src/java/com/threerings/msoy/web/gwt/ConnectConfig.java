//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Directs an applet to a particular server to connect (used by Swiftly and the Dashboard).
 */
public class ConnectConfig
    implements IsSerializable
{
    /** The server to which the applet should connect.  */
    public String server;

    /** The port on which the applet should connect to the server. */
    public int port;

    /** The HTTP port from which the applet should be downloaded. */
    public int httpPort;

    /**
     * Creates a URL that can be used to communicate directly to the server represented by this
     * connection config.
     */
    public String getURL (String path)
    {
        String port = (httpPort == 80) ? "" : (":" + httpPort);
        return "http://" + server + port + path;
    }
}
