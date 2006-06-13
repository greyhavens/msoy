//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Manages a "Room".
 */
public class RoomManager extends SpotSceneManager
{
    // documentation inherited
    protected long idleUnloadPeriod ()
    {
        return 0L; // don't ever unload this place
    }

    // documentation inherited
    protected SceneLocation computeEnteringLocation (
            BodyObject body, Portal entry)
    {
        if (entry != null) {
            return super.computeEnteringLocation(body, entry);
        }

        // fallback if there is no portal
        return new SceneLocation(
            new MsoyLocation(0, 0, 0, (short) 0), body.getOid());
    }

    protected Class getPlaceObjectClass ()
    {
        // TODO
        return SpotSceneObject.class;
    }
}
