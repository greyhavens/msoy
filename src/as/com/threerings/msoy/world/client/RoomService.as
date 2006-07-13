//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.io.TypedArray;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.whirled.data.SceneUpdate;

/**
 * Service requests for rooms.
 */
public interface RoomService extends InvocationService
{
    /**
     * Request to apply the specified scene updates to the room.
     */
    function updateRoom (
            client :Client, updates :TypedArray,
            listener :InvocationService_InvocationListener)
            :void;
}
}
