//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.peer.client.PeerGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link PeerGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PeerGameMarshaller extends InvocationMarshaller
    implements PeerGameService
{
    /** The method id used to dispatch {@link #gameRecordUpdated} requests. */
    public static final int GAME_RECORD_UPDATED = 1;

    // from interface PeerGameService
    public void gameRecordUpdated (Client arg1, int arg2)
    {
        sendRequest(arg1, GAME_RECORD_UPDATED, new Object[] {
            Integer.valueOf(arg2)
        });
    }
}
