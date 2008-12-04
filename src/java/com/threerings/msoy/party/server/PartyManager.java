//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationManager;

import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.party.data.PartyObject;

public class PartyManager
    implements PartyProvider
{
    public PartyManager (PartyObject partyObj)
    {
        _partyObj = partyObj;

        _partyObj.setPartyService(_invmgr.registerDispatcher(new PartyDispatcher(this)));
    }

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

    // from interface PartyProvider
    public void assignLeader (
        ClientObject caller, int memberId, InvocationService.ConfirmListener listener)
    {
        listener.requestProcessed(); // TODO
    }

    protected PartyObject _partyObj;

//    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invmgr;
}
