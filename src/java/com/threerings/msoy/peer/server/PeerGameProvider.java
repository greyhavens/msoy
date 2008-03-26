//
// $Id$

package com.threerings.msoy.peer.server;

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
     * Handles a {@link PeerGameService#gameRecordUpdated} request.
     */
    public void gameRecordUpdated (ClientObject caller, int arg1);
}
