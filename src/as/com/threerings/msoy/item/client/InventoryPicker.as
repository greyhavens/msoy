//
// $Id$

package com.threerings.msoy.item.client {

import flash.events.Event;

import mx.binding.utils.BindingUtils;

import mx.core.ClassFactory;

import mx.containers.VBox;

import mx.controls.CheckBox;
import mx.controls.Tree;

import mx.collections.ArrayCollection;
import mx.collections.ICollectionView;
import mx.collections.ListCollectionView;
import mx.collections.Sort;

import mx.core.ScrollPolicy;

import mx.events.TreeEvent;

import com.threerings.mx.events.CommandEvent

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.web.Item;

public class InventoryPicker extends VBox
{
    /** A CommandEvent dispatched when a new selection is made. The arg
     * is the Item instance, if any. */
    public static const ITEM_SELECTED :String = "InventoryItemSelected";

    public function InventoryPicker (
        ctx :MsoyContext, soleType :int = Item.NOT_A_TYPE,
        showUsed :Boolean = false)
    {
        _collection = new InventoryCollectionView(ctx, soleType, showUsed);
    }

    /**
     * Get the currently-selected item, or null.
     */
    public function getSelectedItem () :Item
    {
        return (_tree.selectedItem as Item);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _tree = new Tree();
        _tree.verticalScrollPolicy = ScrollPolicy.ON;
        _tree.variableRowHeight = true;
        _tree.dragEnabled =  true;
        _tree.dragMoveEnabled = false;
        _tree.itemRenderer = new ClassFactory(ItemTreeRenderer);
        _tree.dataProvider = _collection;

        _tree.percentWidth = 100;
        _tree.percentHeight = 100;

        _tree.addEventListener(TreeEvent.ITEM_OPENING, handleItemOpening);
        _tree.addEventListener(Event.CHANGE, handleItemSelected);

        var showUsed :CheckBox = new CheckBox();
        showUsed.label = "Show Used?";
        showUsed.selected = _collection.isShowUsed();

        addChild(showUsed);
        addChild(_tree);

        // and bind the checkbox to the showing of used items...
        BindingUtils.bindSetter(_collection.setShowUsed, showUsed, "selected");
    }

    protected function handleItemOpening (event :TreeEvent) :void
    {
        if (event.opening) {
            _collection.typeOpened(event.item);

        } else if (_collection.length == 1) {
            // if there's only one type showing, prevent closing
            event.preventDefault();
        }
    }

    protected function handleItemSelected (event :Event) :void
    {
        CommandEvent.dispatch(this, ITEM_SELECTED, getSelectedItem());
    }

    /** The tree that is displaying our inventory. */
    protected var _tree :Tree;

    protected var _collection :InventoryCollectionView;
}
}
