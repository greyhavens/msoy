//
// $Id$

package com.threerings.msoy.game.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.game.client.GameGameService;

/**
 * Provides the implementation of the {@link GameGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from GameGameService.java.")
public class GameGameMarshaller extends InvocationMarshaller<ClientObject>
    implements GameGameService
{
    /** The method id used to dispatch {@link #complainPlayer} requests. */
    public static final int COMPLAIN_PLAYER = 1;

    // from interface GameGameService
    public void complainPlayer (int arg1, String arg2)
    {
        sendRequest(COMPLAIN_PLAYER, new Object[] {
            Integer.valueOf(arg1), arg2
        });
    }

    /** The method id used to dispatch {@link #getTrophies} requests. */
    public static final int GET_TROPHIES = 2;

    // from interface GameGameService
    public void getTrophies (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_TROPHIES, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #removeDevelopmentTrophies} requests. */
    public static final int REMOVE_DEVELOPMENT_TROPHIES = 3;

    // from interface GameGameService
    public void removeDevelopmentTrophies (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(REMOVE_DEVELOPMENT_TROPHIES, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }
}
