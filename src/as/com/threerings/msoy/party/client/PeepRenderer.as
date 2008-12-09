//
// $Id$

package com.threerings.msoy.party.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.Label;

import com.threerings.flex.CommandMenu;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.party.data.PartyPeep;

public class PeepRenderer extends HBox
{
    // Initialized by ClassFactory
    public var mctx :MsoyContext;

    public function PeepRenderer ()
    {
        addEventListener(MouseEvent.CLICK, handleClick);
    }

    override public function set data (value :Object) :void
    {
        super.data = value;

        if (value != null) {
            var peep :PartyPeep = PartyPeep(value);
            _label.text = peep.name.toString();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        _label = new Label();
        addChild(_label);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        if (data != null) {
            mctx.getPartyDirector().popPeepMenu(PartyPeep(data));
        }
    }

    protected var _label :Label;
}
}
