//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.LobbyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link LobbyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class LobbyMarshaller extends InvocationMarshaller
    implements LobbyService
{
    /** The method id used to dispatch {@link #identifyLobby} requests. */
    public static final int IDENTIFY_LOBBY = 1;

    // from interface LobbyService
    public void identifyLobby (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, IDENTIFY_LOBBY, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #joinPlayerGame} requests. */
    public static final int JOIN_PLAYER_GAME = 2;

    // from interface LobbyService
    public void joinPlayerGame (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_PLAYER_GAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
