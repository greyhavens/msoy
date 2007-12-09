//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.Properties;

import com.samskivert.jdbc.depot.CacheAdapter;
import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;

import com.threerings.presents.client.Client;

import static com.threerings.msoy.Log.log;

/**
 * Provides access to installation specific configuration. Properties that
 * are specific to a particular Bang! server installation are accessed via
 * this class.
 */
public class ServerConfig
{
    /** The name assigned to this server node. */
    public static String nodeName;

    /** The root directory of the server installation. */
    public static File serverRoot;

    /** The publicly accessible DNS name of the host on which this server is running. */
    public static String serverHost;

    /** The back-channel DNS name of the host on which this server is running. */
    public static String backChannelHost;

    /** The ports on which we are listening for client connections. */
    public static int[] serverPorts;

    /** The port on which we are listening for HTTP connections. */
    public static int httpPort;

    /** The port on which game servers start listening. */
    public static int gameServerPort;

    /** The secret used to authenticate other servers in our cluster. */
    public static String sharedSecret;

    /** The local directory where dictionary files are stored. */
    public static File dictionaryDir;

    /** The local directory into which uploaded media is stored. */
    public static File mediaDir;

    /** The URL from which we instruct clients to load their media. */
    public static String mediaURL;

    /** Enables S3 media storage. */
    public static boolean mediaS3Enable;

    /** The remote S3 bucket in which media is stored. */
    public static String mediaS3Bucket;

    /** The user ID used for S3 authentication. */
    public static String mediaS3Id;

    /** The secret key used for S3 authentication. */
    public static String mediaS3Key;

    /** Event logging server RPC URL. */
    public static String eventLogURL;

    /** The port on which we listen to socket policy requests. */
    public static int socketPolicyPort;

    /** Provides access to our config properties. <em>Do not</em> modify
     * these properties! */
    public static Config config = new Config("server");

    /**
     * Returns the JDBC configuration.
     */
    public static Properties getJDBCConfig ()
    {
        return config.getSubProperties("db");
    }

    /**
     * Creates the Depot cache that should be used by this server and its tools. May return null,
     * in which case no caching is used.
     */
    public static CacheAdapter createCacheAdapter ()
        throws Exception
    {
        CacheAdapter cacheAdapter = null;
        String adapterName = config.getValue("depot.cache.adapter", "");
        if (adapterName.length() > 0) {
            Class<?> adapterClass = Class.forName(adapterName);
            if (adapterClass != null) {
                if (CacheAdapter.class.isAssignableFrom(adapterClass)) {
                    log.info("Using cache manager: " + adapterClass);
                    cacheAdapter = (CacheAdapter) adapterClass.newInstance();
                } else {
                    log.warning("Configured with invalid CacheAdapter: " + adapterName + ".");
                }
            }
        }
        return cacheAdapter;
    }

    /**
     * Returns a URL that can be used to make HTTP requests from this server.
     */
    public static String getServerURL ()
    {
        String defurl = "http://" + serverHost + (httpPort != 80 ? ":" + httpPort : "");
        return config.getValue("server_url", defurl);
    }

    /**
     * Returns the address from which automated emails are sent.
     */
    public static String getFromAddress ()
    {
        return "\"Whirled Mailbot\" <peas@whirled.com>"; // TODO: move this to the server config
    }

    /**
     * Returns the HTTP port on which the specified node should be listening.
     */
    public static int getHttpPort (String nodeName)
    {
        return config.getValue(nodeName + ".http_port", httpPort);
    }

    /**
     * Configures server bits when this class is resolved.
     */
    static {
        // these will be overridden if we're running in cluster mode
        serverHost = config.getValue("server_host", "localhost");
        serverPorts = config.getValue("server_ports", Client.DEFAULT_SERVER_PORTS);
        httpPort = config.getValue("http_port", 8080);
        gameServerPort = config.getValue("game_server_port", 47625);
        socketPolicyPort = config.getValue("socket_policy_port", 47623);

        // if we're a server node (not the webapp or a tool) do some extra stuff
        if (Boolean.getBoolean("is_node")) {
            // our node name and hostname come from system properties passed by our startup scripts
            nodeName = System.getProperty("node");
            if (StringUtil.isBlank(nodeName)) {
                log.warning("Missing 'node' system property. Cannot start.");
            }
            backChannelHost = System.getProperty("hostname");
            if (StringUtil.isBlank(backChannelHost)) {
                log.warning("Missing 'hostname' system property. Cannot start.");
            }
            if (StringUtil.isBlank(nodeName) || StringUtil.isBlank(backChannelHost)) {
                System.exit(-1);
            }

            // fill in our node-specific properties
            serverHost = config.getValue(nodeName + ".server_host", serverHost);
            serverPorts = config.getValue(nodeName + ".server_ports", serverPorts);
            httpPort = config.getValue(nodeName + ".http_port", httpPort);
            gameServerPort = config.getValue(nodeName + ".game_server_port", gameServerPort);
            socketPolicyPort = config.getValue(nodeName + ".socket_policy_port", socketPolicyPort);
        }

        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        mediaDir = new File(config.getValue("media_dir", "/tmp"));
        mediaURL = config.getValue("media_url", "http://localhost:" + httpPort + "/media");
        mediaS3Enable = config.getValue("media_s3enable", false);
        mediaS3Bucket = config.getValue("media_s3bucket", "msoy");
        mediaS3Id = config.getValue("media_s3id", "id");
        mediaS3Key = config.getValue("media_s3key", "key");
        sharedSecret = config.getValue("server_secret", "");
        eventLogURL = config.getValue("event_log_url", "http://localhost:8080");
    }
}
