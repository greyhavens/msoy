package com.threerings.msoy.item.client {

import mx.containers.TabNavigator;
import mx.containers.VBox;

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
        super(ctx, ctx.xlate("item", "t.inventory"));
        showCloseButton = true;

        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var tn :TabNavigator = new TabNavigator();
        addChild(tn);

        addTab(tn, "FURNITURE");
        addTab(tn, "PHOTO");
        addTab(tn, "DOCUMENT");
        addTab(tn, "GAME");
    }

    protected function addTab (tn :TabNavigator, type :String) :void
    {
        var box :VBox = new VBox();
        box.label = _ctx.xlate("item", "t.items_" + type);
        box.addChild(new InventoryList(_ctx, type));
        tn.addChild(box);
    }
}
}
