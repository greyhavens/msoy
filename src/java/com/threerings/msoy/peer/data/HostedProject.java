//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.msoy.web.gwt.ConnectConfig;

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

    /**
     * Creates a key that can be used to look up a {@link HostedProject} in a DSet.
     */
    public static HostedProject makeKey (int projectId)
    {
        HostedProject key = new HostedProject();
        key.placeId = projectId;
        return key;
    }

    /**
     * Empty constructor used for unserializing
     */
    public HostedProject ()
    {
    }

    /**
     * Creates a hosted project record.
     */
    public HostedProject (SwiftlyProject project, ConnectConfig config)
    {
        super(project.projectId, project.projectName);
        this.port = config.port;
        this.httpPort = config.httpPort;
        this.server = config.server;
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
