//
// $Id$

package com.threerings.msoy.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import static com.threerings.msoy.Log.log;

/**
 * A class that listens to "XMLSOCKET" requests from Flash clients. These are straight TCP
 * connections, where the client initiates with a <policy-file-request/> line and the server
 * returns a bit of XML -- in fact, we can just send the XML right away.
 */
public class PolicyServer extends IoHandlerAdapter
{
    /** Fire up a stand-alone server. */
    public static void main (String[] args) throws IOException
    {
        // The following two lines change the default buffer type to 'heap', which yields better
        // performance according to the Apache MINA documentation.
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());

        IoAcceptor acceptor = new SocketAcceptor();
        final SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.getFilterChain().addLast(
            "codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));

        int port = ServerConfig.socketPolicyPort;

        String publicServerHost;
        try {
            URL url = new URL(ServerConfig.getServerURL());
            publicServerHost = url.getHost();
        } catch (Exception e) {
            log.warning("Failed to parse server_url " + e + ".");
            publicServerHost = ServerConfig.serverHost;
        }

        acceptor.bind(new InetSocketAddress(port),
                      new PolicyServer(port, publicServerHost), cfg);
    }

    /** Create a new server instance. */
    public PolicyServer (int socketPolicyPort, String publicServerHost)
    {
        // build the XML once and for all
        StringBuilder policyBuilder = new StringBuilder().
            append("<?xml version=\"1.0\"?>\n").
            append("<cross-domain-policy>\n");
        // if we're running on 843, serve a master policy file
        if (socketPolicyPort == MASTER_PORT) {
            policyBuilder.append(
                " <site-control permitted-cross-domain-policies=\"master-only\"/>\n");
        }
        policyBuilder.append("  <allow-access-from domain=\"").append(publicServerHost).
            append("\"").append(" to-ports=\"");

        // allow Flash connections on our server & game ports
        for (int port : ServerConfig.serverPorts) {
            policyBuilder.append(port).append(",");
        }
        policyBuilder.append(ServerConfig.gameServerPort).append("\"/>\n").
            append("</cross-domain-policy>\n");

        _policy = policyBuilder.toString();
    }

    @Override
    public void messageReceived (IoSession session, Object msg) {
        // ignored
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status)
        throws Exception
    {
        session.close();
    }

    @Override
    public void sessionCreated(IoSession session)
        throws Exception
    {
        session.write(_policy);
        session.setIdleTime(IdleStatus.WRITER_IDLE, 1);
    }

    protected int _socketPolicyPort;
    protected String _policy;
    protected IoAcceptor _acceptor;

    protected static final int MASTER_PORT = 843;
}
