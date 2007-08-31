package com.threerings.msoy.web.data;

/**
 * Wrap ConnectConfig with a SwiftlyProject.
 */
public class SwiftlyConnectConfig extends ConnectConfig
{
    public SwiftlyProject project;

    public SwiftlyConnectConfig ()
    {
        // for serialization
    }

    public SwiftlyConnectConfig (ConnectConfig config, SwiftlyProject project)
    {
        this.server = config.server;
        this.port = config.port;
        this.httpPort = config.httpPort;
        this.project = project;
    }
}
