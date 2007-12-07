//
// $Id$

package com.threerings.msoy.item.client {

import mx.core.ClassFactory;

import mx.collections.ArrayCollection;
import mx.collections.Sort;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.ui.MsoyList;
import com.threerings.msoy.world.client.WorldContext;

/**
 * A basic list that can show items in-world.
 */
public class ItemList extends MsoyList
{
    public function ItemList (ctx :WorldContext, rendererClass :Class = null)
    {
        super(ctx);

        maxHeight = 400;
        minWidth = 300;
        if (rendererClass == null) {
            rendererClass = ItemRenderer;
        }
        itemRenderer = new ClassFactory(rendererClass);
        dataProvider = _itemsToShow;
    }

    public function removeItem (item :Object) :void
    {
        var dex :int = _itemsToShow.getItemIndex(item);
        if (dex != -1) {
            _itemsToShow.removeItemAt(dex);
        }
    }

    /**
     * Clear all currently shown items.
     */
    public function clearItems () :void
    {
        _itemsToShow.removeAll();
    }

    /**
     * Add the specified item to the end of the list.
     */
    public function addItem (item :Object) :void
    {
        _itemsToShow.addItem(item);
    }

    /**
     * Add the specified items to the end of the list.
     */
    public function addItems (items :Array) :void
    {
        for each (var item :Object in items) {
            _itemsToShow.addItem(item);
        }
    }

    /**
     * Set the sort that should be used on these items.
     */
    public function setSort (sort :Sort) :void
    {
        _itemsToShow.sort = sort;
        refresh();
    }

    public function refresh () :void
    {
        _itemsToShow.refresh();
        var si :int = selectedIndex;
        // again, this should scroll things on-screen, but it doesn't always
        if (si != -1) {
            scrollToIndex(si);
        }
    }

    /** The underlying collection being used by the list. */
    protected var _itemsToShow :ArrayCollection = new ArrayCollection();
}
}
