//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PartyManager
    implements PartyProvider
{
    public void init (PartyObject partyObj)
    {
        _partyObj = partyObj;
        _partyObj.setPartyService(_invMgr.registerDispatcher(new PartyDispatcher(this)));
    }

    /**
     * Shutdown this party.
     */
    public void shutdown ()
    {
        removeFromNode();

        _invMgr.clearDispatcher(_partyObj.partyService);
        _omgr.destroyObject(_partyObj.getOid());
    }

    /**
     * Remove this party from the current node.
     */
    public void removeFromNode ()
    {
        _peerMgr.removePartyInfo(_partyObj.id);
        _partyReg.partyWasRemoved(_partyObj.id);
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

        // go ahead and add them
        _partyObj.addToPeeps(new PartyPeep(member.memberName, nextJoinOrder()));
        member.setPartyId(_partyObj.id);

        // start listening for them to die
        UserListener listener = new UserListener(member);
        _userListeners.put(member.getMemberId(), listener);
        member.addListener(listener);

        updatePartyInfo();

        // tell them the OID so they can subscribe
        rl.requestProcessed(_partyObj.getOid());
    }

    // from interface PartyProvider
    public void bootMember (
        ClientObject caller, int playerId, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject member = (MemberObject)caller;
        // only the leader may boot
        if (member.getMemberId() != _partyObj.leaderId) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }

        removePlayer(playerId);
    }

    // from interface PartyProvider
    public void leaveParty (ClientObject caller, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        MemberObject member = (MemberObject)caller;

        removePlayer(member.getMemberId());
        listener.requestProcessed();
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
     * Remove the specified player from the party.
     */
    protected void removePlayer (int memberId)
    {
        // make sure they're actually in
        if (!_partyObj.peeps.containsKey(memberId)) {
            return; // silently cope
        }

        // remove the listener
        UserListener listener = _userListeners.remove(memberId);
        if (listener != null && listener.memObj.isActive()) {
            listener.memObj.removeListener(listener);
            listener.memObj.setPartyId(0); // clear the party id
        }

        // if they're the last one, just kill the party
        if (_partyObj.peeps.size() == 1) {
            shutdown();
            return;
        }

        _partyObj.startTransaction();
        try {
            _partyObj.removeFromPeeps(memberId);

            // maybe reassign the leader
            if (_partyObj.leaderId == memberId) {
                _partyObj.setLeaderId(nextLeader());
            }
        } finally {
            _partyObj.commitTransaction();
        }
    }

    /**
     * Return the next join order.
     */
    protected int nextJoinOrder ()
    {
        // return 0, or 1 higher than any other joinOrder
        int joinOrder = -1;
        for (PartyPeep peep : _partyObj.peeps) {
            if (peep.joinOrder > joinOrder) {
                joinOrder = peep.joinOrder;
            }
        }
        return (joinOrder + 1);
    }

    /**
     * Return the playerId of the next leader.
     */
    protected int nextLeader ()
    {
        // find the lowest joinOrder
        int joinOrder = Integer.MAX_VALUE;
        int newLeader = 0;
        for (PartyPeep peep : _partyObj.peeps) {
            if (peep.joinOrder < joinOrder) {
                joinOrder = peep.joinOrder;
                newLeader = peep.name.getMemberId();
            }
        }
        return newLeader;
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

    protected class UserListener
        implements ObjectDeathListener
    {
        public MemberObject memObj;

        public UserListener (MemberObject memObj)
        {
            this.memObj = memObj;
        }

        public void objectDestroyed (ObjectDestroyedEvent event)
        {
            removePlayer(memObj.getMemberId());
        }
    }

    protected PartyObject _partyObj;

    protected IntMap<UserListener> _userListeners = IntMaps.newHashIntMap();

    @Inject protected PartyRegistry _partyReg;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invMgr;
    @Inject protected MsoyPeerManager _peerMgr;
}
