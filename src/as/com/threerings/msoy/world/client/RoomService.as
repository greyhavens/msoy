//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.world.client.RoomService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.whirled.data.SceneUpdate;

/**
 * An ActionScript version of the Java RoomService interface.
 */
public interface RoomService extends InvocationService
{
    // from Java interface RoomService
    function updateRoom (arg1 :Client, arg2 :Array, arg3 :InvocationService_InvocationListener) :void;
}
}
