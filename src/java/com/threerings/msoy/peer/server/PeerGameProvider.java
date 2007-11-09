//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.peer.client.PeerGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PeerGameService}.
 */
public interface PeerGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerGameService#peerLeaveAVRGame} request.
     */
    public void peerLeaveAVRGame (ClientObject caller, int arg1);

    /**
     * Handles a {@link PeerGameService#peerReportFlowAward} request.
     */
    public void peerReportFlowAward (ClientObject caller, int arg1, int arg2);

    /**
     * Handles a {@link PeerGameService#peerUpdatePlayer} request.
     */
    public void peerUpdatePlayer (ClientObject caller, int arg1, GameSummary arg2);
}
