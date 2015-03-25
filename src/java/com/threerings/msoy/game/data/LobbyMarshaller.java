//
// $Id$

package com.threerings.msoy.game.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.game.client.LobbyService;

/**
 * Provides the implementation of the {@link LobbyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from LobbyService.java.")
public class LobbyMarshaller extends InvocationMarshaller<ClientObject>
    implements LobbyService
{
    /** The method id used to dispatch {@link #identifyLobby} requests. */
    public static final int IDENTIFY_LOBBY = 1;

    // from interface LobbyService
    public void identifyLobby (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(IDENTIFY_LOBBY, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #playNow} requests. */
    public static final int PLAY_NOW = 2;

    // from interface LobbyService
    public void playNow (int arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(PLAY_NOW, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), listener3
        });
    }
}
