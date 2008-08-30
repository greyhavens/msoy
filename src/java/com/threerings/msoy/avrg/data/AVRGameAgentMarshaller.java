//
// $Id$

package com.threerings.msoy.avrg.data;

import com.threerings.msoy.avrg.client.AVRGameAgentService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link AVRGameAgentService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRGameAgentMarshaller extends InvocationMarshaller
    implements AVRGameAgentService
{
    /** The method id used to dispatch {@link #leaveGame} requests. */
    public static final int LEAVE_GAME = 1;

    // from interface AVRGameAgentService
    public void leaveGame (Client arg1, int arg2)
    {
        sendRequest(arg1, LEAVE_GAME, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #roomSubscriptionComplete} requests. */
    public static final int ROOM_SUBSCRIPTION_COMPLETE = 2;

    // from interface AVRGameAgentService
    public void roomSubscriptionComplete (Client arg1, int arg2)
    {
        sendRequest(arg1, ROOM_SUBSCRIPTION_COMPLETE, new Object[] {
            Integer.valueOf(arg2)
        });
    }
}
