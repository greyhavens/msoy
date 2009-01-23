//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.GameGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link GameGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class GameGameMarshaller extends InvocationMarshaller
    implements GameGameService
{
    /** The method id used to dispatch {@link #getTrophies} requests. */
    public static final int GET_TROPHIES = 1;

    // from interface GameGameService
    public void getTrophies (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_TROPHIES, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #removeDevelopmentTrophies} requests. */
    public static final int REMOVE_DEVELOPMENT_TROPHIES = 2;

    // from interface GameGameService
    public void removeDevelopmentTrophies (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, REMOVE_DEVELOPMENT_TROPHIES, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
