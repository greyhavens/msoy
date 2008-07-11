//
// $Id$

package com.threerings.msoy.bureau.server;

import java.io.File;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.Config;

/**
 * Provides access to installation specific configuration. Properties that
 * are specific to a particular burl server installation are accessed via
 * this class.
 */
public class BureauLauncherConfig
{
    /** The root directory of the server installation. */
    public static File serverRoot;

    /** The DNS name of the host on which this server is running. */
    public static String serverHost;

    /** Provides access to our config properties. */
    public static Config config = new Config("burl-server");

    /** The secret used to authenticate the bureau launching client. */
    public static String bureauSharedSecret;

    /** True if the world/game servers will be restarting when code changes (and therefore 
     * probably not calling BureauLauncherSender.shutdown. */
    public static boolean worldServerWillAutoRestart;

    /**
     * Returns a provider of JDBC connections.
     */
    public static ConnectionProvider createConnectionProvider ()
        throws Exception
    {
        return new StaticConnectionProvider(config.getSubProperties("db"));
    }

    /**
     * Configures server bits when this class is resolved.
     */
    static {
        // these will be overridden if we're running in cluster mode
        serverHost = config.getValue("server_host", "localhost");

        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        bureauSharedSecret = config.getValue("bureau_secret", "");
        worldServerWillAutoRestart = config.getValue("world_server_will_auto_restart", false);
    }
}
