package com.threerings.msoy.item.client {

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * A simple in-game panel that shows inventory and acts as a drag source
 * for scene editing.
 */
public class InventoryWindow extends FloatingPanel
{
    public function InventoryWindow (ctx :MsoyContext)
    {
        super(ctx, Msgs.ITEM.get("t.inventory"));
        showCloseButton = true;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(new InventoryDisplay(_ctx));
    }
}
}
