//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Represents a hosted room on a particular server.
 */
public class HostedRoom extends HostedPlace
{
    /** Access control information. See {@link MsoySceneModel}. */
    public byte accessControl;

    /**
     * Empty constructor used for unserializing 
     */
    public HostedRoom ()
    {
    }

    /**
     * Creates a hosted game record.
     */
    public HostedRoom (int placeId, String name, byte accessControl)
    {
        super(placeId, name);
        this.accessControl = accessControl;
    }
}
