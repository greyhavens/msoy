//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.AVRGameService;
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
    /** The method id used to dispatch {@link #completeQuest} requests. */
    public static final int COMPLETE_QUEST = 1;

    // from interface AVRGameService
    public void completeQuest (Client arg1, String arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, COMPLETE_QUEST, new Object[] {
            arg2, Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #deletePlayerProperty} requests. */
    public static final int DELETE_PLAYER_PROPERTY = 2;

    // from interface AVRGameService
    public void deletePlayerProperty (Client arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DELETE_PLAYER_PROPERTY, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #deleteProperty} requests. */
    public static final int DELETE_PROPERTY = 3;

    // from interface AVRGameService
    public void deleteProperty (Client arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DELETE_PROPERTY, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #setPlayerProperty} requests. */
    public static final int SET_PLAYER_PROPERTY = 4;

    // from interface AVRGameService
    public void setPlayerProperty (Client arg1, String arg2, byte[] arg3, boolean arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_PLAYER_PROPERTY, new Object[] {
            arg2, arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #setProperty} requests. */
    public static final int SET_PROPERTY = 5;

    // from interface AVRGameService
    public void setProperty (Client arg1, String arg2, byte[] arg3, boolean arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_PROPERTY, new Object[] {
            arg2, arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #startQuest} requests. */
    public static final int START_QUEST = 6;

    // from interface AVRGameService
    public void startQuest (Client arg1, String arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, START_QUEST, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #updateQuest} requests. */
    public static final int UPDATE_QUEST = 7;

    // from interface AVRGameService
    public void updateQuest (Client arg1, String arg2, int arg3, String arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, UPDATE_QUEST, new Object[] {
            arg2, Integer.valueOf(arg3), arg4, listener5
        });
    }
}
