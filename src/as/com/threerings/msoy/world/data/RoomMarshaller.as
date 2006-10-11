//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.util.*; // for Float, Integer, etc.

import com.threerings.msoy.world.client.RoomService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
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
    /** The method id used to dispatch {@link #editRoom} requests. */
    public static const EDIT_ROOM :int = 1;

    // from interface RoomService
    public function editRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, EDIT_ROOM, [
            listener2
        ]);
    }

    /** The method id used to dispatch {@link #updateRoom} requests. */
    public static const UPDATE_ROOM :int = 2;

    // from interface RoomService
    public function updateRoom (arg1 :Client, arg2 :Array, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_ROOM, [
            arg2, listener3
        ]);
    }
}
}
