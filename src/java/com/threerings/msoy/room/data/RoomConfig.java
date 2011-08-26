//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.crowd.data.PlaceConfig;

/**
 * Probably temporary.
 */
@com.threerings.util.ActionScript(omit=true)
public class RoomConfig extends PlaceConfig
{
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.room.server.RoomManager";
    }
}
