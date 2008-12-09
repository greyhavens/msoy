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
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Roster;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PartyPanel extends FloatingPanel
    implements AttributeChangeListener
{
    public function PartyPanel (ctx :MsoyContext, partyObj :PartyObject)
    {
        super(ctx, Msgs.PARTY.get("t.party"));
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

        addChild(new CommandButton(Msgs.PARTY.get("b.leave"), _ctx.getPartyDirector().leaveParty));

        _status = new TextInput();
        _status.percentWidth = 100;
        _status.styleName = "statusEdit"; // Punked from FriendsListPanel
        _status.text = _partyObj.status;
        _status.addEventListener(FlexEvent.ENTER, commitStatus);
        addChild(_status);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == PartyObject.STATUS) {
            _status.text = String(event.getValue());
        }
    }

    protected function commitStatus (event :FlexEvent) :void
    {
        _ctx.getPartyDirector().updateStatus(_status.text);
    }


    protected var _partyObj :PartyObject;

    protected var _roster :Roster;
    protected var _status :TextInput;
}

}
