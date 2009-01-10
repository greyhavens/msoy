//
// $Id$

package com.threerings.msoy.party.client {

import flash.utils.Dictionary;

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.presents.dobj.AttributeChangeAdapter;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ChangeListener;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SubscriberAdapter;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandMenu;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.party.client.PartyBoardService_JoinListener;
import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.msoy.party.data.PartyBootstrapData;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyDetail;
import com.threerings.msoy.party.data.PartyMarshaller;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldControlBar;

/**
 * Manages party stuff on the client.
 */
public class PartyDirector extends BasicDirector
    implements PartyBoardService_JoinListener
{
    // reference the PartyBoardMarshaller class
    PartyBoardMarshaller;

    public const log :Log = Log.getLog(this);

    public function PartyDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Can we invite people to our party?
     */
    public function canInviteToParty () :Boolean
    {
        return (_partyObj != null) &&
            ((_partyObj.recruitment == PartyCodes.RECRUITMENT_OPEN) || isPartyLeader());
    }

    public function partyContainsPlayer (memberId :int) :Boolean
    {
        return (_partyObj != null) && _partyObj.peeps.containsKey(memberId);
    }

    public function isInParty () :Boolean
    {
        var clobj :Object = _wctx.getClient().getClientObject();
        return (clobj is MemberObject) && (MemberObject(clobj).partyId != 0);
    }

    public function isPartyLeader () :Boolean
    {
        return (_partyObj != null) && (_partyObj.leaderId == _wctx.getMyName().getMemberId());
    }

    /**
     * Create either a party board popup, or a party popup if we're already in a party.
     */
    public function createAppropriatePartyPanel () :FloatingPanel
    {
        if (_partyObj != null) {
            return new PartyPanel(_wctx, _partyObj);
        } else {
            return new PartyBoardPanel(_wctx);
        }
    }

    public function popPeepMenu (peep :PartyPeep, partyId :int) :void
    {
        var menuItems :Array = [];

        _wctx.getMsoyController().addMemberMenuItems(peep.name, menuItems);

        if (_partyObj != null && partyId == _partyObj.id) {
            const peepId :int = peep.name.getMemberId();
            const ourId :int = _wctx.getMyName().getMemberId();
            if (_partyObj.leaderId == ourId && peepId != ourId) {
                menuItems.push({ type: "separator" });
                menuItems.push({ label: Msgs.PARTY.get("b.boot"),
                                 callback: bootMember, arg: peep.name.getMemberId() });
                menuItems.push({ label: Msgs.PARTY.get("b.assign_leader"),
                                 callback: assignLeader, arg: peep.name.getMemberId() });
            }
        }

        CommandMenu.createMenu(menuItems, _wctx.getTopPanel()).popUpAtMouse();
    }

    /**
     * Get the party board.
     */
    public function getPartyBoard (resultHandler :Function, query :String = null) :void
    {
        _pbsvc.getPartyBoard(_wctx.getClient(), query,
            _wctx.resultListener(resultHandler, MsoyCodes.PARTY_MSGS));
    }

    /**
     * Request info on the specified party. Results will be displayed in a popup.
     */
    public function getPartyDetail (partyId :int) :void
    {
        // suppress requests that are already outstanding
        if (Boolean(_detailRequests[partyId])) {
            // suppress
            return;
        }
        _detailRequests[partyId] = true;
        var handleFailure :Function = function (error :String) :void {
            delete _detailRequests[partyId];
            _wctx.displayFeedback(MsoyCodes.PARTY_MSGS, error);
        };
        _pbsvc.getPartyDetail(_wctx.getClient(), partyId,
            new ResultAdapter(gotPartyDetail, handleFailure));
    }

    /**
     * Create a new party.
     */
    public function createParty (name :String, groupId :int, inviteAllFriends :Boolean) :void
    {
//         var handleSuccess :Function = function (sceneId :int) :void {
//             visitPartyScene(sceneId);
//             Prefs.setPartyGroup(groupId);
//         };
//         var handleFailure :Function = function (error :String) :void {
//             _wctx.displayFeedback(MsoyCodes.PARTY_MSGS, error);
//             // re-open...
//             var panel :CreatePartyPanel = new CreatePartyPanel(_wctx);
//             panel.open();
//             panel.init(name, groupId, inviteAllFriends);
//         };

        // TODO: make the locator listener an adapter
        _pbsvc.createParty(_wctx.getClient(), name, groupId, inviteAllFriends, this);
    }

    /**
     * Join a party.
     */
    public function joinParty (id :int) :void
    {
        // first we have to find out what node is hosting the party in question
        _pbsvc.locateParty(_wctx.getClient(), id, this);
    }

    /**
     * Leaves the current party.
     */
    public function leaveParty () :void
    {
        // TODO: we might possibly need to be able to leave a party prior to receiving
        // our subscription response.
        if (_partyObj == null) {
            // we have no party to leave
            return;
        }
        _partyObj.partyService.leaveParty(_wctx.getClient(),
            _wctx.confirmListener(handleLeaveParty, MsoyCodes.PARTY_MSGS));
    }

    public function assignLeader (memberId :int) :void
    {
        _partyObj.partyService.assignLeader(_wctx.getClient(), memberId,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    public function updateStatus (status :String) :void
    {
        _partyObj.partyService.updateStatus(_wctx.getClient(), status,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    public function updateRecruitment (recruitment :int) :void
    {
        _partyObj.partyService.updateRecruitment(_wctx.getClient(), recruitment,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    /**
     * Leaves the current party.
     */
    public function bootMember (memberId :int) :void
    {
        _partyObj.partyService.bootMember(_wctx.getClient(), memberId,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    public function inviteMember (memberId :int) :void
    {
        _partyObj.partyService.inviteMember(_wctx.getClient(), memberId,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    // from PartyBoardService_JoinListener
    public function foundParty (partyId :int, hostname :String, port :int) :void
    {
        // create a new party session and connect to our party host node
        _pctx = new PartyContextImpl(_wctx);
        _pctx.getClient().addClientObserver(new ClientAdapter(
            null, partyDidLogon, null, partyDidLogoff, partyLogonFailed, partyConnectFailed));
        _pctx.connect(partyId, hostname, port);
    }

    // from PartyBoardService_JoinListener
    public function requestFailed (cause :String) :void
    {
        _wctx.displayFeedback(MsoyCodes.PARTY_MSGS, cause);
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // if they're in a party and have the popup down, make a note to not pop it
        // up on the new node
        _suppressPartyPop = (_partyObj != null) && !getButton().selected;
        clearParty();
    }

    protected function checkFollowParty () :void
    {
        visitPartyScene(_partyObj.sceneId);
    }

    /**
     * A success handler for creating and joining parties.
     */
    protected function visitPartyScene (sceneId :int) :void
    {
        if (sceneId != 0) {
            _wctx.getSceneDirector().moveTo(sceneId);
        }
    }

    /**
     * Handles the response from a leaveParty() request.
     */
    protected function handleLeaveParty () :void
    {
        clearParty();

        // TODO: have the party popup pop itself down, or something
        var btn :CommandButton = getButton();
        if (btn.selected) {
            btn.activate(); // pop down the party window.
        }
    }

    protected function partyDidLogon (event :ClientEvent) :void
    {
        var pbd :PartyBootstrapData = (event.getClient().getBootstrapData() as PartyBootstrapData);
        _safeSubscriber = new SafeSubscriber(pbd.partyOid,
            new SubscriberAdapter(gotPartyObject, subscribeFailed));
        _safeSubscriber.subscribe(_pctx.getDObjectManager());
    }

    protected function partyDidLogoff (event :ClientEvent) :void
    {
        trace("ZOMG! Logged off");
    }

    protected function partyLogonFailed (event :ClientEvent) :void
    {
        log.warning("Failed to logon to party server", "cause", event.getCause());

        // TODO: report via world chat that we failed to connect and hence failed to join the party
    }

    protected function partyConnectFailed (event :ClientEvent) :void
    {
        log.warning("Lost connection to party server", event.getCause());

        // we need to clear out our party stuff manually since everything was dropped
        _safeSubscriber = null;
        _partyObj = null;
        _pctx = null;

        // TODO: report via world chat that we lost our party connection
    }

    protected function clearParty () :void
    {
        if (_safeSubscriber != null) {
            _safeSubscriber.unsubscribe(_pctx.getDObjectManager());
            _safeSubscriber = null;
        }
        if (_partyObj != null) {
            _partyObj.removeListener(_partyListener);
            _partyListener = null;
            _partyObj = null;
        }
        if (_pctx != null) {
            _pctx.getClient().logoff(false);
            _pctx = null;
        }
    }

    /**
     * Called if our safe subscriber has succeeded in getting the party object.
     */
    protected function gotPartyObject (obj :PartyObject) :void
    {
        _partyObj = obj;
        _partyListener = new AttributeChangeAdapter(partyAttrChanged);
        _partyObj.addListener(_partyListener);

        // if the party popup is up, change to the new popup...
        var btn :CommandButton = getButton();
        if (btn.selected) {
            // click it down and then back up...
            btn.activate();
            btn.activate();

        } else if (!_suppressPartyPop) {
            btn.activate();
        }
        _suppressPartyPop = false;

        // we might need to warp to the party location
        checkFollowParty();
    }

    /**
     * Callback for a getPartyDetail request.
     */
    protected function gotPartyDetail (detail :PartyDetail) :void
    {
        // stop tracking that we have an outstanding request
        delete _detailRequests[detail.info.id];

        // close any previous detail panel for this party
        var panel :PartyDetailPanel = _detailPanels[detail.info.id] as PartyDetailPanel;
        if (panel != null) {
            panel.close();
        }

        // pop open the new one
        panel = new PartyDetailPanel(_wctx, detail);
        _detailPanels[detail.info.id] = panel;
        panel.setCloseCallback(function () :void {
            delete _detailPanels[detail.info.id];
        });
        panel.open();
    }

    protected function closeAllDetailPanels () :void
    {
        var panels :Array = [];
        for each (var o :Object in _detailPanels) {
            panels.push(o);
        }
        for each (var panel :PartyDetailPanel in panels) {
            panel.close();
        }
    }

    /**
     * Called when we've failed to subscribe to a party.
     */
    protected function subscribeFailed (oid :int, cause :ObjectAccessError) :void
    {
        log.warning("Party subscription failed", "cause", cause);
        // TODO
    }

    /**
     * Handles changes on the party object.
     */
    protected function partyAttrChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
        case PartyObject.SCENE_ID:
            checkFollowParty();
            break;
        }
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        super.registerServices(client);

        client.addServiceGroup(MsoyCodes.WORLD_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _pbsvc = (client.requireService(PartyBoardService) as PartyBoardService);
    }

    /**
     * Access the party button.
     */
    protected function getButton () :CommandButton
    {
        return WorldControlBar(_wctx.getControlBar()).partyBtn;
    }

    protected var _wctx :WorldContext;
    protected var _pbsvc :PartyBoardService;

    protected var _pctx :PartyContextImpl;
    protected var _partyObj :PartyObject;
    protected var _safeSubscriber :SafeSubscriber;

    /** True if we should not pop up the party panel when subscribing to a party. */
    protected var _suppressPartyPop :Boolean = false;

    protected var _detailRequests :Dictionary = new Dictionary();
    protected var _detailPanels :Dictionary = new Dictionary();

    protected var _partyListener :ChangeListener;
}
}
