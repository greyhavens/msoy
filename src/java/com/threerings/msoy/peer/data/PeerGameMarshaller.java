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
    /** The method id used to dispatch {@link #gameRecordUpdated} requests. */
    public static final int GAME_RECORD_UPDATED = 1;

    // from interface PeerGameService
    public void gameRecordUpdated (Client arg1, int arg2)
    {
        sendRequest(arg1, GAME_RECORD_UPDATED, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #peerLeaveAVRGame} requests. */
    public static final int PEER_LEAVE_AVRGAME = 2;

    // from interface PeerGameService
    public void peerLeaveAVRGame (Client arg1, int arg2)
    {
        sendRequest(arg1, PEER_LEAVE_AVRGAME, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #peerReportFlowAward} requests. */
    public static final int PEER_REPORT_FLOW_AWARD = 3;

    // from interface PeerGameService
    public void peerReportFlowAward (Client arg1, int arg2, int arg3)
    {
        sendRequest(arg1, PEER_REPORT_FLOW_AWARD, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #peerUpdatePlayer} requests. */
    public static final int PEER_UPDATE_PLAYER = 4;

    // from interface PeerGameService
    public void peerUpdatePlayer (Client arg1, int arg2, GameSummary arg3)
    {
        sendRequest(arg1, PEER_UPDATE_PLAYER, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }
}
