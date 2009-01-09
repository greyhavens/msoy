//
// $Id$

package com.threerings.msoy.party.client {

import flash.events.MouseEvent;

import mx.containers.VBox;
import mx.controls.Label;

import mx.core.ClassFactory;
import mx.core.IFactory;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.PlayerRenderer;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PeepRenderer extends PlayerRenderer
{
    /**
     * Optional: an object with the following fields:
     * leaderId: (int) the leader of the party.
     */
    public var partyInfo :Object;

    /**
     * Return a factory for use with this renderer.
     */
    public static function createFactory (ctx :WorldContext, partyInfo :Object = null) :IFactory
    {
        var cf :ClassFactory = new ClassFactory(PeepRenderer);
        cf.properties = { mctx: ctx, partyInfo: partyInfo };
        return cf;
    }

    public function PeepRenderer ()
    {
        addEventListener(MouseEvent.CLICK, handleClick);
    }

    override protected function configureUI () :void
    {
        super.configureUI();

        var isLeader :Boolean = (this.data != null) &&
            (partyInfo.leaderId == PartyPeep(this.data).name.getMemberId());
        setStyle("backgroundAlpha", isLeader ? .5 : 0);
        setStyle("backgroundColor", isLeader ? 0x000077 : 0x000000);
    }

    override protected function addCustomControls (content :VBox) :void
    {
        var peep :PartyPeep = PartyPeep(this.data);

        var name :Label = FlexUtil.createLabel(peep.name.toString(), "playerLabel");
        name.width = content.width;
        content.addChild(name);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        if (data != null) {
            WorldContext(mctx).getPartyDirector().popPeepMenu(PartyPeep(data), partyInfo.id);
        }
    }

    override protected function getIconSize () :int
    {
        return MediaDesc.QUARTER_THUMBNAIL_SIZE;
    }
}
}
