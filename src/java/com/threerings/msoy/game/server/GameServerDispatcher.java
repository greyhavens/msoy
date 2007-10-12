//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.data.GameServerMarshaller;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link GameServerProvider}.
 */
public class GameServerDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public GameServerDispatcher (GameServerProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new GameServerMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case GameServerMarshaller.CLEAR_GAME_HOST:
            ((GameServerProvider)provider).clearGameHost(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue()
            );
            return;

        case GameServerMarshaller.LEAVE_AVRGAME:
            ((GameServerProvider)provider).leaveAVRGame(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case GameServerMarshaller.REPORT_FLOW_AWARD:
            ((GameServerProvider)provider).reportFlowAward(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue()
            );
            return;

        case GameServerMarshaller.SAY_HELLO:
            ((GameServerProvider)provider).sayHello(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case GameServerMarshaller.UPDATE_PLAYER:
            ((GameServerProvider)provider).updatePlayer(
                source,
                ((Integer)args[0]).intValue(), (GameSummary)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
