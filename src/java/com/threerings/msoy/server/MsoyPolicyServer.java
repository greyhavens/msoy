//
// $Id$

package com.threerings.msoy.server;

import java.io.IOException;
import java.net.URL;

import org.apache.mina.common.IoAcceptor;

import com.whirled.server.PolicyServer;

import static com.threerings.msoy.Log.log;

/**
 * A stand-alone policy server that we run on the production machines.
 */
public class MsoyPolicyServer extends PolicyServer
{
    /**
     * Entry point for the stand-alone server.
     */
    public static void main (String[] args) throws IOException
    {
        init();
    }

    /**
     * Initialize with all the msoy bits.
     */
    public static IoAcceptor init ()
        throws IOException
    {
        String publicHost;
        try {
            URL url = new URL(ServerConfig.getServerURL());
            publicHost = url.getHost();
        } catch (Exception e) {
            log.warning("Failed to parse server_url " + e + ".");
            publicHost = ServerConfig.serverHost;
        }

        // call the other init (in our superclass)
        return init(ServerConfig.socketPolicyPort, publicHost, ServerConfig.serverPorts,
            ServerConfig.gameServerPort);
    }

    // This constructor is never used, it's here to shut-up the compiler
    private MsoyPolicyServer ()
    {
        super(0, null, null, 0);
    }
}
