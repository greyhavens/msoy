//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.crowd.data.PlaceConfig;

/**
 * Probably temporary.
 */
public class RoomConfig extends PlaceConfig
{
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.world.server.RoomManager";
    }
}
