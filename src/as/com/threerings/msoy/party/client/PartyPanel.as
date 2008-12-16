//
// $Id$

package com.threerings.msoy.party.client {

import mx.collections.ArrayCollection;
import mx.controls.TextInput;
import mx.events.FlexEvent;

import com.threerings.util.Log;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandComboBox;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Roster;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PartyPanel extends FloatingPanel
    implements AttributeChangeListener
{
    public function PartyPanel (ctx :WorldContext, partyObj :PartyObject)
    {
        super(ctx, partyObj.name);
        _wctx = ctx;
        showCloseButton = true;
        styleName = "partyPanel";

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

        _roster = new Roster(_ctx, PartyObject.PEEPS, PeepRenderer,
            PartyPeep.createSortByOrder(_partyObj));
        addChild(_roster);

        addChild(new CommandButton(Msgs.PARTY.get("b.leave"), _wctx.getPartyDirector().leaveParty));

        _status = new TextInput();
        _status.styleName = "partyStatus";
        _status.maxChars = PartyCodes.MAX_NAME_LENGTH;
        _status.percentWidth = 100;
        _status.text = Msgs.PARTY.xlate(_partyObj.status);
        _status.enabled = isLeader;
        _status.addEventListener(FlexEvent.ENTER, commitStatus);
        addChild(_status);

        var options :Array = [];
        for (var ii :int = 0; ii < PartyCodes.RECRUITMENT_COUNT; ii++) {
            options.push({ label: Msgs.PARTY.get("l.recruit_" + ii), data: ii });
        }
        _recruit = new CommandComboBox(_wctx.getPartyDirector().updateRecruitment);
        _recruit.dataProvider = options;
        _recruit.selectedData = _partyObj.recruitment;
        _recruit.enabled = isLeader;
        addChild(_recruit);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
            case PartyObject.STATUS:
                _status.text = Msgs.PARTY.xlate(String(event.getValue()));
                break;

            case PartyObject.LEADER_ID:
                var isLeader :Boolean = (event.getValue() == _ctx.getMyName().getMemberId());
                _status.enabled = isLeader;
                _recruit.enabled = isLeader;
                // re-sort the list
                _roster.dataProvider.refresh();
                break;

            case PartyObject.RECRUITMENT:
                _recruit.selectedData = int(event.getValue());
                break;
        }
    }

    protected function commitStatus (event :FlexEvent) :void
    {
        _wctx.getPartyDirector().updateStatus(_status.text);
        _wctx.getControlBar().giveChatFocus();
    }

    protected var _wctx :WorldContext;

    protected var _partyObj :PartyObject;

    protected var _roster :Roster;
    protected var _status :TextInput;
    protected var _recruit :CommandComboBox;
}
}
