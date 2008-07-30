//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.msoy.game.client.LobbyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>LobbyService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class LobbyMarshaller extends InvocationMarshaller
    implements LobbyService
{
    /** The method id used to dispatch <code>identifyLobby</code> requests. */
    public static const IDENTIFY_LOBBY :int = 1;

    // from interface LobbyService
    public function identifyLobby (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, IDENTIFY_LOBBY, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>joinPlayerGame</code> requests. */
    public static const JOIN_PLAYER_GAME :int = 2;

    // from interface LobbyService
    public function joinPlayerGame (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_PLAYER_GAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>playNow</code> requests. */
    public static const PLAY_NOW :int = 3;

    // from interface LobbyService
    public function playNow (arg1 :Client, arg2 :int, arg3 :int, arg4 :InvocationService_ResultListener) :void
    {
        var listener4 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, PLAY_NOW, [
            Integer.valueOf(arg2), Integer.valueOf(arg3), listener4
        ]);
    }
}
}
