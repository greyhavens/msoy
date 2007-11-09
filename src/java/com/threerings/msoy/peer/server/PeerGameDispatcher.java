//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.peer.client.PeerGameService;
import com.threerings.msoy.peer.data.PeerGameMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PeerGameProvider}.
 */
public class PeerGameDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PeerGameDispatcher (PeerGameProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new PeerGameMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PeerGameMarshaller.PEER_LEAVE_AVRGAME:
            ((PeerGameProvider)provider).peerLeaveAVRGame(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case PeerGameMarshaller.PEER_REPORT_FLOW_AWARD:
            ((PeerGameProvider)provider).peerReportFlowAward(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue()
            );
            return;

        case PeerGameMarshaller.PEER_UPDATE_PLAYER:
            ((PeerGameProvider)provider).peerUpdatePlayer(
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
