//
// $Id$

package com.threerings.msoy.party.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.RootDObjectManager;

//import com.threerings.crowd.chat.server.SpeakHandler;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.notify.server.MsoyNotificationManager;
import com.threerings.msoy.party.data.PartyMarshaller;
import com.threerings.msoy.server.MemberNodeActions;

import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.orth.notify.data.GenericNotification;
import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.party.data.MemberParty;
import com.threerings.msoy.party.data.PartierObject;
import com.threerings.msoy.party.data.PartyAuthName;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyDetail;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;
import com.threerings.msoy.party.data.PartySummary;

import static com.threerings.msoy.Log.log;

/**
 * Manages a particular party, living on a single node.
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
        List<PartyPeep> list = Lists.newArrayList(_partyObj.peeps);
        return new PartyDetail(_summary, _lastInfo, list.toArray(new PartyPeep[list.size()]));
    }

    public void init (PartyObject partyObj, int creatorId)
    {
        _partyObj = partyObj;
        _summary = new PartySummary(_partyObj.id, _partyObj.name, _partyObj.group, _partyObj.icon);
        _partyObj.setAccessController(new PartyAccessController(this));

        MsoyNodeObject nodeObj = _peerMgr.getMsoyNodeObject();
        nodeObj.startTransaction();
        try {
            nodeObj.addToHostedParties(_summary);

            // in the middle of that, update the party object (and status), which will
            // also publish a partyInfo to the node object in this transaction
            _partyObj.startTransaction();
            try {
                _partyObj.setPartyService(_invMgr.registerProvider(this, PartyMarshaller.class));
    //            _partyObj.setSpeakService(_invMgr.registerDispatcher(
    //                new SpeakDispatcher(new SpeakHandler(_partyObj, this))));
                updateStatus();
            } finally {
                _partyObj.commitTransaction();
            }
        } finally {
            nodeObj.commitTransaction();
        }

        // "invite" the creator
        _invitedIds.add(creatorId);
    }

    /**
     * Shutdown this party.
     */
    public void shutdown ()
    {
        if (_partyObj == null) {
            return; // already shut down
        }

        MsoyNodeObject nodeObj = _peerMgr.getMsoyNodeObject();
        nodeObj.startTransaction();
        try {
            nodeObj.removeFromHostedParties(_partyObj.id);
            nodeObj.removeFromPartyInfos(_partyObj.id);
            // clear the party info from all remaining players' member objects
            for (PartyPeep peep : _partyObj.peeps) {
                indicateMemberPartying(peep.name.getId(), false);
            }
        } finally {
            nodeObj.commitTransaction();
        }

        _invMgr.clearDispatcher(_partyObj.partyService);
        // _invMgr.clearDispatcher(_partyObj.speakService);
        _omgr.destroyObject(_partyObj.getOid());

        _partyReg.partyWasRemoved(_partyObj.id);
        _partyObj = null;
        _lastInfo = null;
        _summary = null;
    }

    /**
     * Add the specified player to the party. Called from the PartyRegistry, which also takes care
     * of filling-in the partyId in the MemberObject. If the method returns normally, the player
     * will have been added to the party.
     *
     * @throws InvocationException if the player is not allowed into the party for some reason.
     */
    public void addPlayer (MemberName name, Rank groupRank)
        throws InvocationException
    {
        // TODO: now that we don't modify the _partyObj here, we could simplify the PartyRegistry
        // to not register the dobj until the user successfully joins.

        String snub = _partyObj.mayJoin(name, groupRank, _invitedIds.contains(name.getId()));
        if (snub != null) {
            throw new InvocationException(snub);
        }
    }

    /**
     * Called from the access controller when subscription is approved for the specified member.
     */
    public void clientSubscribed (PartierObject partier)
    {
        final int memberId = partier.getMemberId();
        // listen for them to die
        partier.addListener(new ObjectDeathListener() {
            public void objectDestroyed (ObjectDestroyedEvent event) {
                removePlayer(memberId);
            }
        });

        // clear their invites to this party, if any
        _invitedIds.remove(memberId);

        // update member's party info via a node action
        indicateMemberPartying(memberId, true);

        // Crap, we used to do this in addPlayer, but they could never actually enter the party
        // and leave it hosed. The downside of doing it this way is that we could approve
        // more than MAX_PLAYERS to join the party...
        // The user may already be in the party if they arrived from another node.
        if (!_partyObj.peeps.containsKey(memberId)) {
            _partyObj.addToPeeps(new PartyPeep(partier.memberName, nextJoinOrder()));
        }
        updatePartyInfo();
    }

    public void inviteAllFriends (MemberObject inviter)
    {
        MemberNodeActions.inviteAllFriendsToParty(inviter, _partyObj.id, _partyObj.name);
    }

    // from interface PartyProvider
    public void bootMember (
        ClientObject caller, int playerId, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        requireLeader(caller);
        if (removePlayer(playerId)) {
            MemberNodeActions.sendNotification(playerId,
                new GenericNotification("m.party_booted", Notification.PERSONAL));
        }
    }

    // from interface PartyProvider
    public void moveParty (
        ClientObject caller, int sceneId, InvocationService.InvocationListener il)
        throws InvocationException
    {
        requireLeader(caller);
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
    public void setGame (
        ClientObject caller, int gameId, byte gameState, int gameOid,
        InvocationService.InvocationListener il)
        throws InvocationException
    {
        requireLeader(caller);
        if ((_partyObj.gameId == gameId) && (_partyObj.gameState == gameState) &&
                (_partyObj.gameOid == gameOid)) {
            return; // NOOP!
        }

        // update the party's game location
        _partyObj.startTransaction();
        try {
            _partyObj.setGameOid(gameOid);
            _partyObj.setGameState(gameState);
            _partyObj.setGameId(gameId);
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
            _partyObj.setLeaderId(peep.name.getId());
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
        requireLeader(caller);
        if (status == null) {
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }
        status = StringUtil.truncate(status, PartyCodes.MAX_NAME_LENGTH);
        setStatus(status, PartyCodes.STATUS_TYPE_USER);
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
    public void updateDisband (
        ClientObject caller, boolean disband, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        requireLeader(caller);
        _partyObj.setDisband(disband);
    }

    // from interface PartyProvider
    public void inviteMember (
        ClientObject caller, int memberId, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        PartierObject inviter = (PartierObject)caller;
        if (_partyObj.recruitment == PartyCodes.RECRUITMENT_CLOSED &&
                _partyObj.leaderId != inviter.getMemberId()) {
            throw new InvocationException(PartyCodes.E_CANT_INVITE_CLOSED);
        }
        // add them to the invited set
        _invitedIds.add(memberId);
        // send them a notification
        //MemberNodeActions.sendNotification(memberId, createInvite(inviter));
        MemberNodeActions.inviteToParty(
            memberId, inviter.memberName.toMemberName(), _partyObj.id, _partyObj.name);
    }

    protected PartierObject requireLeader (ClientObject client)
        throws InvocationException
    {
        PartierObject partier = (PartierObject)client;
        if (partier.getMemberId() != _partyObj.leaderId) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }
        return partier;
    }

    /**
     * Remove the specified player from the party.
     * @return true if they were removed.
     */
    protected boolean removePlayer (int memberId)
    {
        // make sure we're still alive and they're actually in
        if (_partyObj == null || !_partyObj.peeps.containsKey(memberId)) {
            return false;
        }

        // if they're the last one, just kill the party
        if (_partyObj.peeps.size() == 1) {
            shutdown();
            return true;
        }

        if ((_partyObj.leaderId == memberId) && _partyObj.disband) {
            _partyObj.postMessage(PartyObject.NOTIFICATION,
                new GenericNotification("m.party_disbanded", Notification.PERSONAL));
            shutdown();
            return true;
        }

        // clear the party info from this player's member object
        indicateMemberPartying(memberId, false);

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
        return true;
    }

    protected void indicateMemberPartying (int memberId, boolean set)
    {
        MsoyNodeObject nodeObj = _peerMgr.getMsoyNodeObject();

        if (set) {
            MemberParty mp = new MemberParty(memberId, _partyObj.id);
            MemberParty omp = nodeObj.memberParties.get(mp.memberId);
            if (omp == null) {
                nodeObj.addToMemberParties(mp); // normal case
            } else if (omp.partyId != mp.partyId) {
                log.warning("Wha? Replacing stale MemberParty", "mp", mp, "omp", omp);
                nodeObj.updateMemberParties(mp);
            }
            // otherwise: no need to update anything. This can happen in normal circumstances
            // when a user logs in over themselves

        } else {
            nodeObj.removeFromMemberParties(memberId);
        }

        // tell the registry about this one directly
        _partyReg.updateUserParty(memberId, set ? _partyObj.id : 0, nodeObj);

        // and, if they're no longer partying, end their session
        if (!set) {
            PartySession session = (PartySession) _clmgr.getClient(PartyAuthName.makeKey(memberId));
            if (session != null) {
                session.endSession();
            }
        }
    }

//    // from SpeakHandler.SpeakerValidator
//    public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode)
//    {
//        return (speaker instanceof MemberObject) &&
//            ((MemberObject) speaker).partyId == _partyObj.id;
//    }

    /**
     * Automatically update the status of the party based on the current scene/party.
     */
    protected void updateStatus ()
    {
        if (_partyObj.gameId != 0) {
            Tuple<String, HostedGame> game = _peerMgr.getGameHost(_partyObj.gameId);
            if (game != null) {
                byte statusType = (_partyObj.gameState == PartyCodes.GAME_STATE_LOBBY)
                    ? PartyCodes.STATUS_TYPE_LOBBY : PartyCodes.STATUS_TYPE_PLAYING;
                setStatus(game.right.name, statusType);
            }
            return;
        }
        Tuple<String, HostedRoom> room = _peerMgr.getSceneHost(_partyObj.sceneId);
        if (room != null) {
            setStatus(room.right.name, PartyCodes.STATUS_TYPE_SCENE);
        } else {
            // we see this, we can investigate
            setStatus("unknown: " + _partyObj.sceneId, PartyCodes.STATUS_TYPE_USER);
        }
    }

    protected void setStatus (String status, byte statusType)
    {
        if (_partyObj.status == null || !_partyObj.status.equals(status) ||
                statusType != _partyObj.statusType) {
            _partyObj.startTransaction();
            try {
                _partyObj.setStatusType(statusType);
                _partyObj.setStatus(status);
            } finally {
                _partyObj.commitTransaction();
            }
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
                newLeader = peep.name.getId();
            }
        }
        return newLeader;
    }

    /**
     * Update the partyInfo we have currently published in the node object.
     */
    protected void updatePartyInfo ()
    {
        PartyInfo newInfo = new PartyInfo(_partyObj.id, _partyObj.leaderId, _partyObj.status,
            _partyObj.statusType, _partyObj.peeps.size(), _partyObj.recruitment);
        MsoyNodeObject nodeObj = _peerMgr.getMsoyNodeObject();
        if (nodeObj.partyInfos.containsKey(_partyObj.id)) {
            nodeObj.updatePartyInfos(newInfo);
        } else {
            nodeObj.addToPartyInfos(newInfo);
        }
        // notify the current node (other nodes will be notified by MsoyPeerNode)
        if (_lastInfo != null) {
            _partyReg.partyInfoChanged(_lastInfo, newInfo);
        }
        _lastInfo = newInfo;
    }

    protected PartyObject _partyObj;
    protected PartySummary _summary;
    protected PartyInfo _lastInfo;
    protected Set<Integer> _invitedIds = Sets.newHashSet();

    @Inject protected ClientManager _clmgr;
    @Inject protected InvocationManager _invMgr;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected MsoyNotificationManager _notifyMgr;
    @Inject protected PartyRegistry _partyReg;
    @Inject protected RootDObjectManager _omgr;
}
