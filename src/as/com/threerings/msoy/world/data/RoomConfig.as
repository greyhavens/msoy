//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.world.client.RoomObjectController;

public class RoomConfig extends PlaceConfig
{
    // documentation inherited
    override public function createController () :PlaceController
    {
        return new RoomObjectController();
    }
}
}
