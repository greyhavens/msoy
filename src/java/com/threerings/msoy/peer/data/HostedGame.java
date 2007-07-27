//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Represents a hosted game lobby on a particular server.
 */
public class HostedGame extends HostedPlace
{
    /** The oid of the LobbyObject on the host. */
    public Integer lobbyOid;

    /**
     * Empty constructor used for unserializing 
     */
    public HostedGame ()
    {
    }

    /**
     * Creates a hosted game record.
     */
    public HostedGame (int placeId, String name, int lobbyOid)
    {
        super(placeId, name);
        this.lobbyOid = lobbyOid;
    }
}
