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

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.crowd.data.Place;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

import static com.threerings.msoy.Log.log;

/**
 * Manages a particular party while it lives on a single node.
 */
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
        requireLeader(caller);

        removePlayer(playerId);
    }

    protected MemberObject requireLeader (ClientObject client)
        throws InvocationException
    {
        MemberObject member = (MemberObject)client;
        if (member.getMemberId() != _partyObj.leaderId) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }
        return member;
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
        MemberObject member = requireLeader(caller);

        // TODO
    }

    // from interface PartyProvider
    public void updateStatus (
        ClientObject caller, String status, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        requireLeader(caller);

        _partyObj.setStatus(status);
    }

    /**
     * Can the specified member join this party?
     */
    protected boolean canJoinParty (MemberObject member)
    {
        // TODO: allow players specifically invited by the leader

        if (_lastInfo == null) {
            // allow the first player to join, which happens immediately
            return true;
        }

        return _lastInfo.hasAccess(member);
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
     * React to a player changing location.
     */
    protected void playerChangedLocation (MemberObject player, Place place)
    {
        if (place == null) {
            // TODO?
            log.debug("Player moved to nowhere", "who", player.who());
            // ignore for now
            return;
        }

        // see if it's a new scene
        if (place instanceof ScenePlace) {
            int sceneId = ((ScenePlace)place).sceneId;
            if (sceneId == _partyObj.sceneId) {
                return;
            }
            if (_partyObj.leaderId == player.getMemberId()) {
                // the leader just moved location.
                _partyObj.setSceneId(sceneId);

            } else {
                // otherwise, they leave the party with a notification that they've done so
                log.info("TODO: partier left party scene.");
            }
        }
    }

    /**
     * Return the next join order.
     */
    protected int nextJoinOrder ()
    {
        // return 1 higher than any other joinOrder, or 0.
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
        _lastInfo = new PartyInfo(
            _partyObj.id, _partyObj.name, _partyObj.leaderId, _partyObj.group, _partyObj.status,
            _partyObj.peeps.size(), _partyObj.recruiting);
        _peerMgr.updatePartyInfo(_lastInfo);
    }

    /**
     * A listener is created for each participant in the party.
     */
    protected class UserListener
        implements AttributeChangeListener, ObjectDeathListener
    {
        public MemberObject memObj;

        public UserListener (MemberObject memObj)
        {
            this.memObj = memObj;
        }

        // from AttributeChangeListener
        public void attributeChanged (AttributeChangedEvent event)
        {
            if (MemberObject.LOCATION.equals(event.getName())) {
                playerChangedLocation(memObj, (Place) event.getValue());
            }
        }

        // from ObjectDeathListener
        public void objectDestroyed (ObjectDestroyedEvent event)
        {
            removePlayer(memObj.getMemberId());
        }
    } // end: class UserListener

    protected PartyObject _partyObj;

    protected PartyInfo _lastInfo;

    protected IntMap<UserListener> _userListeners = IntMaps.newHashIntMap();

    @Inject protected PartyRegistry _partyReg;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invMgr;
    @Inject protected MsoyPeerManager _peerMgr;
}
