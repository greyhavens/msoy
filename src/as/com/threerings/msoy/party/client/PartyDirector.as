//
// $Id$

package com.threerings.msoy.party.client {

import flash.events.Event;
import flash.utils.Dictionary;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandMenu;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.presents.dobj.AttributeChangeAdapter;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ChangeListener;
import com.threerings.presents.dobj.NamedEvent;
import com.threerings.presents.dobj.ObjectAccessError;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.game.client.GameDirector;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.msoy.party.data.PartyBootstrapData;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyDetail;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldControlBar;

/**
 * Manages party stuff on the client.
 */
public class PartyDirector extends BasicDirector
{
    // reference the PartyBoardMarshaller class
    PartyBoardMarshaller;

    public const log :Log = Log.getLog(this);

    public function PartyDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
        _wctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, locationDidChange, null));
        _wctx.getGameDirector().addEventListener(GameDirector.GAMING_STATE_CHANGED,
            handleGamingStateChanged);
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

    public function getPartyId () :int
    {
        return _wctx.getMemberObject().partyId;
    }

    public function getPartySize () :int
    {
        return (_partyObj == null) ? 0 : _partyObj.peeps.size();
    }

    public function isInParty () :Boolean
    {
        return (0 != getPartyId());
    }

    public function isPartyLeader () :Boolean
    {
        return (_partyObj != null) && (_partyObj.leaderId == _wctx.getMyId());
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
            const ourId :int = _wctx.getMyId();
            if (_partyObj.leaderId == ourId && peepId != ourId) {
                CommandMenu.addSeparator(menuItems);
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
        if (Boolean(_detailRequests[partyId])) {
            return; // suppress requests that are already outstanding
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
     * Get the cost to start a party from the server.
     */
    public function getCreateCost (callback :Function) :void
    {
        _pbsvc.getCreateCost(_wctx.getClient(), _wctx.resultListener(callback));
    }

    /**
     * Create a new party.
     */
    public function createParty (
        currency :Currency, authedCost :int, name :String, groupId :int, inviteAllFriends :Boolean)
        :void
    {
        var handleSuccess :Function = function (partyId :int, host :String, port :int) :void {
            connectParty(partyId, host, port);
            Prefs.setPartyGroup(groupId);
        };
        var handleNewPrice :Function = function (price :PriceQuote) :void {
            // re-open...
            var panel :CreatePartyPanel = new CreatePartyPanel(_wctx, price);
            panel.open();
            panel.init(name, groupId, inviteAllFriends);
        };
        var handleFailure :Function = function (error :String) :void {
            _wctx.displayFeedback(MsoyCodes.PARTY_MSGS, error);
            // re-open...
            var panel :CreatePartyPanel = new CreatePartyPanel(_wctx);
            panel.open();
            panel.init(name, groupId, inviteAllFriends);
        };
        _pbsvc.createParty(_wctx.getClient(), currency, authedCost, name, groupId, inviteAllFriends,
            new JoinAdapter(handleSuccess, handleNewPrice, handleFailure));
    }

    /**
     * Join a party.
     */
    public function joinParty (id :int) :void
    {
        if (isInParty()) {
            clearParty();
        }

        // first we have to find out what node is hosting the party in question
        _pbsvc.locateParty(_wctx.getClient(), id,
            new JoinAdapter(connectParty, null, function (cause :String) :void {
                _wctx.displayFeedback(MsoyCodes.PARTY_MSGS, cause);
            }));
    }

    /**
     * Clear/leave the current party, if any.
     */
    public function clearParty () :void
    {
        // pop down the party window.
        var btn :CommandButton = getButton();
        if (btn.selected) {
            btn.activate();
        }

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

    public function assignLeader (memberId :int) :void
    {
        _partyObj.partyService.assignLeader(_pctx.getClient(), memberId,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    public function updateStatus (status :String) :void
    {
        _partyObj.partyService.updateStatus(_pctx.getClient(), status,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    public function updateRecruitment (recruitment :int) :void
    {
        _partyObj.partyService.updateRecruitment(_pctx.getClient(), recruitment,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    /**
     * Leaves the current party.
     */
    public function bootMember (memberId :int) :void
    {
        _partyObj.partyService.bootMember(_pctx.getClient(), memberId,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    public function inviteMember (memberId :int) :void
    {
        _partyObj.partyService.inviteMember(_pctx.getClient(), memberId,
            _wctx.listener(MsoyCodes.PARTY_MSGS));
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        if (!event.isSwitchingServers()) {
            clearParty();
        }
    }

    protected function checkFollowParty () :void
    {
        /* TODO: if (_partyObj.partyFollowsLeader) */
        visitPartyScene(_partyObj.sceneId);
    }

    protected function checkFollowGame () :void
    {
        if (isPartyLeader()) {
            return; // we don't follow! (prevent leaving a game we're in)
        }
        const gameDir :GameDirector = _wctx.getGameDirector();
        if (_partyObj.gameId != gameDir.getGameId()) {
            gameDir.clearAnyGame();
        }
        if (_partyObj.gameId != 0) {
            if (_partyObj.avrGame) {
                gameDir.activateAVRGame(_partyObj.gameId);
            } else {
                // join the leader's game
                gameDir.playNow(_partyObj.gameId, _partyObj.leaderId);
            }
        }
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

    protected function partyDidLogon (event :ClientEvent) :void
    {
        var pbd :PartyBootstrapData = (event.getClient().getBootstrapData() as PartyBootstrapData);
        _safeSubscriber = new SafeSubscriber(pbd.partyOid, gotPartyObject, subscribeFailed);
        _safeSubscriber.subscribe(_pctx.getDObjectManager());
    }

    protected function partyLogonFailed (event :ClientEvent) :void
    {
        log.warning("Failed to logon to party server", "cause", event.getCause());
        _wctx.displayFeedback(MsoyCodes.PARTY_MSGS, event.getCause().message);
    }

    protected function partyConnectFailed (event :ClientEvent) :void
    {
        var cause :Error = event.getCause();
        log.warning("Lost connection to party server", cause);

        // we need to clear out our party stuff manually since everything was dropped
        _safeSubscriber = null;
        _partyListener = null;
        _partyObj = null;
        _pctx = null;
        clearParty(); // clear the rest

        // report via world chat that we lost our party connection
        if (cause != null) {
            _wctx.displayFeedback(MsoyCodes.PARTY_MSGS,
                MessageBundle.tcompose("e.lost_party", cause.message));
        }
    }

    protected function connectParty (partyId :int, hostname :String, port :int) :void
    {
        // we are joining a party- close all detail panels
        closeAllDetailPanels();

        // create a new party session and connect to our party host node
        _pctx = new PartyContextImpl(_wctx);
        _pctx.getClient().addClientObserver(new ClientAdapter(
            null, partyDidLogon, null, null, partyLogonFailed, partyConnectFailed));
        _pctx.connect(partyId, hostname, port);
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

        } else {
            btn.activate();
        }

        // we might need to warp to the party location
        checkFollowParty();
        if (isPartyLeader()) {
            handleGamingStateChanged();
        } else {
            checkFollowGame();
        }
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
        panel.addCloseCallback(function () :void {
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
        clearParty();
    }

    /**
     * Called when our world location changes.
     */
    protected function locationDidChange (place :PlaceObject) :void
    {
        // if we're the leader of the party, change the party's location when we move
        if (isPartyLeader()) {
            var scene :Scene = _wctx.getSceneDirector().getScene();
            if ((scene != null) && (scene.getId() != _partyObj.sceneId)) {
                _partyObj.partyService.moveParty(
                    _pctx.getClient(), scene.getId(), _wctx.listener(MsoyCodes.PARTY_MSGS));
            }
        }
    }

    /**
     * Called whenever our gaming state changes.
     */
    protected function handleGamingStateChanged (ignored :Event = null) :void
    {
        if (!isPartyLeader()) {
            return;
        }

        const gameDir :GameDirector = _wctx.getGameDirector();
        var avrGame :Boolean = gameDir.isAVRGame();
        var gameId :int = gameDir.getGameId();
        if ((gameId != _partyObj.gameId) || (avrGame != _partyObj.avrGame)) {
            _partyObj.partyService.setGame(
                _pctx.getClient(), gameId, avrGame, _wctx.listener(MsoyCodes.PARTY_MSGS));
        }
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

        case PartyObject.GAME_ID:
            checkFollowGame();
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

    protected var _detailRequests :Dictionary = new Dictionary();
    protected var _detailPanels :Dictionary = new Dictionary();

    protected var _partyListener :ChangeListener;
}
}

import com.threerings.presents.client.InvocationAdapter;
import com.threerings.msoy.party.client.PartyBoardService_JoinListener;

import com.threerings.msoy.money.data.all.PriceQuote;

class JoinAdapter extends InvocationAdapter
    implements PartyBoardService_JoinListener
{
    public function JoinAdapter (
        foundFunc :Function, costUpdatedFunc :Function, failedFunc :Function)
    {
        super(failedFunc);
        _foundFunc = foundFunc;
        _costUpdatedFunc = costUpdatedFunc;
    }

    public function priceUpdated (newQuote :PriceQuote) :void
    {
        _costUpdatedFunc(newQuote);
    }

    public function foundParty (partyId :int, hostname :String, port :int) :void
    {
        _foundFunc(partyId, hostname, port);
    }

    protected var _foundFunc :Function;
    protected var _costUpdatedFunc :Function;
}
