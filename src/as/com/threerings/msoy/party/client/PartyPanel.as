//
// $Id$

package com.threerings.msoy.party.client {

import mx.collections.ArrayCollection;
import mx.controls.TextInput;
import mx.events.FlexEvent;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandComboBox;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Roster;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.party.data.PartyCodes;
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

        var isLeader :Boolean = (_partyObj.leaderId == _ctx.getMyName().getMemberId());

        _name = new TextInput();
        _name.styleName = "partyTitle";
        _name.maxChars = PartyCodes.MAX_NAME_LENGTH;
        _name.percentWidth = 100;
        _name.text = _partyObj.name;
        _name.enabled = isLeader;
        _name.addEventListener(FlexEvent.ENTER, commitName);
        addChild(_name);

        _roster = new Roster(_ctx, PartyObject.PEEPS, PeepRenderer,
            PartyPeep.createSortByOrder(_partyObj));
        addChild(_roster);

        addChild(new CommandButton(Msgs.PARTY.get("b.leave"), _wctx.getPartyDirector().leaveParty));

        _status = new TextInput();
        _status.styleName = "partyStatus";
        _status.maxChars = PartyCodes.MAX_NAME_LENGTH;
        _status.percentWidth = 100;
        _status.text = _partyObj.status;
        _status.enabled = isLeader;
        _status.addEventListener(FlexEvent.ENTER, commitStatus);
        addChild(_status);

        var options :Array = [];
        for (var ii :int = 0; ii < PartyCodes.RECRUITMENT_COUNT; ii++) {
            options.push({ label: Msgs.PARTY.get("l.recruitment_" + ii), data: ii });
        }
        _recruiting = new CommandComboBox(_wctx.getPartyDirector().updateRecruiting);
        _recruiting.dataProvider = options;
        _recruiting.selectedIndex = _partyObj.recruiting;
        _recruiting.enabled = isLeader;
        addChild(_recruiting);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
            case PartyObject.STATUS:
                _status.text = String(event.getValue());
                break;

            case PartyObject.LEADER_ID:
                var isLeader :Boolean = (event.getValue() == _ctx.getMyName().getMemberId());
                _name.enabled = isLeader;
                _status.enabled = isLeader;
                _recruiting.enabled = isLeader;
                break;

            case PartyObject.RECRUITING:
                // This will be a problem if the values of RECRUITMENT_* don't match up to indexes
                _recruiting.selectedIndex = int(event.getValue());
        }
    }

    protected function commitName (event :FlexEvent) :void
    {
        _wctx.getPartyDirector().updateNameOrStatus(_name.text, true);
        _wctx.getControlBar().giveChatFocus();
    }

    protected function commitStatus (event :FlexEvent) :void
    {
        _wctx.getPartyDirector().updateNameOrStatus(_status.text, false);
        _wctx.getControlBar().giveChatFocus();
    }

    protected var _wctx :WorldContext;

    protected var _partyObj :PartyObject;

    protected var _roster :Roster;
    protected var _name :TextInput;
    protected var _status :TextInput;
    protected var _recruiting :CommandComboBox;
}

}
