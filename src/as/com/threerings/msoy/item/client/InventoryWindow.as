package com.threerings.msoy.item.client {

import mx.binding.utils.BindingUtils;

import mx.core.ContainerCreationPolicy;

import mx.core.UIComponent;

import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.ComboBox;

import com.threerings.util.ArrayUtil;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.LazyContainer;
import com.threerings.msoy.ui.MsoyUI;

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

        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("item", "l.types")),
            _type = new ComboBox());
        addChild(grid);

        // get all the item types
        var itemTypes :Array = ItemEnum.values();
        // move furniture to be the first
        ArrayUtil.removeFirst(itemTypes, ItemEnum.FURNITURE);
        itemTypes.unshift(ItemEnum.FURNITURE);

        addChild(_lists = new ViewStack());
        var typeLabels :Array = [];
        for each (var itemType :ItemEnum in itemTypes) {
            addList(itemType);
            typeLabels.push(
                _ctx.xlate("item", "t.items_" + itemType.getStringCode()));
        }
        _type.dataProvider = typeLabels;

        // wire up the combobox to select items from the viewstack
        BindingUtils.bindProperty(_lists, "selectedIndex",
            _type, "selectedIndex");
    }

    protected function addList (type :ItemEnum) :void
    {
        _lists.addChild(new LazyContainer(function () :UIComponent {
            return new InventoryList(_ctx, type);
        }));
    }

    /** The item type to display. */
    protected var _type :ComboBox;

    protected var _lists :ViewStack;
}
}
