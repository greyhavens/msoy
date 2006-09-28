package com.threerings.msoy.item.client {

import mx.core.ContainerCreationPolicy;

import mx.core.UIComponent;

import mx.containers.TabNavigator;
import mx.containers.VBox;

import com.threerings.util.ArrayUtil;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.LazyTabNavigator;

import com.threerings.msoy.item.util.ItemEnum;

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

        // get all the item types
        var itemTypes :Array = ItemEnum.values();
        // move furniture to be the first
        ArrayUtil.removeFirst(itemTypes, ItemEnum.FURNITURE);
        itemTypes.unshift(ItemEnum.FURNITURE);

        for each (var itemType :ItemEnum in itemTypes) {
            addTab(tn, itemType.getStringCode());
        }
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
