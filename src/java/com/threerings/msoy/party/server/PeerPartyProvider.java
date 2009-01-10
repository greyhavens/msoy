//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.msoy.party.client.PeerPartyService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PeerPartyService}.
 */
public interface PeerPartyProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerPartyService#getPartyDetail} request.
     */
    void getPartyDetail (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
