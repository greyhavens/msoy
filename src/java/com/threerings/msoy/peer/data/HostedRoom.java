//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Represents a hosted room on a particular server.
 */
public class HostedRoom extends HostedPlace
{
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
    public HostedRoom (int placeId, String name, int ownerId, byte ownerType, byte accessControl)
    {
        super(placeId, name);
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.accessControl = accessControl;
    }
}
