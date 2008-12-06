//
// $Id$

package com.threerings.msoy.party.client {

import mx.containers.HBox;
import mx.controls.Label;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.party.data.PartyInfo;

public class PartyRenderer extends HBox
{
    // Initialized by ClassFactory
    public var mctx :MsoyContext;

    override public function set data (party :Object) :void
    {
        _name.text = PartyInfo(party).name;
    }

    override protected function createChildren () :void
    {
        addChild(_name);
    }

    protected var _name :Label = new Label();
}

}
