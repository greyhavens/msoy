//
// $Id$

package com.threerings.msoy.avrg.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.avrg.client.AVRService;

/**
 * Provides the implementation of the {@link AVRService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from AVRService.java.")
public class AVRMarshaller extends InvocationMarshaller<ClientObject>
    implements AVRService
{
    /**
     * Marshalls results to implementations of {@code AVRService.AVRGameJoinListener}.
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
            sendResponse(AVRG_JOINED, new Object[] { Integer.valueOf(arg1), arg2 });
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
    public void activateGame (int arg1, AVRService.AVRGameJoinListener arg2)
    {
        AVRMarshaller.AVRGameJoinMarshaller listener2 = new AVRMarshaller.AVRGameJoinMarshaller();
        listener2.listener = arg2;
        sendRequest(ACTIVATE_GAME, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #deactivateGame} requests. */
    public static final int DEACTIVATE_GAME = 2;

    // from interface AVRService
    public void deactivateGame (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(DEACTIVATE_GAME, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }
}
