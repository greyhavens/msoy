//
// $Id$

package com.threerings.msoy.party.client {

import mx.containers.HBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.party.data.PartyInfo;

public class PartyInfoRenderer extends HBox
{
    // Initialized by ClassFactory
    public var mctx :MsoyContext;

    override public function set data (value :Object) :void
    {
        super.data = value;
        if (value == null) {
            return;
        }
        var info :PartyInfo = PartyInfo(value);
        _name.text = info.name;
        _join.setCallback(mctx.getPartyDirector().joinParty, info.id);
    }

    override protected function createChildren () :void
    {
        addChild(_name);
        addChild(_join);
    }

    protected var _name :Label = new Label();
    protected var _join :CommandButton = new CommandButton(Msgs.PARTY.get("b.join"));
}
}
