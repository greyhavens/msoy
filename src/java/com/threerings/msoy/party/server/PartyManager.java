//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

public class PartyManager
    implements PartyProvider
{
    // from interface PartyProvider
    public void bootMember (ClientObject caller, InvocationService.ConfirmListener listener)
    {
        listener.requestProcessed(); // TODO
    }

    // from interface PartyProvider
    public void leaveParty (ClientObject caller, InvocationService.ConfirmListener listener)
    {
        listener.requestProcessed(); // TODO
    }
}
