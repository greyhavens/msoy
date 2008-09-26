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
    /** The method id used to dispatch {@link #awardPrize} requests. */
    public static final int AWARD_PRIZE = 1;

    // from interface AVRGameService
    public void awardPrize (Client arg1, String arg2, int arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, AWARD_PRIZE, new Object[] {
            arg2, Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #awardTrophy} requests. */
    public static final int AWARD_TROPHY = 2;

    // from interface AVRGameService
    public void awardTrophy (Client arg1, String arg2, int arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, AWARD_TROPHY, new Object[] {
            arg2, Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #completeTask} requests. */
    public static final int COMPLETE_TASK = 3;

    // from interface AVRGameService
    public void completeTask (Client arg1, int arg2, String arg3, float arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, COMPLETE_TASK, new Object[] {
            Integer.valueOf(arg2), arg3, Float.valueOf(arg4), listener5
        });
    }
}
