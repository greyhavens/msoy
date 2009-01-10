//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Interval;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.util.MessageBundle;

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

//import com.threerings.crowd.chat.server.SpeakDispatcher;
//import com.threerings.crowd.chat.server.SpeakHandler;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberNodeActions;

import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.notify.data.GenericNotification;
import com.threerings.msoy.notify.data.Notification;
//import com.threerings.msoy.notify.data.PartyInviteNotification;
import com.threerings.msoy.notify.server.NotificationManager;

import com.threerings.msoy.room.data.RoomObject;

import com.threerings.msoy.party.data.PartierObject;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyDetail;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;
import com.threerings.msoy.party.data.PartySummary;

import static com.threerings.msoy.Log.log;

/**
 * Manages a particular party while it lives on a single node.
 */
public class PartyManager
    implements /* SpeakHandler.SpeakerValidator, */ PartyProvider
{
    /**
     * Returns our party distributed object.
     */
    public PartyObject getPartyObject ()
    {
        return _partyObj;
    }

    /**
     * Get the party detail, sans the group logo.
     */
    public PartyDetail getPartyDetail ()
    {
        return new PartyDetail(
            _lastInfo, _partyObj.peeps.toArray(new PartyPeep[_partyObj.peeps.size()]));
    }

    public void init (PartyObject partyObj, int creatorId)
    {
        _partyObj = partyObj;
        _partyObj.setAccessController(new PartyAccessController(this));
        _partyObj.startTransaction();
        try {
            _partyObj.setPartyService(_invMgr.registerDispatcher(new PartyDispatcher(this)));
//            _partyObj.setSpeakService(_invMgr.registerDispatcher(
//                new SpeakDispatcher(new SpeakHandler(_partyObj, this))));
            updateStatus();
        } finally {
            _partyObj.commitTransaction();
        }

        // "invite" the creator
        _invitedIds.add(creatorId);
    }

    /**
     * Shutdown this party.
     */
    public void shutdown ()
    {
        if (_partyObj != null) {
            removeFromNode();
            // clear the party info from all remaining players' member objects
            for (PartyPeep peep : _partyObj.peeps) {
                updatePartySummary(peep.name.getMemberId(), false);
            }
            _invMgr.clearDispatcher(_partyObj.partyService);
            // _invMgr.clearDispatcher(_partyObj.speakService);
            _omgr.destroyObject(_partyObj.getOid());
            _partyObj = null;
        }
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
     * Add the specified player to the party. Called from the PartyRegistry, which also takes care
     * of filling-in the partyId in the MemberObject. If the method returns normally, the player
     * will have been added to the party.
     *
     * @throws InvocationException if the player is not allowed into the party for some reason.
     */
    public void addPlayer (MemberName name, byte groupRank)
        throws InvocationException
    {
        // TODO: now that we don't modify the _partyObj here, we could simplify the PartyRegistry
        // to not register the dobj until the user successfully joins.

        String snub = _partyObj.mayJoin(name, groupRank, _invitedIds.contains(name.getMemberId()));
        if (snub != null) {
            throw new InvocationException(snub);
        }
    }

    /**
     * Called from the access controller when subscription is approved for the specified member.
     */
    public void clientSubscribed (final PartierObject partier)
    {
        // listen for them to die
        partier.addListener(new ObjectDeathListener() {
            public void objectDestroyed (ObjectDestroyedEvent event) {
                removePlayer(partier.getMemberId());
            }
        });

        // clear their invites to this party, if any
        _invitedIds.remove(partier.getMemberId());

        // update member's party info via a node action
        updatePartySummary(partier.getMemberId(), true);

        // Crap, we used to do this in addPlayer, but they could never actually enter the party
        // and leave it hosed. The downside of doing it this way is that we could approve
        // more than MAX_PLAYERS to join the party...
        // The user may already be in the party if they arrived from another node.
        if (!_partyObj.peeps.containsKey(partier.getMemberId())) {
            _partyObj.addToPeeps(new PartyPeep(partier.memberName, nextJoinOrder()));
        }
        updatePartyInfo();
    }

    public int getSceneId ()
    {
        return _partyObj.sceneId;
    }

    public void inviteAllFriends (MemberObject inviter)
    {
        MemberNodeActions.inviteAllFriendsToParty(inviter, _partyObj.id, _partyObj.name);
        //MemberNodeActions.notifyAllFriends(inviter, createInvite(inviter));
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
    public void moveParty (ClientObject caller, int sceneId, InvocationService.InvocationListener il)
        throws InvocationException
    {
        // only the leader can move the party
        PartierObject partier = (PartierObject)caller;
        if (partier.getMemberId() != _partyObj.leaderId) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }

        if (_partyObj.sceneId == sceneId) {
            return; // NOOP!
        }

        // update the party's location
        _partyObj.startTransaction();
        try {
            _partyObj.setSceneId(sceneId);
            updateStatus();
        } finally {
            _partyObj.commitTransaction();
        }
    }

    // from interface PartyProvider
    public void assignLeader (
        ClientObject caller, int memberId, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        requireLeader(caller);

        PartyPeep leader = _partyObj.peeps.get(_partyObj.leaderId);
        PartyPeep peep = _partyObj.peeps.get(memberId);
        if (peep == null || peep == leader) {
            // TODO: nicer error? The player may have just left
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }

        _partyObj.startTransaction();
        try {
            peep.joinOrder = leader.joinOrder;
            leader.joinOrder = leader.joinOrder + 1;
            _partyObj.setLeaderId(peep.name.getMemberId());
            _partyObj.updatePeeps(peep);
            _partyObj.updatePeeps(leader);
        } finally {
            _partyObj.commitTransaction();
        }
    }

    // from interface PartyProvider
    public void updateStatus (
        ClientObject caller, String status, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (status == null) {
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }
        requireLeader(caller);
        setStatus(MessageBundle.taint(
            StringUtil.truncate(status, PartyCodes.MAX_NAME_LENGTH)));
    }

    // from interface PartyProvider
    public void updateRecruitment (
        ClientObject caller, byte recruitment, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        requireLeader(caller);

        _partyObj.setRecruitment(recruitment);
        updatePartyInfo();
    }

    // from interface PartyProvider
    public void inviteMember (
        ClientObject caller, int memberId, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject inviter = (MemberObject)caller;
        if (_partyObj.recruitment == PartyCodes.RECRUITMENT_CLOSED &&
                _partyObj.leaderId != inviter.getMemberId()) {
            throw new InvocationException(PartyCodes.E_CANT_INVITE_CLOSED);
        }
        // add them to the invited set
        _invitedIds.add(memberId);
        // send them a notification
        //MemberNodeActions.sendNotification(memberId, createInvite(inviter));
        MemberNodeActions.inviteToParty(memberId, inviter, _partyObj.id, _partyObj.name);
    }

    /**
     * Remove the specified player from the party.
     */
    protected void removePlayer (int memberId)
    {
        // make sure we're still alive and they're actually in
        if (_partyObj == null || !_partyObj.peeps.containsKey(memberId)) {
            return;
        }

        // if they're the last one, just kill the party
        if (_partyObj.peeps.size() == 1) {
            shutdown();
            return;
        }

        // clear the party info from this player's member object
        updatePartySummary(memberId, false);

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
        updatePartyInfo();
    }

    protected void updatePartySummary (int memberId, boolean added)
    {
        PartySummary summary = added ?
            new PartySummary(_partyObj.id, _partyObj.name, _partyObj.group, _partyObj.icon) : null;
        MemberNodeActions.updateParty(memberId, summary);
    }

//    // from SpeakHandler.SpeakerValidator
//    public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode)
//    {
//        return (speaker instanceof MemberObject) &&
//            ((MemberObject) speaker).partyId == _partyObj.id;
//    }

//    /**
//     * Create an invitation to this party.
//     */
//    protected Notification createInvite (MemberObject inviter)
//    {
//        return new PartyInviteNotification(inviter.memberName, _partyObj.id, _partyObj.name);
//    }

    /**
     * Automatically update the status of the party based on the current scene.
     */
    protected void updateStatus ()
    {
        Tuple<String, HostedRoom> room = _peerMgr.getSceneHost(_partyObj.sceneId);
        if (room != null) {
            setStatus(MessageBundle.tcompose("m.status_room", room.right.name));
        }
    }

   protected void setStatus (String status)
    {
        if (_partyObj.status == null || !_partyObj.status.equals(status)) {
            _partyObj.setStatus(status);
            updatePartyInfo();
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
            _partyObj.id, _partyObj.name, _partyObj.leaderId, _partyObj.group.getGroupId(),
            _partyObj.status, _partyObj.peeps.size(), _partyObj.recruitment);
        _peerMgr.updatePartyInfo(_lastInfo);
    }

    protected PartyObject _partyObj;
    protected PartyInfo _lastInfo;
    protected ArrayIntSet _invitedIds = new ArrayIntSet();

    @Inject protected PartyRegistry _partyReg;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invMgr;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected NotificationManager _notifyMgr;
}
