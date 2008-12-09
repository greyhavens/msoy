//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SubscriberAdapter;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.world.client.WorldController;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.msoy.party.data.PartyMarshaller;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

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

    public function getPeepMenuItems (peep :PartyPeep) :Array
    {
        var menuItems :Array = [];

        menuItems.push({ label: Msgs.GENERAL.get("b.open_channel"),
                         command: WorldController.OPEN_CHANNEL, arg: peep.name });
        menuItems.push({ label: Msgs.GENERAL.get("b.view_member"),
                         command: WorldController.VIEW_MEMBER, arg: peep.name.getMemberId() });

        if (_partyObj.leaderId == _mctx.getMyName().getMemberId()) {
            menuItems.push({ label: Msgs.PARTY.get("b.boot_peep"),
                             callback: bootMember, arg: peep.name.getMemberId() });
        }

        return menuItems;
    }

    /**
     * Get the party board.
     */
    public function getPartyBoard (resultHandler :Function, query :String = null) :void
    {
        _pbsvc.getPartyBoard(_mctx.getClient(), query,
            new ResultAdapter(_mctx.chatErrHandler(MsoyCodes.PARTY_MSGS), resultHandler));
    }

    /**
     * Create a new party.
     */
    public function createParty (name :String, groupId :int) :void
    {
        _pbsvc.createParty(_mctx.getClient(), name, groupId,
            new ResultAdapter(_mctx.chatErrHandler(MsoyCodes.PARTY_MSGS), partyOidKnown));
    }

    /**
     * Join a party.
     */
    public function joinParty (id :int) :void
    {
        _pbsvc.joinParty(_mctx.getClient(), id,
            new ResultAdapter(_mctx.chatErrHandler(MsoyCodes.PARTY_MSGS), partyOidKnown));
    }

    /**
     * Leaves the current party.
     */
    public function leaveParty () :void
    {
        _partyObj.partyService.leaveParty(_mctx.getClient(), new ConfirmAdapter(
            _mctx.chatErrHandler(MsoyCodes.PARTY_MSGS),
            function () :void {
                trace("Left the party"); // TODO: Close the party window, other stuff
            }));
    }

    public function assignLeader (memberId :int) :void
    {
        _partyObj.partyService.assignLeader(_mctx.getClient(), memberId,
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

    /**
     * A success handler for creating and joining parties.
     */
    protected function partyOidKnown (oid :int) :void
    {
        _safeSubscriber = new SafeSubscriber(oid,
            new SubscriberAdapter(gotPartyObject, subscribeFailed));
        _safeSubscriber.subscribe(_ctx.getDObjectManager());
    }

    protected function gotPartyObject (obj :PartyObject) :void
    {
        _partyObj = obj;

        // if the party popup is up, change to the new popup...
        var btn :CommandButton = _mctx.getControlBar().partyBtn;
        if (btn.selected) {
            // click it down and then back up...
            btn.activate();
            btn.activate();
        }
    }

    protected function subscribeFailed (oid :int, cause :ObjectAccessError) :void
    {
        // TODO
    }

    protected var _mctx :MsoyContext;

    protected var _pbsvc :PartyBoardService;

    protected var _partyObj :PartyObject;

    protected var _safeSubscriber :SafeSubscriber;
}
}
