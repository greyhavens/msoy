//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PartyManager
    implements PartyProvider
{
    public PartyManager (PartyObject partyObj, InvocationManager invMgr, MsoyPeerManager peerMgr)
    {
        _partyObj = partyObj;
        _invMgr = invMgr;
        _peerMgr = peerMgr;

        _partyObj.setPartyService(_invMgr.registerDispatcher(new PartyDispatcher(this)));
    }

    /**
     * Shutdown this party.
     */
    public void shutdown ()
    {
        removeFromNode();
    }

    /**
     * Remove this party from the current node.
     */
    public void removeFromNode ()
    {
        _peerMgr.removePartyInfo(_partyObj.id);
    }

    /**
     * Add the specified player to the party.
     */
    public void addPlayer (MemberObject member, InvocationService.ResultListener rl)
        throws InvocationException
    {
        if (!canJoinParty(member)) {
            throw new InvocationException("TODO");
        }

        // figure out the highest yet-seen joinOrder
        int joinOrder = 0;
        for (PartyPeep peep : _partyObj.peeps) {
            joinOrder = Math.max(joinOrder, peep.joinOrder + 1);
        }

        // go ahead and add them
        _partyObj.addToPeeps(new PartyPeep(member.memberName, joinOrder));
        member.setPartyId(_partyObj.id);

        // tell them the OID so they can subscribe
        rl.requestProcessed(_partyObj.getOid());

        updatePartyInfo();
    }

    // from interface PartyProvider
    public void bootMember (
        ClientObject caller, int memberId, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // TODO
    }

    // from interface PartyProvider
    public void leaveParty (ClientObject caller, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        listener.requestProcessed(); // TODO
    }

    // from interface PartyProvider
    public void assignLeader (
        ClientObject caller, int memberId, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // TODO
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

    /**
     * Update the partyInfo we have currently published in the node object.
     */
    protected void updatePartyInfo ()
    {
        _peerMgr.updatePartyInfo(new PartyInfo(
            _partyObj.id, _partyObj.name, _partyObj.leaderId, _partyObj.group, _partyObj.status,
            _partyObj.peeps.size(), _partyObj.recruiting));
    }

    protected PartyObject _partyObj;

    protected InvocationManager _invMgr;
    protected MsoyPeerManager _peerMgr;
}
