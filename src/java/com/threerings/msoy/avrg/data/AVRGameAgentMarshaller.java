//
// $Id$

package com.threerings.msoy.avrg.data;

import javax.annotation.Generated;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.avrg.client.AVRGameAgentService;

/**
 * Provides the implementation of the {@link AVRGameAgentService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from AVRGameAgentService.java.")
public class AVRGameAgentMarshaller extends InvocationMarshaller<ClientObject>
    implements AVRGameAgentService
{
    /** The method id used to dispatch {@link #leaveGame} requests. */
    public static final int LEAVE_GAME = 1;

    // from interface AVRGameAgentService
    public void leaveGame (int arg1)
    {
        sendRequest(LEAVE_GAME, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #roomSubscriptionComplete} requests. */
    public static final int ROOM_SUBSCRIPTION_COMPLETE = 2;

    // from interface AVRGameAgentService
    public void roomSubscriptionComplete (int arg1)
    {
        sendRequest(ROOM_SUBSCRIPTION_COMPLETE, new Object[] {
            Integer.valueOf(arg1)
        });
    }
}
