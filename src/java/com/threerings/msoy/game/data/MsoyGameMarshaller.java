//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link MsoyGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoyGameMarshaller extends InvocationMarshaller
    implements MsoyGameService
{
    /**
     * Marshalls results to implementations of {@link MsoyGameService.LocationListener}.
     */
    public static class LocationMarshaller extends ListenerMarshaller
        implements LocationListener
    {
        /** The method id used to dispatch {@link #gameLocated}
         * responses. */
        public static final int GAME_LOCATED = 1;

        // from interface LocationMarshaller
        public void gameLocated (String arg1, int arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, GAME_LOCATED,
                               new Object[] { arg1, Integer.valueOf(arg2) }, transport));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case GAME_LOCATED:
                ((LocationListener)listener).gameLocated(
                    (String)args[0], ((Integer)args[1]).intValue());
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #inviteFriends} requests. */
    public static final int INVITE_FRIENDS = 1;

    // from interface MsoyGameService
    public void inviteFriends (Client arg1, int arg2, int[] arg3)
    {
        sendRequest(arg1, INVITE_FRIENDS, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #locateGame} requests. */
    public static final int LOCATE_GAME = 2;

    // from interface MsoyGameService
    public void locateGame (Client arg1, int arg2, MsoyGameService.LocationListener arg3)
    {
        MsoyGameMarshaller.LocationMarshaller listener3 = new MsoyGameMarshaller.LocationMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_GAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
