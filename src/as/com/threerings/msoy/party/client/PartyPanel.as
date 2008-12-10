//
// $Id$

package com.threerings.msoy.party.client {

import mx.controls.TextInput;
import mx.events.FlexEvent;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Roster;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PartyPanel extends FloatingPanel
    implements AttributeChangeListener
{
    public function PartyPanel (ctx :WorldContext, partyObj :PartyObject)
    {
        super(ctx, Msgs.PARTY.get("t.party"));
        _wctx = ctx;
        showCloseButton = true;

        _partyObj = partyObj;
    }

    override public function close () :void
    {
        _partyObj.removeListener(_roster);
        _partyObj.removeListener(this);

        super.close();
    }

    override protected function didOpen () :void
    {
        super.didOpen();

        _roster.init(_partyObj.peeps.toArray());
        _partyObj.addListener(_roster);
        _partyObj.addListener(this);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _roster = new Roster(_ctx, PartyObject.PEEPS, PeepRenderer,
            PartyPeep.createSortByOrder(_partyObj));
        addChild(_roster);

        addChild(new CommandButton(Msgs.PARTY.get("b.leave"), _wctx.getPartyDirector().leaveParty));

        _status = new TextInput();
        _status.percentWidth = 100;
        _status.styleName = "statusEdit"; // Punked from FriendsListPanel
        _status.text = _partyObj.status;
        _status.editable = (_partyObj.leaderId == _ctx.getMyName().getMemberId());
        _status.addEventListener(FlexEvent.ENTER, commitStatus);
        addChild(_status);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
            case PartyObject.STATUS:
                _status.text = String(event.getValue());
                break;

            case PartyObject.LEADER_ID:
                var leaderId :int = int(event.getValue());
                _status.editable = (leaderId == _ctx.getMyName().getMemberId());
                break;
        }
    }

    protected function commitStatus (event :FlexEvent) :void
    {
        _wctx.getPartyDirector().updateStatus(_status.text);
    }

    protected var _wctx :WorldContext;

    protected var _partyObj :PartyObject;

    protected var _roster :Roster;
    protected var _status :TextInput;
}

}
