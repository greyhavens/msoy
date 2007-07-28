//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Represents a hosted game lobby on a particular server.
 */
public class HostedGame extends HostedPlace
{
    /** The port on which this game server is listening. */
    public Integer port;

    /**
     * Empty constructor used for unserializing 
     */
    public HostedGame ()
    {
    }

    /**
     * Creates a hosted game record.
     */
    public HostedGame (int placeId, String name, int port)
    {
        super(placeId, name);
        this.port = port;
    }
}
