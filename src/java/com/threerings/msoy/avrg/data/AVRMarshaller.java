//
// $Id$

package com.threerings.msoy.avrg.data;

import com.threerings.msoy.avrg.client.AVRService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link AVRService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRMarshaller extends InvocationMarshaller
    implements AVRService
{
    /**
     * Marshalls results to implementations of {@link AVRService.AVRGameJoinListener}.
     */
    public static class AVRGameJoinMarshaller extends ListenerMarshaller
        implements AVRGameJoinListener
    {
        /** The method id used to dispatch {@link #avrgJoined}
         * responses. */
        public static final int AVRG_JOINED = 1;

        // from interface AVRGameJoinMarshaller
        public void avrgJoined (int arg1, AVRGameConfig arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, AVRG_JOINED,
                               new Object[] { Integer.valueOf(arg1), arg2 }, transport));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case AVRG_JOINED:
                ((AVRGameJoinListener)listener).avrgJoined(
                    ((Integer)args[0]).intValue(), (AVRGameConfig)args[1]);
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #activateGame} requests. */
    public static final int ACTIVATE_GAME = 1;

    // from interface AVRService
    public void activateGame (Client arg1, int arg2, AVRService.AVRGameJoinListener arg3)
    {
        AVRMarshaller.AVRGameJoinMarshaller listener3 = new AVRMarshaller.AVRGameJoinMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ACTIVATE_GAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #deactivateGame} requests. */
    public static final int DEACTIVATE_GAME = 2;

    // from interface AVRService
    public void deactivateGame (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DEACTIVATE_GAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
