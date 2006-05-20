package com.threerings.msoy.data {

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.client.RoomController;

public class RoomConfig extends PlaceConfig
{
    // documentation inherited
    public override function createController () :PlaceController
    {
        return new RoomController();
    }
}
}
