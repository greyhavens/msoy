//
// $Id$

package com.threerings.msoy.client {

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.item.client.InventoryPicker;
import com.threerings.msoy.item.web.Item;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

/**
 * Displays a list of the player's pets and allows them to be "called" into the current room.
 */
public class PetsDialog extends FloatingPanel
{
    public function PetsDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.pets"));
        _pets = new InventoryPicker(_ctx, Item.PET, true);
        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(MsoyUI.createLabel(Msgs.GENERAL.get("l.pets_tip")));

        _pets.percentWidth = 100;
        _pets.tree.dragEnabled = false;
        addChild(_pets);

        addButtons(OK_BUTTON);
    }

    protected var _pets :InventoryPicker;
}
}
