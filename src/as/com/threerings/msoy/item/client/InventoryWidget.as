//
// $Id$

package com.threerings.msoy.item.client {

import flash.events.Event;

import mx.core.ClassFactory;

import mx.controls.Tree;

import mx.collections.ArrayCollection;
import mx.collections.ICollectionView;
import mx.collections.ListCollectionView;
import mx.collections.Sort;

import mx.core.ScrollPolicy;

import mx.events.TreeEvent;

import com.threerings.mx.events.CommandEvent

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.web.Item;

public class InventoryWidget extends Tree
{
    /** A CommandEvent dispatched when a new selection is made. The arg
     * is the Item instance, if any. */
    public static const ITEM_SELECTED :String = "InventoryItemSelected";

    public function InventoryWidget (
        ctx :MsoyContext, soleType :int = Item.NOT_A_TYPE,
        showUsed :Boolean = false)
    {
        _ctx = ctx;
        _showUsed = showUsed;

        verticalScrollPolicy = ScrollPolicy.ON;
        variableRowHeight = true;
        dragEnabled = true;
        dragMoveEnabled = false;
        allowMultipleSelection = true;
        itemRenderer = new ClassFactory(ItemTreeRenderer);

        addEventListener(TreeEvent.ITEM_OPENING, handleItemOpening);
        addEventListener(Event.CHANGE, handleChange);
        configureItemCategories(soleType);
    }

    /**
     * Get the currently selected item.
     */
    public function getSelectedItem () :Item
    {
        return (selectedItem as Item); // will return null if it's not.
    }

    /**
     * Set whether or not we show items that are 'used'.
     */
    public function setShowUsed (showUsed :Boolean) :void
    {
        if (showUsed != _showUsed) {
            _showUsed = showUsed;
            refresh();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var coll :ListCollectionView = ListCollectionView(dataProvider);
        if (coll.length == 1) {
            loadCategory(coll.getItemAt(0));
        }
    }

    /**
     * Set up the top-level nodes of the tree.
     */
    protected function configureItemCategories (soleType :int) :void
    {
        var itemTypes :Array;

        if (soleType == Item.NOT_A_TYPE) {
            itemTypes = Item.getTypes();
        } else {
            itemTypes = [ soleType ];
        }

        var sort :Sort = new Sort();
        sort.compareFunction = sortItems;

        var nodes :ArrayCollection = new ArrayCollection();
        for each (var itemType :int in itemTypes) {
            var childs :ArrayCollection = new ArrayCollection();
            childs.filterFunction = filterItems;
            childs.sort = sort;
            childs.addItem(Msgs.ITEM.get("m.retrieving"));
            var desc :Object = {
                label: Msgs.ITEM.get("t.items_" + Item.getTypeName(itemType)),
                itemType: itemType,
                children: childs
            };
            nodes.addItem(desc);
        }

        dataProvider = nodes;
    }

    protected function refresh () :void
    {
        for each (var node :Object in dataProvider) {
            dataDescriptor.getChildren(node).refresh();
        }
    }

    protected function handleChange (event :Event) :void
    {
        CommandEvent.dispatch(this, ITEM_SELECTED, getSelectedItem());
    }

    protected function handleItemOpening (event :TreeEvent) :void
    {
        if (event.opening) {
            loadCategory(event.item);

        } else if (dataProvider.length == 1) {
            // if there is only one type, prevent closeage
            event.preventDefault();
        }
    }

    protected function filterItems (thing :Object) :Boolean
    {
        // if the item isn't a thing we should always show it
        if (thing is Item) {
            var item :Item = Item(thing);
            if (!_showUsed && (item.used != Item.UNUSED)) {
                return false;
            }
        }

        return true;
    }

    protected function sortItems (a :Object, b:Object, fields :Array = null) :int
    {
        // for now, sort by itemId
        if ((a is Item) && (b is Item)) {
            var itemA :Item = Item(a);
            var itemB :Item = Item(b);
            if (itemA.itemId > itemB.itemId) {
                return -1;
            } else if (itemA.itemId < itemB.itemId) {
                return 1;
            } else {
                return 0;
            }

        } else if (a is Item) {
            return 1;

        } else if (b is Item) {
            return -1;

        } else {
            return 0;
        }
    }

    protected function loadCategory (node :Object) :void
    {
        var type :int = int(node.itemType);
        if (null != _loadedTypes[type]) {
            return;
        }

        _loadedTypes[type] = true;

        var svc :ItemService =
            (_ctx.getClient().requireService(ItemService) as ItemService);
        svc.getInventory(_ctx.getClient(), type,
            new ResultWrapper(
            function (cause :String) :void {
                // TODO: report status somewhere
                Log.getLog(this).warning("Error retrieving inventory: " +
                    cause);
                delete _loadedTypes[type];
            },
            function (items :Array) :void {
                // first, close the branch
                expandItem(node, false);

                // remove the "loading..." node and add the received children
                dataDescriptor.removeChildAt(node, null, 0);
                if (items.length == 0) {
                    dataDescriptor.addChildAt(
                        node, Msgs.ITEM.get("m.no_items"), 0);

                } else {
                    var count :int = 0;
                    for each (var item :Object in items) {
                        dataDescriptor.addChildAt(node, item, count++);
                    }
                }
                // TODO: come up with a way to have a "no items" label in a
                // filtered view where there are items but they're just not
                // displayed

                // try to invalidate the current list data and make it
                // re-render non-fuckedup
                dataDescriptor.getChildren(node).refresh();
                ListCollectionView(dataProvider).refresh();
                invalidateList();

                // re-open the branch with the new items
                expandItem(node, true, true);
            }));
    }

    protected var _ctx :MsoyContext;
    protected var _showUsed :Boolean;
    protected var _loadedTypes :Object = new Object();
}
}
