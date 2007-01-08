//
// $Id$

package com.threerings.msoy.web.server;

import com.threerings.msoy.swiftly.server.SwiftlyProjectRpc;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Provides the server implementation the Swiftly editor.
 */
public class SwiftlyServlet extends XmlRpcServlet
{
    /**
     * Provides the Xml-Rpc method handler mapping.
     */
    protected XmlRpcHandlerMapping newXmlRpcHandlerMapping()
        throws XmlRpcException
    {
        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        phm.addHandler("project", SwiftlyProjectRpc.class);
        return phm;
    }
}
