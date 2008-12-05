//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.party.data.PartierEntry;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyObject;

public class PartyManager
    implements PartyProvider
{
    public PartyManager (PartyObject partyObj)
    {
        _partyObj = partyObj;

        _partyObj.setPartyService(_invmgr.registerDispatcher(new PartyDispatcher(this)));
    }

    /**
     * Add the specified player to the party.
     */
    public void addMate (MemberObject member, InvocationService.ResultListener rl)
        throws InvocationException
    {
        if (!canJoinParty(member)) {
            throw new InvocationException("TODO");
        }

        // figure out the highest yet-seen joinOrder
        int joinOrder = 0;
        for (PartierEntry partier : _partyObj.mates) {
            joinOrder = Math.max(joinOrder, partier.joinOrder + 1);
        }

        // go ahead and add them
        _partyObj.addToMates(new PartierEntry(member.memberName, joinOrder));
        member.setPartyId(_partyObj.id);

        // tell them the OID so they can subscribe
        rl.requestProcessed(_partyObj.getOid());
    }

    // from interface PartyProvider
    public void bootMember (ClientObject caller, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        listener.requestProcessed(); // TODO
    }

    // from interface PartyProvider
    public void leaveParty (ClientObject caller, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        listener.requestProcessed(); // TODO
    }

    // from interface PartyProvider
    public void assignLeader (
        ClientObject caller, int memberId, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        listener.requestProcessed(); // TODO
    }

    /**
     * Can the specified member join this party?
     */
    protected boolean canJoinParty (MemberObject member)
    {
        // TODO: allow players specifically invited by the leader

        switch (_partyObj.recruiting) {
        case PartyCodes.RECRUITMENT_OPEN:
            return true;

        case PartyCodes.RECRUITMENT_GROUP:
            return member.isGroupMember(_partyObj.group.getGroupId());

        default:
        case PartyCodes.RECRUITMENT_CLOSED:
            return false;
        }
    }

    protected PartyObject _partyObj;

//    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invmgr;
}
