//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Directs an applet to a particular server to connect (used by Swiftly and the Dashboard).
 */
public class ConnectConfig
    implements IsSerializable
{
    /** The server to which the applet should connect.  */
    public String server;

    /** The port on which the applet should connect to the server. */
    public int port;
}
