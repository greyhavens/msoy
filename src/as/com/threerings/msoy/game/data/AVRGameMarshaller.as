//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.game.client.AVRGameService;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

/**
 * Provides the implementation of the {@link AVRGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRGameMarshaller extends InvocationMarshaller
    implements AVRGameService
{
    /** The method id used to dispatch {@link #joinAVRGame} requests. */
    public static const JOIN_AVRGAME :int = 1;

    // from interface AVRGameService
    public function joinAVRGame (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_AVRGAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #leaveAVRGame} requests. */
    public static const LEAVE_AVRGAME :int = 2;

    // from interface AVRGameService
    public function leaveAVRGame (arg1 :Client, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LEAVE_AVRGAME, [
            listener2
        ]);
    }

    /** The method id used to dispatch {@link #updateMemory} requests. */
    public static const UPDATE_MEMORY :int = 3;

    // from interface AVRGameService
    public function updateMemory (arg1 :Client, arg2 :MemoryEntry) :void
    {
        sendRequest(arg1, UPDATE_MEMORY, [
            arg2
        ]);
    }
}
}
