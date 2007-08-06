//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link GameServerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class GameServerMarshaller extends InvocationMarshaller
    implements GameServerService
{
    /** The method id used to dispatch {@link #clearGameHost} requests. */
    public static final int CLEAR_GAME_HOST = 1;

    // from interface GameServerService
    public void clearGameHost (Client arg1, int arg2, int arg3)
    {
        sendRequest(arg1, CLEAR_GAME_HOST, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #reportFlowAward} requests. */
    public static final int REPORT_FLOW_AWARD = 2;

    // from interface GameServerService
    public void reportFlowAward (Client arg1, int arg2, int arg3)
    {
        sendRequest(arg1, REPORT_FLOW_AWARD, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #sayHello} requests. */
    public static final int SAY_HELLO = 3;

    // from interface GameServerService
    public void sayHello (Client arg1, int arg2)
    {
        sendRequest(arg1, SAY_HELLO, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #updateGameInfo} requests. */
    public static final int UPDATE_GAME_INFO = 4;

    // from interface GameServerService
    public void updateGameInfo (Client arg1, int arg2, int arg3)
    {
        sendRequest(arg1, UPDATE_GAME_INFO, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3)
        });
    }
}
