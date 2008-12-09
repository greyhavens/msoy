//
// $Id$

package com.threerings.msoy.party.client {

import flash.events.MouseEvent;

import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.PlayerRenderer;

import com.threerings.msoy.party.data.PartyPeep;

public class PeepRenderer extends PlayerRenderer
{
    public function PeepRenderer ()
    {
        addEventListener(MouseEvent.CLICK, handleClick);
    }

    override protected function addCustomControls (content :VBox) :void
    {
        var peep :PartyPeep = PartyPeep(this.data);

        // TODO: add peepLabel style
        var name :Label = FlexUtil.createLabel(peep.name.toString(), "friendLabel");
        name.width = content.width;
        content.addChild(name);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        if (data != null) {
            mctx.getPartyDirector().popPeepMenu(PartyPeep(data));
        }
    }
}
}
