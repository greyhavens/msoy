//
// $Id$

package com.threerings.msoy.avrg.data;

import com.threerings.msoy.avrg.client.AVRGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

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
    /** The method id used to dispatch {@link #completeTask} requests. */
    public static final int COMPLETE_TASK = 1;

    // from interface AVRGameService
    public void completeTask (Client arg1, int arg2, String arg3, float arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, COMPLETE_TASK, new Object[] {
            Integer.valueOf(arg2), arg3, Float.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #loadOfflinePlayer} requests. */
    public static final int LOAD_OFFLINE_PLAYER = 2;

    // from interface AVRGameService
    public void loadOfflinePlayer (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOAD_OFFLINE_PLAYER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setOfflinePlayerProperty} requests. */
    public static final int SET_OFFLINE_PLAYER_PROPERTY = 3;

    // from interface AVRGameService
    public void setOfflinePlayerProperty (Client arg1, int arg2, String arg3, Object arg4, Integer arg5, boolean arg6, InvocationService.ConfirmListener arg7)
    {
        InvocationMarshaller.ConfirmMarshaller listener7 = new InvocationMarshaller.ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, SET_OFFLINE_PLAYER_PROPERTY, new Object[] {
            Integer.valueOf(arg2), arg3, arg4, arg5, Boolean.valueOf(arg6), listener7
        });
    }
}
