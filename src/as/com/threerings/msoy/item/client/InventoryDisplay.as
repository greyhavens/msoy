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

import com.threerings.msoy.item.web.Item;

/**
 * A simple in-game panel that shows inventory and acts as a drag source
 * for scene editing.
 */
public class InventoryDisplay extends VBox
{
    public function InventoryDisplay (
        ctx :MsoyContext,
        showUsed :Boolean = false, showUnused :Boolean = true)
    {
        _ctx = ctx;
        _showUsed = showUsed;
        _showUnused = showUnused;
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
        var itemTypes :Array = Item.getTypes();
        // move furniture to be the first
        ArrayUtil.removeFirst(itemTypes, Item.FURNITURE);
        itemTypes.unshift(Item.FURNITURE);

        addChild(_lists = new ViewStack());
        var typeLabels :Array = [];
        for each (var itemType :int in itemTypes) {
            addList(itemType);
            typeLabels.push(
                _ctx.xlate("item", "t.items_" + Item.getTypeName(itemType)));
        }
        _type.dataProvider = typeLabels;

        // wire up the combobox to select items from the viewstack
        BindingUtils.bindProperty(_lists, "selectedIndex",
            _type, "selectedIndex");
    }

    protected function addList (type :int) :void
    {
        _lists.addChild(new LazyContainer(function () :UIComponent {
            return new InventoryList(_ctx, type, _showUsed, _showUnused);
        }));
    }

    protected var _ctx :MsoyContext;

    /** The item type to display. */
    protected var _type :ComboBox;

    /** Should we show items that are currently in use? */
    protected var _showUsed :Boolean;

    /** Should we show items that are not currently in use? */
    protected var _showUnused :Boolean;

    protected var _lists :ViewStack;
}
}
