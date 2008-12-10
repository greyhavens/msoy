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
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.msoy.party.data.PartyMarshaller;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

// TODO: we shouldn't reach into 'world'. We should either be there, or do some other
// refiguring
import com.threerings.msoy.world.client.WorldContext;

/**
 * Manages party stuff on the client.
 */
public class PartyDirector extends BasicDirector
{
    // reference the PartyBoardMarshaller class
    PartyBoardMarshaller;

    public function PartyDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    public function isInParty () :Boolean
    {
        var clobj :Object = _mctx.getClient().getClientObject();
        return (clobj is MemberObject) && (MemberObject(clobj).partyId != 0);
    }

    public function isPartyLeader () :Boolean
    {
        return (_partyObj != null) && (_partyObj.leaderId == _mctx.getMyName().getMemberId());
    }

    /**
     * Create either a party board popup, or a party popup if we're already in a party.
     */
    public function createAppropriatePartyPanel () :FloatingPanel
    {
        if (_partyObj != null) {
            return new PartyPanel(_mctx, _partyObj);
        } else {
            return new PartyBoardPanel(_mctx);
        }
    }

    public function popPeepMenu (peep :PartyPeep) :void
    {
        var menuItems :Array = [];

        _mctx.getMsoyController().addMemberMenuItems(peep.name, menuItems);

        const peepId :int = peep.name.getMemberId();
        const ourId :int = _mctx.getMyName().getMemberId();
        if (_partyObj.leaderId == ourId && peepId != ourId) {
            menuItems.push({ type: "separator" });
            menuItems.push({ label: Msgs.PARTY.get("b.boot"),
                             callback: bootMember, arg: peep.name.getMemberId() });
        }

        CommandMenu.createMenu(menuItems, _mctx.getTopPanel()).popUpAtMouse();
    }

    /**
     * Get the party board.
     */
    public function getPartyBoard (resultHandler :Function, query :String = null) :void
    {
        _pbsvc.getPartyBoard(_mctx.getClient(), query,
            new ResultAdapter(resultHandler, _mctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
    }

    /**
     * Create a new party.
     */
    public function createParty (name :String, groupId :int, inviteAllFriends :Boolean) :void
    {
        _pbsvc.createParty(_mctx.getClient(), name, groupId, inviteAllFriends,
            new ResultAdapter(partyOidKnown, _mctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
    }

    /**
     * Join a party.
     */
    public function joinParty (id :int) :void
    {
        _pbsvc.joinParty(_mctx.getClient(), id,
            new ResultAdapter(partyOidKnown, _mctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
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
        _partyObj.partyService.leaveParty(_mctx.getClient(), new ConfirmAdapter(
            function () :void {
                _safeSubscriber.unsubscribe(_ctx.getDObjectManager());
                _safeSubscriber = null;
                _partyObj.removeListener(_partyListener);
                _partyListener = null;
                _partyObj = null;

                var btn :CommandButton = _mctx.getControlBar().partyBtn;
                if (btn.selected) {
                    btn.activate(); // pop down the party window.
                }
            }, _mctx.chatErrHandler(MsoyCodes.PARTY_MSGS)));
    }

    public function assignLeader (memberId :int) :void
    {
        _partyObj.partyService.assignLeader(_mctx.getClient(), memberId,
            new ReportingListener(_mctx, MsoyCodes.PARTY_MSGS));
    }

    public function updateStatus (status :String) :void
    {
        _partyObj.partyService.updateStatus(_mctx.getClient(), status,
            new ReportingListener(_mctx, MsoyCodes.PARTY_MSGS));
    }

    /**
     * Leaves the current party.
     */
    public function bootMember (memberId :int) :void
    {
        _partyObj.partyService.bootMember(_mctx.getClient(), memberId,
            new ReportingListener(_mctx, MsoyCodes.PARTY_MSGS));
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        super.registerServices(client);

        client.addServiceGroup(MsoyCodes.MEMBER_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _pbsvc = (client.requireService(PartyBoardService) as PartyBoardService);
    }

    protected function checkFollowParty () :void
    {
        if (_partyObj.sceneId != 0) {
            WorldContext(_mctx).getSceneDirector().moveTo(_partyObj.sceneId);
        }
    }

    /**
     * A success handler for creating and joining parties.
     */
    protected function partyOidKnown (oid :int) :void
    {
        _safeSubscriber = new SafeSubscriber(oid,
            new SubscriberAdapter(gotPartyObject, subscribeFailed));
        _safeSubscriber.subscribe(_ctx.getDObjectManager());
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
        var btn :CommandButton = _mctx.getControlBar().partyBtn;
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

    protected var _mctx :MsoyContext;

    protected var _pbsvc :PartyBoardService;

    protected var _partyObj :PartyObject;

    protected var _safeSubscriber :SafeSubscriber;

    protected var _partyListener :ChangeListener;
}
}
