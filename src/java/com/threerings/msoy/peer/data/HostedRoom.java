//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Represents a hosted room on a particular server.
 */
public class HostedRoom extends HostedPlace
{
    /** Mog id, to find out if a peer transition also involves moving into or out of a Mog. */
    public int mogId;

    /** Room owner id, used for access control. */
    public int ownerId;

    /** Owner type used for access control. */
    public byte ownerType;

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
    public HostedRoom (int placeId, String name, int mogId, int ownerId,
        byte ownerType, byte accessControl)
    {
        super(placeId, name);

        this.mogId = mogId;
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.accessControl = accessControl;
    }
}
