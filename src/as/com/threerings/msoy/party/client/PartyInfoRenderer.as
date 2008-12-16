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

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

import com.threerings.msoy.party.data.PartyInfo;

public class PartyInfoRenderer extends HBox
{
    // Initialized by ClassFactory
    public var wctx :WorldContext;

    public function PartyInfoRenderer ()
    {
        addEventListener(MouseEvent.CLICK, handleClick);
    }

    override public function set data (value :Object) :void
    {
        super.data = value;
        if (value == null) {
            return;
        }
        var info :PartyInfo = PartyInfo(value);

        _name.text = info.name;
        _group.text = info.group.toString();
        _population.text = Msgs.PARTY.get("l.population", info.population);
        _status.text = Msgs.PARTY.xlate(info.status);

        var deets :String = "";
        var us :MemberObject = wctx.getMemberObject();
        if (us.friends.containsKey(info.leaderId)) {
            deets += "F";
        }
        if (us.groups != null && us.groups.containsKey(info.group.getGroupId())) {
            deets += "G";
        }
        if (deets != "") {
            deets += "  -  ";
        }
        deets += Msgs.PARTY.get("l.recruit_" + info.recruitment);
        _details.text = deets;

        _join.setCommand(WorldController.JOIN_PARTY, info.id);
    }

    override protected function createChildren () :void
    {
        var col :VBox = new VBox();
        col.addChild(_name);
        col.addChild(_group);
        addChild(col);

        col = new VBox();
        col.addChild(_status);
        col.addChild(_population);
        col.addChild(_details);
        addChild(col);

        addChild(_join);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        var info :PartyInfo = PartyInfo(data);
        if (info != null) {
            wctx.getPartyDirector().getPartyDetail(info.id);
        }
    }

    protected var _name :Label = FlexUtil.createLabel(null, "partyName");
    protected var _group :Label = FlexUtil.createLabel(null, "partyGroup");
    protected var _population :Label = FlexUtil.createLabel(null, "partyPopulation");
    protected var _status :Label = FlexUtil.createLabel(null, "partyStatus");
    protected var _details :Label = FlexUtil.createLabel("");
    protected var _join :CommandButton = new CommandButton(Msgs.PARTY.get("b.join"));
}
}
