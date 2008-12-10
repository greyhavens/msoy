//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ConfirmAdapter;
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

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.msoy.party.data.PartyMarshaller;
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

    public function PartyDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
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

    public function popPeepMenu (peep :PartyPeep) :void
    {
        var menuItems :Array = [];

        _wctx.getMsoyController().addMemberMenuItems(peep.name, menuItems);

        const peepId :int = peep.name.getMemberId();
        const ourId :int = _wctx.getMyName().getMemberId();
        if (_partyObj.leaderId == ourId && peepId != ourId) {
            menuItems.push({ type: "separator" });
            menuItems.push({ label: Msgs.PARTY.get("b.boot"),
                             callback: bootMember, arg: peep.name.getMemberId() });
        }

        CommandMenu.createMenu(menuItems, _wctx.getTopPanel()).popUpAtMouse();
    }

    /**
     * Get the party board.
     */
    public function getPartyBoard (resultHandler :Function, query :String = null) :void
    {
        _pbsvc.getPartyBoard(_wctx.getClient(), query,
            new ResultAdapter(resultHandler, _wctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
    }

    /**
     * Create a new party.
     */
    public function createParty (name :String, groupId :int, inviteAllFriends :Boolean) :void
    {
        _pbsvc.createParty(_wctx.getClient(), name, groupId, inviteAllFriends,
            new ResultAdapter(visitPartyScene, _wctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
    }

    /**
     * Join a party.
     */
    public function joinParty (id :int) :void
    {
        _pbsvc.joinParty(_wctx.getClient(), id,
            new ResultAdapter(visitPartyScene, _wctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
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
            new ConfirmAdapter(handleLeaveParty, _wctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
    }

    public function assignLeader (memberId :int) :void
    {
        _partyObj.partyService.assignLeader(_wctx.getClient(), memberId,
            new ReportingListener(_wctx, MsoyCodes.PARTY_MSGS));
    }

    public function updateNameOrStatus (s :String, name :Boolean) :void
    {
        _partyObj.partyService.updateNameOrStatus(_wctx.getClient(), s, name,
            new ReportingListener(_wctx, MsoyCodes.PARTY_MSGS));
    }

    /**
     * Leaves the current party.
     */
    public function bootMember (memberId :int) :void
    {
        _partyObj.partyService.bootMember(_wctx.getClient(), memberId,
            new ReportingListener(_wctx, MsoyCodes.PARTY_MSGS));
    }

    protected function checkPartyId () :void
    {
        const partyId :int = _wctx.getMemberObject().partyId;
        if (partyId == 0 || (_partyObj != null && _partyObj.id != partyId)) {
            unsubscribeParty();
        }
        if (partyId != 0 && (_partyObj == null)) {
            _pbsvc.locateMyParty(_ctx.getClient(),
                new ResultAdapter(handleLocateParty, _wctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
        }
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
     * Handles the response from a locateMyParty() request.
     */
    protected function handleLocateParty (result :Object) :void
    {
        // we get either an int[] or an Integer back.
        if (result is Array) {
            subscribeParty(int(result[0]));
        } else {
            visitPartyScene(int(result));
        }
    }

    /**
     * Handles the respponse from a leaveParty() request.
     */
    protected function handleLeaveParty () :void
    {
        unsubscribeParty();

        // TODO: have the party popup pop itself down, or something
        var btn :CommandButton = WorldControlBar(_wctx.getControlBar()).partyBtn;
        if (btn.selected) {
            btn.activate(); // pop down the party window.
        }
    }

    protected function subscribeParty (oid :int) :void
    {
        unsubscribeParty(); // TODO: maybe noop if we're asked to subscribe to the same oid?

        _safeSubscriber = new SafeSubscriber(oid,
            new SubscriberAdapter(gotPartyObject, subscribeFailed));
        _safeSubscriber.subscribe(_ctx.getDObjectManager());
    }

    protected function unsubscribeParty () :void
    {
        if (_safeSubscriber == null) {
            return;
        }
        _safeSubscriber.unsubscribe(_ctx.getDObjectManager());
        _safeSubscriber = null;
        _partyObj.removeListener(_partyListener);
        _partyListener = null;
        _partyObj = null;
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
        var btn :CommandButton = WorldControlBar(_wctx.getControlBar()).partyBtn;
        if (btn.selected) {
            // click it down and then back up...
            btn.activate();
            btn.activate();
        }

        // we might need to warp to the party location
        checkFollowParty();
    }

    /**
     * Called when we've failed to subscribe to a party.
     */
    protected function subscribeFailed (oid :int, cause :ObjectAccessError) :void
    {
        // TODO
    }

    protected function partyAttrChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
        case PartyObject.SCENE_ID:
            checkFollowParty();
            break;
        }
    }

    protected function userAttrChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.PARTY_ID:
            checkPartyId();
        }
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);

        client.getClientObject().addListener(new AttributeChangeAdapter(userAttrChanged));
        checkPartyId();
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

    protected var _wctx :WorldContext;

    protected var _pbsvc :PartyBoardService;

    protected var _partyObj :PartyObject;

    protected var _safeSubscriber :SafeSubscriber;

    protected var _partyListener :ChangeListener;
}
}
