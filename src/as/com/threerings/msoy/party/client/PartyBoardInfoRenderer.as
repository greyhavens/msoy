//
// $Id$

package com.threerings.msoy.party.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

import com.threerings.msoy.party.data.PartyBoardInfo;

public class PartyBoardInfoRenderer extends HBox
{
    // Initialized by ClassFactory
    public var wctx :WorldContext;

    public function PartyBoardInfoRenderer ()
    {
        addEventListener(MouseEvent.CLICK, handleClick);
    }

    override public function set data (value :Object) :void
    {
        super.data = value;
        if (value == null) {
            return;
        }
        var party :PartyBoardInfo = PartyBoardInfo(value);

        _picHolder.removeAllChildren();
        _picHolder.addChild(MediaWrapper.createView(party.icon, MediaDesc.QUARTER_THUMBNAIL_SIZE));

        _name.text = party.info.name;
        _population.text = String(party.info.population);
        _status.text = Msgs.PARTY.xlate(party.info.status);

        var us :MemberObject = wctx.getMemberObject();
        _name.setStyle("fontWeight",
            us.friends.containsKey(party.info.leaderId) ? "bold" : "normal");

        _join.setCommand(WorldController.JOIN_PARTY, party.info.id);
    }

    override protected function createChildren () :void
    {
        addChild(_picHolder);
        addChild(_name);
        _name.width = 150;
        addChild(_population);
        _population.width = 20;
        addChild(_status);
        _status.width = 110;
        addChild(_join);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        var party :PartyBoardInfo = PartyBoardInfo(data);
        if (party != null) {
            wctx.getPartyDirector().getPartyDetail(party.info.id);
        }
    }

    protected var _picHolder :VBox = new VBox();
    protected var _name :Label = FlexUtil.createLabel(null, "partyName");
    protected var _population :Label = FlexUtil.createLabel(null, "partyPopulation");
    protected var _status :Label = FlexUtil.createLabel(null, "partyStatus");
    protected var _join :CommandButton = new CommandButton(Msgs.PARTY.get("b.join"));
}
}
