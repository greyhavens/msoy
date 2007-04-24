//
// $Id$

package com.threerings.msoy.item.client {

import flash.events.Event;

import mx.binding.utils.BindingUtils;
import mx.containers.VBox;
import mx.core.ClassFactory;

import mx.controls.CheckBox;
import mx.controls.Tree;

import mx.collections.ArrayCollection;
import mx.collections.ICollectionView;
import mx.collections.ListCollectionView;
import mx.collections.Sort;

import mx.core.ScrollPolicy;

import mx.events.DragEvent;
import mx.events.TreeEvent;

import com.threerings.util.CommandEvent

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.data.all.Item;

public class InventoryPicker extends VBox
{
    /** A CommandEvent dispatched when a new selection is made. The arg
     * is the Item instance, if any. */
    public static const ITEM_SELECTED :String = "InventoryItemSelected";

    /**
     * Returns the Item being dragged, or null if not an item or not the right kind of drag, or
     * whatever.
     */
    public static function dragItem (event :DragEvent) :Item
    {
        // this is internal flex juju, not related to our items
        for each (var format :String in ["items", "treeItems"]) {
            if (event.dragSource.hasFormat(format)) {
                var arr :Array =
                    (event.dragSource.dataForFormat(format) as Array);
                if ((arr.length == 1) && (arr[0] is Item)) {
                    var item :Item = (arr[0] as Item);
                    // we only accept drops of un-utilized items
                    if (!item.isUsed()) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Create an inventory picker.
     *
     * @param includedTypes if specified, the list of item types to show, and excludeTypes is
     *                      ignored.
     * @param excludeTypes if includedTypes == null, may contain item types to exclude.
     */
    public function InventoryPicker (
        ctx :WorldContext, includedTypes :Array = null, excludedTypes :Array = null,
        showUsed :Boolean = false)
    {
        _collection = new InventoryCollectionView(ctx, includedTypes, excludedTypes, showUsed);

        _tree = new InventoryTree();
        _tree.verticalScrollPolicy = ScrollPolicy.ON;
        _tree.variableRowHeight = true;
        _tree.dragEnabled =  true;
        _tree.dragMoveEnabled = false;
        _tree.itemRenderer = new ClassFactory(ItemTreeRenderer);
        _tree.dataProvider = _collection;

        _tree.percentWidth = 100;
        _tree.percentHeight = 100;

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    /**
     * Adds a special item to be shown alongside the user's inventory items.
     */
    public function addFakeItem (fake :Item) :void
    {
        _collection.addFakeItem(fake);
    }

    /**
     * Get access directly to the tree widget, for customizing behavior.
     */
    public function get tree () :Tree
    {
        return _tree;
    }

    /**
     * Get the currently-selected item, or null.
     */
    public function getSelectedItem () :Item
    {
        return (_tree.selectedItem as Item);
    }

    /**
     * Set the currently-selected item.
     */
    public function setSelectedItem (item :Item) :void
    {
        _tree.selectedItem = item;
        var idx :int = _tree.selectedIndex;
        if (idx != -1) {
            _tree.scrollToIndex(idx);
        }
    }

    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            _collection.startup();

            // if we're only showing one category, open it.
            if (_collection.length == 1) {
                callLater(_tree.expandItem, [ _collection[0], true ]);
            }

        } else {
            _collection.shutdown();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _tree.addEventListener(TreeEvent.ITEM_OPENING, handleItemOpening);
        _tree.addEventListener(Event.CHANGE, handleItemSelected);

        var showUsed :CheckBox = new CheckBox();
        showUsed.label = Msgs.ITEM.get("l.show_used");
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

    /** The collection representing our inventory. */
    protected var _collection :InventoryCollectionView;
}
}
