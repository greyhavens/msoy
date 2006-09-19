package com.threerings.msoy.item.client {

import mx.core.ContainerCreationPolicy;

import mx.core.UIComponent;

import mx.containers.TabNavigator;
import mx.containers.VBox;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.LazyTabNavigator;


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

        var tn :LazyTabNavigator = new LazyTabNavigator();
        addChild(tn);

        addTab(tn, "FURNITURE");
        addTab(tn, "PHOTO");
        addTab(tn, "DOCUMENT");
        addTab(tn, "GAME");
    }

    protected function addTab (tn :LazyTabNavigator, type :String) :void
    {
        tn.addTab(_ctx.xlate("item", "t.items_" + type),
            function () :UIComponent {
                return new InventoryList(_ctx, type);
            });
    }
}
}
