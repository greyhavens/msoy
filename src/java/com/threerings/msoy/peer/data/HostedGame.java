//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Represents a hosted game on a particular server.
 */
public class HostedGame extends HostedPlace
{
    /** True if this game is an AVRG. */
    public boolean isAVRG;

    /**
     * Empty constructor used for unserializing
     */
    public HostedGame ()
    {
    }

    /**
     * Creates a hosted game record.
     */
    public HostedGame (int placeId, String name, boolean isAVRG)
    {
        super(placeId, name);
        this.isAVRG = isAVRG;
    }
}
