//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;

/**
 * Provides a way for the {@link MemberObject} and {@link PetObject} to obtain information from the
 * RoomManager when configuring their {@link OccupantInfo}.
 */
public interface RoomLocal
{
    /** Whether or not we should use static media. */
    public boolean useStaticMedia (MsoyBodyObject body);

    /** Whether or not we're a manager of this room. */
    public boolean isManager (MsoyBodyObject body);
}
