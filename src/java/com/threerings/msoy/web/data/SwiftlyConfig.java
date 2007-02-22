//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains all the information needed to launch an instance of the Swiftly applet.
 */
public class SwiftlyConfig
    implements IsSerializable
{
    /** The server to which the applet should connect.  */
    public String server;

    /** The port on which the applet should connect to the server. */
    public int port;
}
