//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.game.data.GameSummary;
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
    /** The method id used to dispatch {@link #leaveAVRGame} requests. */
    public static final int LEAVE_AVRGAME = 1;

    // from interface PeerGameService
    public void leaveAVRGame (Client arg1, int arg2)
    {
        sendRequest(arg1, LEAVE_AVRGAME, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #reportFlowAward} requests. */
    public static final int REPORT_FLOW_AWARD = 2;

    // from interface PeerGameService
    public void reportFlowAward (Client arg1, int arg2, int arg3)
    {
        sendRequest(arg1, REPORT_FLOW_AWARD, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #updatePlayer} requests. */
    public static final int UPDATE_PLAYER = 3;

    // from interface PeerGameService
    public void updatePlayer (Client arg1, int arg2, GameSummary arg3)
    {
        sendRequest(arg1, UPDATE_PLAYER, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }
}
