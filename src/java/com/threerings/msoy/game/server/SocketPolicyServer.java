package com.threerings.msoy.game.server;

import java.io.BufferedOutputStream;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import com.samskivert.util.LoopingThread;

import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * A class that listens to "XMLSOCKET" requests from Flash clients. These are straight TCP
 * connections, where the client initiates with a <policy-file-request/> line and the server
 * returns a bit of XML -- in fact, we can just send the XML right away.
 */
public class SocketPolicyServer extends LoopingThread
{
    /** Create a new {@link SocketPolicyServer} that listens on the given port. */
    public SocketPolicyServer (int socketPolicyPort)
    {
        _socketPolicyPort = socketPolicyPort;
    }

    /** Close down this service. */
    public void shutdown ()
    {
        if (_serverSocket != null) {
            try {
                _serverSocket.close();
            } catch (IOException e) {
                // we don't care
            }
        }

        super.shutdown();
    }

    @Override // from LoopingThread
    protected void willStart ()
    {
        String publicServerHost;
        try {
            URL url = new URL(ServerConfig.getServerURL());
            publicServerHost = url.getHost();
        } catch (Exception e) {
            log.warning("Failed to parse server_url " + e + ".");
            publicServerHost = ServerConfig.serverHost;
        }

        // build the XML once and for all
        StringBuilder policyFile = new StringBuilder().
            append("<?xml version=\"1.0\"?>\n").
            append("<cross-domain-policy>\n").
            append("  <allow-access-from domain=\"").append(publicServerHost).append("\"").
            append(" to-ports=\"");

        // allow Flash connections on our server & game ports
        for (int port : ServerConfig.serverPorts) {
            policyFile.append(port).append(",");
        }
        policyFile.append(ServerConfig.gameServerPort).append("\"/>\n").
        append("</cross-domain-policy>\n");

        // the default character set should suffice
        _bytes = policyFile.toString().getBytes();

        try {
            // finally open the socket
            _serverSocket = new ServerSocket(_socketPolicyPort);
            log.info("Socket Policy server running [allowed=" + publicServerHost +
                     ", port=" + _socketPolicyPort + "].");
        } catch (IOException e) {
            log.warning("Could not listen on port: " + _socketPolicyPort);
            shutdown();
        }
    }

    @Override // from LoopingThread
    protected void iterate ()
    {
        try {
            // wait for a connection
            Socket clientSocket = _serverSocket.accept();

            // send it some data
            BufferedOutputStream stream = new BufferedOutputStream(clientSocket.getOutputStream());
            stream.write(_bytes);

            // flush and close
            stream.flush();
            clientSocket.close();

        } catch (IOException e) {
            // we don't care, we're probably shutting down
        }
    }

    protected int _socketPolicyPort;
    protected byte[] _bytes;
    protected ServerSocket _serverSocket;
}
