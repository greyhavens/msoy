//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.AVRGameService;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

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
    public static final int JOIN_AVRGAME = 1;

    // from interface AVRGameService
    public void joinAVRGame (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_AVRGAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #leaveAVRGame} requests. */
    public static final int LEAVE_AVRGAME = 2;

    // from interface AVRGameService
    public void leaveAVRGame (Client arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LEAVE_AVRGAME, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #updateMemory} requests. */
    public static final int UPDATE_MEMORY = 3;

    // from interface AVRGameService
    public void updateMemory (Client arg1, MemoryEntry arg2)
    {
        sendRequest(arg1, UPDATE_MEMORY, new Object[] {
            arg2
        });
    }
}
