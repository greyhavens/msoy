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

        // TODO: due to the way items are classified and used,
        // this should probably not be presented as tabs to the user.
        // Rather, a more appropriate UI might be to have a pull-down
        // box that lists item types and have the resultant types
        // displayed below (use a ViewStack with a ComboBox).
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
