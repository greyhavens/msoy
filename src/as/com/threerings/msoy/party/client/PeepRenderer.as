//
// $Id$

package com.threerings.msoy.party.client {

import flash.events.MouseEvent;

import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.PlayerRenderer;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PeepRenderer extends PlayerRenderer
{
    public var partyObj :PartyObject;

    public function PeepRenderer ()
    {
        addEventListener(MouseEvent.CLICK, handleClick);
    }

    override protected function configureUI () :void
    {
        super.configureUI();

        var isLeader :Boolean = (this.data != null) &&
            (partyObj.leaderId == PartyPeep(this.data).name.getMemberId());
        setStyle("backgroundAlpha", isLeader ? .5 : 0);
        setStyle("backgroundColor", isLeader ? 0x000077 : 0x000000);
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
            WorldContext(mctx).getPartyDirector().popPeepMenu(PartyPeep(data));
        }
    }

    override protected function getIconSize () :int
    {
        return MediaDesc.QUARTER_THUMBNAIL_SIZE;
    }
}
}
