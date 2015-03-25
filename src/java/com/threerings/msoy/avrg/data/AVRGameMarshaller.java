//
// $Id$

package com.threerings.msoy.avrg.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.avrg.client.AVRGameService;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Provides the implementation of the {@link AVRGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from AVRGameService.java.")
public class AVRGameMarshaller extends InvocationMarshaller<ClientObject>
    implements AVRGameService
{
    /** The method id used to dispatch {@link #completeTask} requests. */
    public static final int COMPLETE_TASK = 1;

    // from interface AVRGameService
    public void completeTask (int arg1, String arg2, float arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(COMPLETE_TASK, new Object[] {
            Integer.valueOf(arg1), arg2, Float.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #loadOfflinePlayer} requests. */
    public static final int LOAD_OFFLINE_PLAYER = 2;

    // from interface AVRGameService
    public void loadOfflinePlayer (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(LOAD_OFFLINE_PLAYER, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #movePlayerToRoom} requests. */
    public static final int MOVE_PLAYER_TO_ROOM = 3;

    // from interface AVRGameService
    public void movePlayerToRoom (int arg1, int arg2, MsoyLocation arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(MOVE_PLAYER_TO_ROOM, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #setIdle} requests. */
    public static final int SET_IDLE = 4;

    // from interface AVRGameService
    public void setIdle (boolean arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_IDLE, new Object[] {
            Boolean.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #setOfflinePlayerProperty} requests. */
    public static final int SET_OFFLINE_PLAYER_PROPERTY = 5;

    // from interface AVRGameService
    public void setOfflinePlayerProperty (int arg1, String arg2, Object arg3, Integer arg4, boolean arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(SET_OFFLINE_PLAYER_PROPERTY, new Object[] {
            Integer.valueOf(arg1), arg2, arg3, arg4, Boolean.valueOf(arg5), listener6
        });
    }
}
