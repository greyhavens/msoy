package com.threerings.msoy.data {

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.client.SimpleChatController;

public class SimpleChatConfig extends PlaceConfig
{
    // documentation inherited
    public override function createController () :PlaceController
    {
        return new SimpleChatController();
    }
}
}
