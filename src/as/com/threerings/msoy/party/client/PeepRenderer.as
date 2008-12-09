//
// $Id$

package com.threerings.msoy.party.client {

import flash.events.MouseEvent;

import mx.containers.HBox;

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
    }

    override protected function createChildren () :void
    {
        super.createChildren();
    }

    protected function handleClick (event :MouseEvent) :void
    {
        var menuItems :Array = mctx.getPartyDirector().getPeepMenuItems(_peep);
        CommandMenu.createMenu(menuItems, mctx.getTopPanel()).popUpAtMouse();
    }

    protected var _peep :PartyPeep;
}

}
