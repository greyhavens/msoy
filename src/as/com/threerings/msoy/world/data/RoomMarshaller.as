//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.TypedArray;

import com.threerings.msoy.world.client.RoomService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.whirled.data.SceneUpdate;

/**
 * Provides the implementation of the {@link RoomService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class RoomMarshaller extends InvocationMarshaller
    implements RoomService
{
    /** The method id used to dispatch {@link #updateRoom} requests. */
    public static const UPDATE_ROOM :int = 1;

    // documentation inherited from interface
    public function updateRoom (
            arg1 :Client, arg2 :TypedArray,
            arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_ROOM, [ arg2, listener3 ]);
    }

}
}
