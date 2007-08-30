//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Represents a hosted Swiftly project on a particular server.
 */
public class HostedProject extends HostedPlace
{
    /** The server to which the applet should connect.  */
    public String server;

    /** The port on which the applet should connect to the server. */
    public Integer port;

    /** The HTTP port from which the applet should be downloaded. */
    public int httpPort;

    /** The node currently hosting this game. */
    public String node;

    /**
     * Empty constructor used for unserializing
     */
    public HostedProject ()
    {
    }

    /**
     * Creates a hosted project record.
     */
    public HostedProject (SwiftlyProject project, ConnectConfig config, String node)
    {
        super(project.projectId, project.projectName);
        this.port = config.port;
        this.httpPort = config.httpPort;
        this.server = config.server;
        this.node = node;
    }

    /**
     * Return a ConnectConfig derived from this HostedProject
     */
    public ConnectConfig createConnectConfig ()
    {
        ConnectConfig config = new ConnectConfig();
        config.server = this.server;
        config.port = this.port;
        config.httpPort = this.httpPort;
        return config;
    }
}
