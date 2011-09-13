//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Represents a hosted room on a particular server.
 */
@com.threerings.util.ActionScript(omit=true)
public class HostedRoom extends HostedPlace
{
    /** Theme id, to find out if a peer transition also involves moving into or out of a Theme. */
    public int themeId;

    /** Room owner id, used for access control. */
    public int ownerId;

    /** Owner type used for access control. */
    public byte ownerType;

    /** Access control information. See {@link MsoySceneModel}. */
    public byte accessControl;

    /** Whether there are good DJs here. */
    public boolean hopping;

    /**
     * Empty constructor used for unserializing
     */
    public HostedRoom ()
    {
    }

    /**
     * Creates a hosted game record.
     */
    public HostedRoom (int placeId, String name, int themeId, int ownerId,
        byte ownerType, byte accessControl, boolean hopping)
    {
        super(placeId, name);

        this.themeId = themeId;
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.accessControl = accessControl;
        this.hopping = hopping;
    }
}
