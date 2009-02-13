//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Represents a hosted game lobby on a particular server.
 */
public class HostedGame extends HostedPlace
{
    /**
     * Empty constructor used for unserializing
     */
    public HostedGame ()
    {
    }

    /**
     * Creates a hosted game record.
     */
    public HostedGame (int placeId, String name)
    {
        super(placeId, name);
        // nothing special currently
    }
}
