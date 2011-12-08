//
// $Id$

package com.threerings.msoy.server;

import java.io.IOException;
import java.net.URL;

import com.whirled.server.PolicyServer;

import org.apache.mina.common.IoAcceptor;

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

    public static IoAcceptor init ()
        throws IOException
    {
        return init(null);
    }

    /**
     * Initialize with all the msoy bits.
     */
    public static IoAcceptor init (int[] otherNodePorts)
        throws IOException
    {
        int[] serverPorts;
        if (otherNodePorts == null) {
            serverPorts = ServerConfig.serverPorts;
        } else {
            int thisPortsLength = ServerConfig.serverPorts.length;
            serverPorts = new int[otherNodePorts.length + thisPortsLength];
            for (int ii = 0; ii < serverPorts.length; ii++) {
                if (ii < thisPortsLength) {
                    serverPorts[ii] = ServerConfig.serverPorts[ii];
                } else {
                    serverPorts[ii] = otherNodePorts[ii - thisPortsLength];
                }
            }
        }

        // call the other init (in our superclass)
        return init(ServerConfig.socketPolicyPort, "*", serverPorts,
            ServerConfig.gameServerPort);
    }

    // This constructor is never used, it's here to shut-up the compiler
    private MsoyPolicyServer ()
    {
        super(0, null, null, 0);
    }
}
