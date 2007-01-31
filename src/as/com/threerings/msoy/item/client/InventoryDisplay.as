package com.threerings.msoy.item.client {

import flash.events.Event;

import mx.binding.utils.BindingUtils;

import mx.core.ContainerCreationPolicy;

import mx.core.UIComponent;

import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.ComboBox;

import com.threerings.util.ArrayUtil;
import com.threerings.util.CommandEvent;

import com.threerings.msoy.client.Msgs;
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
    public static const ITEM_SELECTED :String = "InventoryItemSelected";

    public function InventoryDisplay (
        ctx :MsoyContext,
        showUsed :Boolean = false, showUnused :Boolean = true)
    {
        _ctx = ctx;
        _showUsed = showUsed;
        _showUnused = showUnused;
    }

    public function getSelectedItem () :Item
    {
        // and dispatch an update status of our selection
        var iList :InventoryList =
            (_lists[_listsView.selectedIndex] as InventoryList);
        return (iList == null) ? null : (iList.selectedItem as Item);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(Msgs.ITEM.get("l.types")),
            _type = new ComboBox());
        addChild(grid);

        // get all the item types
        var itemTypes :Array = Item.getTypes();
        // move furniture to be the first
        ArrayUtil.removeFirst(itemTypes, Item.FURNITURE);
        itemTypes.unshift(Item.FURNITURE);

        addChild(_listsView = new ViewStack());
        var typeLabels :Array = [];
        var index :int = 0;
        for each (var itemType :int in itemTypes) {
            addList(itemType, index++);
            typeLabels.push(
                Msgs.ITEM.get("t.items_" + Item.getTypeName(itemType)));
        }
        _type.dataProvider = typeLabels;

        // wire up the combobox to select items from the viewstack
        BindingUtils.bindSetter(setSelectedIndex, _type, "selectedIndex");
    }

    protected function addList (type :int, index :int) :void
    {
        _listsView.addChild(new LazyContainer(function () :UIComponent {
            var iList :InventoryList =
                new InventoryList(_ctx, type, _showUsed, _showUnused);
            _lists[index] = iList;
            iList.addEventListener(Event.CHANGE, itemChanged);
            return iList;
        }));
    }

    protected function setSelectedIndex (index :int) :void
    {
        if (_listsView.selectedIndex == index) {
            return; // nada
        }

        // show the right list
        _listsView.selectedIndex = index;

        // and broadcast the update to what's selected
        CommandEvent.dispatch(this, ITEM_SELECTED, getSelectedItem());
    }

    /**
     * Handles CHANGE events from our sub-lists.
     */
    protected function itemChanged (event :Event) :void
    {
        var iList :InventoryList = (event.currentTarget as InventoryList);
        CommandEvent.dispatch(this, ITEM_SELECTED, iList.selectedItem as Item);
    }

    protected var _ctx :MsoyContext;

    /** The item type to display. */
    protected var _type :ComboBox;

    /** Should we show items that are currently in use? */
    protected var _showUsed :Boolean;

    /** Should we show items that are not currently in use? */
    protected var _showUnused :Boolean;

    protected var _listsView :ViewStack;

    /** A sparse array containing each instantiated InventoryList. */
    protected var _lists :Array = new Array();
}
}
