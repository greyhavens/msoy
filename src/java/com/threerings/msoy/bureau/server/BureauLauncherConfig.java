//
// $Id$

package com.threerings.msoy.bureau.server;

import java.io.File;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.Log;

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

    /** The secret passed to bureaus for them to use when opening connections to world servers. */
    public static String windowSharedSecret;

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
        serverHost = config.getValue("server_host", "");

        if (StringUtil.isBlank(serverHost)) {
            serverHost = System.getProperty("hostname");
            if (StringUtil.isBlank(serverHost)) {
                Log.log.warning("Neither server_host nor hostname is configured, using localhost");
                serverHost = "localhost";
            }
        }

        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        bureauSharedSecret = config.getValue("bureau_secret", "");
        windowSharedSecret = config.getValue("window_secret", "");
        worldServerWillAutoRestart = config.getValue("world_server_will_auto_restart", false);
    }
}
