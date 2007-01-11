//
// $Id$

package com.threerings.msoy.web.server;

import com.threerings.msoy.swiftly.client.SwiftlyProjectRpc;
import com.threerings.msoy.swiftly.server.SwiftlyProjectRpcImpl;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Provides the server implementation the Swiftly editor.
 */
public class SwiftlyEditorServlet extends XmlRpcServlet
{
    /**
     * Provides the Xml-Rpc method handler mapping.
     */
    protected XmlRpcHandlerMapping newXmlRpcHandlerMapping()
        throws XmlRpcException
    {
        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        phm.addHandler(SwiftlyProjectRpc.class.getName(), SwiftlyProjectRpcImpl.class);
        return phm;
    }

    public static final String SVC_PATH = "swiftlyeditsvc";
}
