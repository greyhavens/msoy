//
// $Id$

package com.threerings.msoy.web.server;


import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class SwiftlyServlet extends XmlRpcServlet
{
    /**
     * Provides the Xml-Rpc method handler mapping.
     */
    protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() {
        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        // TODO Map to classes
        return phm;
    }
}
