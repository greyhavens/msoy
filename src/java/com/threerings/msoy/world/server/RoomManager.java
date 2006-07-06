//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

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
    @Override // documentation inherited
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

    @Override
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        // TODO
        return SpotSceneObject.class;
    }
}
