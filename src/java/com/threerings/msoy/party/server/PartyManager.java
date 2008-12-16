//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;

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
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakHandler;

import com.threerings.whirled.server.SceneManager;
import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.server.MemberNodeActions;

import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.notify.data.GenericNotification;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.data.PartyInviteNotification;
import com.threerings.msoy.notify.server.NotificationManager;

import com.threerings.msoy.room.data.RoomObject;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;
import com.threerings.msoy.party.data.PartySummary;

import static com.threerings.msoy.Log.log;

/**
 * Manages a particular party while it lives on a single node.
 */
public class PartyManager
    implements PartyProvider, SpeakHandler.SpeakerValidator
{
    public void init (PartyObject partyObj)
    {
        _partyObj = partyObj;
        _partyObj.setAccessController(new PartyAccessController(this));
        _partyObj.startTransaction();
        try {
            _partyObj.setPartyService(_invMgr.registerDispatcher(new PartyDispatcher(this)));
            _partyObj.setSpeakService(_invMgr.registerDispatcher(
                new SpeakDispatcher(new SpeakHandler(_partyObj, this))));
            updateStatus();
        } finally {
            _partyObj.commitTransaction();
        }

        // on init, we should always be able to find the SceneManager and update the Summary
        addPartySummary(_screg.getSceneManager(partyObj.sceneId));

        // prevent fuckups after a node move: wait 2 minutes and clean out any non-present peeps
        _nodeMoveCleaner = new Interval(_omgr) {
            public void expired () {
                cleanAbsentPeeps();
                _nodeMoveCleaner = null;
            }
        };
        _nodeMoveCleaner.schedule(2 * 60 * 1000); // once
    }

    public void endParty ()
    {
        removePartySummary();
        shutdown();
    }

    /**
     * Shutdown this party.
     */
    public void shutdown ()
    {
        removeFromNode();

        _invMgr.clearDispatcher(_partyObj.partyService);
        _invMgr.clearDispatcher(_partyObj.speakService);
        // TODO: this won't work if nobody is subscribed
        _partyObj.setDestroyOnLastSubscriberRemoved(true);
//        _omgr.destroyObject(_partyObj.getOid());

        for (UserListener listener : _userListeners.values()) {
            listener.memObj.removeListener(listener);
        }
        _userListeners.clear();
    }

    /**
     * Remove this party from the current node.
     */
    public void removeFromNode ()
    {
        _peerMgr.removePartyInfo(_partyObj.id);
        _partyReg.partyWasRemoved(_partyObj.id);

        if (_nodeMoveCleaner != null) {
            _nodeMoveCleaner.cancel();
            _nodeMoveCleaner = null;
        }
    }

    /**
     * Add the specified player to the party. Called from the PartyRegistry, which also
     * takes care of filling-in the partyId in the MemberObject.
     */
    public void addPlayer (VizMemberName name, byte groupRank, InvocationService.ResultListener rl)
        throws InvocationException
    {
        // TODO: now that we don't modify the _partyObj here, we could simplify the PartyRegistry
        // to not register the dobj until the user successfully joins.

        String snub = _partyObj.mayJoin(name, groupRank);
        if (snub != null) {
            throw new InvocationException(snub);
        }

        // inform them of the sceneId so that they can move there.
        rl.requestProcessed(_partyObj.sceneId);
    }

    /**
     * Called via the PartyRegistry (from MsoySceneRegistry) to inform us that a player
     * is moving scenes.
     */
    public void playerWillMove (MemberObject member, int sceneId)
    {
        int memberId = member.getMemberId();
        if (memberId == _partyObj.leaderId) {
            // the leader will move- inform the party immediately because this object may soon die
            // if it needs to be squirted across nodes
            _partyObj.startTransaction();
            try {
                updateSceneId(sceneId);
                updateStatus();
            } finally {
                _partyObj.commitTransaction();
            }

        } else if (_partyObj.peeps.containsKey(memberId) && (sceneId != _partyObj.sceneId)) {
            // otherwise, they leave the party with a notification that they've done so
            removePlayer(memberId);
            _notifyMgr.notify(member, new GenericNotification(
                MessageBundle.tcompose("m.party_left", _partyObj.id), Notification.INVITE));
        }
    }

    /**
     * Called from the access controller when subscription is approved for the specified member.
     */
    public void clientSubscribed (MemberObject member)
    {
        // start listening for them to die
        UserListener listener = new UserListener(member);
        _userListeners.put(member.getMemberId(), listener);
        member.addListener(listener);

        // Crap, we used to do this in addPlayer, but they could never actually enter the party
        // and leave it hosed. The downside of doing it this way is that we could approve
        // more than MAX_PLAYERS to join the party...
        // The user may already be in the party if they arrived from another node.
        if (!_partyObj.peeps.containsKey(member.getMemberId())) {
            _partyObj.addToPeeps(new PartyPeep(member.memberName, nextJoinOrder()));
        }
        updatePartyInfo();
    }

    public PartyObject getPartyObject ()
    {
        return _partyObj;
    }

    /**
     * Get the party detail.
     */
    public PartyPeep[] getPartyDetail ()
    {
        return _partyObj.peeps.toArray(new PartyPeep[_partyObj.peeps.size()]);
    }

    public int getSceneId ()
    {
        return _partyObj.sceneId;
    }

    public void inviteAllFriends (MemberObject inviter)
    {
        MemberNodeActions.notifyAllFriends(inviter, createInvite(inviter));
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
        MemberNodeActions.sendNotification(memberId, createInvite(inviter));
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
            // clear the party id
            _partyReg.updatePartyId(listener.memObj, 0);
        }

        // if they're the last one, just kill the party
        if (_partyObj.peeps.size() == 1) {
            endParty();
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
        updatePartyInfo();
    }

//    /**
//     * React to a player changing location.
//     */
//    protected void playerChangedLocation (MemberObject player, Place place)
//    {
//        if (place == null) {
//            // TODO?
//            log.debug("Player moved to nowhere", "who", player.who());
//            // ignore for now
//            return;
//        }
//
//        // see if it's a new scene
//        if (place instanceof ScenePlace) {
//            int sceneId = ((ScenePlace)place).sceneId;
//            if (sceneId == _partyObj.sceneId) {
//                return;
//            }
//            if (_partyObj.leaderId == player.getMemberId()) {
//                // the leader just moved location.
//                _partyObj.setSceneId(sceneId);
//
//            } else {
//                // otherwise, they leave the party with a notification that they've done so
//                log.info("TODO: partier left party scene.");
//            }
//        }
//    }

    // from SpeakHandler.SpeakerValidator
    public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode)
    {
        return (speaker instanceof MemberObject) &&
            ((MemberObject) speaker).partyId == _partyObj.id;
    }

    /**
     * Create an invitation to this party.
     */
    protected Notification createInvite (MemberObject inviter)
    {
        return new PartyInviteNotification(inviter.memberName, _partyObj.id, _partyObj.name);
    }

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

    protected void updateSceneId (final int sceneId)
    {
        if (_partyObj.sceneId  == sceneId) {
            return;
        }

        // remove the old
        removePartySummary();
        // update the sceneId
        _partyObj.setSceneId(sceneId);

        // then, set up the new summary
        SceneManager newmgr = _screg.getSceneManager(sceneId);
        if (newmgr != null) {
            addPartySummary(newmgr);

        } else {
            Tuple<String, HostedRoom> room = _peerMgr.getSceneHost(sceneId);
            if (room == null) {
                // if the room is not in the list, we must be about to resolve it here....
                _screg.resolveScene(sceneId, new SceneRegistry.ResolutionListener() {
                    public void sceneWasResolved (SceneManager scmgr) {
                        // make sure it's still the right scene
                        if (_partyObj.sceneId == sceneId) {
                            addPartySummary(scmgr);
                        }
                    }

                    public void sceneFailedToResolve (int sceneId, Exception reason) {
                        log.warning("Could not resolve scene for PartySummary", reason);
                    }
                });

            }
            // else: the room is resolving elsewhere, and we'll be there soon...
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
     * Clean out any people who are not yet subscribed to the party object.
     */
    protected void cleanAbsentPeeps ()
    {
        // make a copy to prevent co-mod
        for (PartyPeep peep : _partyObj.peeps.toArray(new PartyPeep[_partyObj.peeps.size()])) {
            if (!_userListeners.containsKey(peep.name.getMemberId())) {
                removePlayer(peep.name.getMemberId());
            }
        }
        // that should be enough...
    }

    /**
     * Update the partyInfo we have currently published in the node object.
     */
    protected void updatePartyInfo ()
    {
        _peerMgr.updatePartyInfo(new PartyInfo(
            _partyObj.id, _partyObj.name, _partyObj.leaderId, _partyObj.group, _partyObj.status,
            _partyObj.peeps.size(), _partyObj.recruitment));
    }

    protected void addPartySummary (SceneManager scmgr)
    {
        RoomObject roomObj = (RoomObject) scmgr.getPlaceObject();
        PartySummary summary = new PartySummary(
            _partyObj.id, _partyObj.name, _partyObj.group, _partyObj.icon);

        if (roomObj.parties.containsKey(summary.getKey())) {
            log.warning("Actually, I don't think this should happen");
            roomObj.updateParties(summary);

        } else {
            roomObj.addToParties(summary);
        }
    }

    protected void removePartySummary ()
    {
        RoomObject roomObj = (RoomObject)_screg.getSceneManager(_partyObj.sceneId).getPlaceObject();
        if (roomObj.parties.containsKey(_partyObj.id)) {
            roomObj.removeFromParties(_partyObj.id);
        }
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
//            if (MemberObject.LOCATION.equals(event.getName())) {
//                playerChangedLocation(memObj, (Place) event.getValue());
//            }
        }

        // from ObjectDeathListener
        public void objectDestroyed (ObjectDestroyedEvent event)
        {
            removePlayer(memObj.getMemberId());
        }
    } // end: class UserListener

    protected PartyObject _partyObj;

    protected IntMap<UserListener> _userListeners = IntMaps.newHashIntMap();

    protected Interval _nodeMoveCleaner;

    @Inject protected PartyRegistry _partyReg;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invMgr;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected NotificationManager _notifyMgr;
    @Inject protected SceneRegistry _screg;
}
