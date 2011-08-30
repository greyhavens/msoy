//
// $Id$

package com.threerings.msoy.party.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.msoy.party.client.PeerPartyService;

/**
 * Defines the server-side of the {@link PeerPartyService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PeerPartyService.java.")
public interface PeerPartyProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerPartyService#getPartyDetail} request.
     */
    void getPartyDetail (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
