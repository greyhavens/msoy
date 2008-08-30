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
    /** The method id used to dispatch {@link #cancelQuest} requests. */
    public static final int CANCEL_QUEST = 1;

    // from interface AVRGameService
    public void cancelQuest (Client arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, CANCEL_QUEST, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #completeQuest} requests. */
    public static final int COMPLETE_QUEST = 2;

    // from interface AVRGameService
    public void completeQuest (Client arg1, String arg2, float arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, COMPLETE_QUEST, new Object[] {
            arg2, Float.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #setTicker} requests. */
    public static final int SET_TICKER = 3;

    // from interface AVRGameService
    public void setTicker (Client arg1, String arg2, int arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_TICKER, new Object[] {
            arg2, Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #startQuest} requests. */
    public static final int START_QUEST = 4;

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
    public static final int UPDATE_QUEST = 5;

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
