package com.threerings.msoy.data {

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.client.RoomController;

public class RoomConfig extends PlaceConfig
{
    // documentation inherited
    override public function createController () :PlaceController
    {
        return new RoomController();
    }
}
}
