//
// $Id$

package com.threerings.msoy.game.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.game.client.WorldGameService;

/**
 * Provides the implementation of the {@link WorldGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from WorldGameService.java.")
public class WorldGameMarshaller extends InvocationMarshaller<ClientObject>
    implements WorldGameService
{
    /**
     * Marshalls results to implementations of {@code WorldGameService.LocationListener}.
     */
    public static class LocationMarshaller extends ListenerMarshaller
        implements LocationListener
    {
        /** The method id used to dispatch {@link #gameLocated}
         * responses. */
        public static final int GAME_LOCATED = 1;

        // from interface LocationMarshaller
        public void gameLocated (String arg1, int arg2, boolean arg3)
        {
            sendResponse(GAME_LOCATED, new Object[] { arg1, Integer.valueOf(arg2), Boolean.valueOf(arg3) });
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case GAME_LOCATED:
                ((LocationListener)listener).gameLocated(
                    (String)args[0], ((Integer)args[1]).intValue(), ((Boolean)args[2]).booleanValue());
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #getTablesWaiting} requests. */
    public static final int GET_TABLES_WAITING = 1;

    // from interface WorldGameService
    public void getTablesWaiting (InvocationService.ResultListener arg1)
    {
        InvocationMarshaller.ResultMarshaller listener1 = new InvocationMarshaller.ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_TABLES_WAITING, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #inviteFriends} requests. */
    public static final int INVITE_FRIENDS = 2;

    // from interface WorldGameService
    public void inviteFriends (int arg1, int[] arg2)
    {
        sendRequest(INVITE_FRIENDS, new Object[] {
            Integer.valueOf(arg1), arg2
        });
    }

    /** The method id used to dispatch {@link #locateGame} requests. */
    public static final int LOCATE_GAME = 3;

    // from interface WorldGameService
    public void locateGame (int arg1, WorldGameService.LocationListener arg2)
    {
        WorldGameMarshaller.LocationMarshaller listener2 = new WorldGameMarshaller.LocationMarshaller();
        listener2.listener = arg2;
        sendRequest(LOCATE_GAME, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }
}
