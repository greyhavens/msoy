package com.threerings.msoy.item.client {

import mx.core.ClassFactory;

import mx.collections.ArrayCollection;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.List;

import com.threerings.msoy.item.web.Item;

/**
 * A basic list that can show items in-world.
 */
public class ItemList extends List
{
    public function ItemList (ctx :MsoyContext)
    {
        super(ctx);

        maxHeight = 400;
        minWidth = 300;
        itemRenderer = new ClassFactory(ItemRenderer);
        dataProvider = _itemsToShow;
    }

    /**
     * Clear all currently shown items.
     */
    public function clearItems () :void
    {
        _itemsToShow.removeAll();
    }

    /**
     * Add the specified items to the end of the list.
     */
    public function addItems (items :Array) :void
    {
        for each (var item :Item in items) {
            _itemsToShow.addItem(item);
        }
    }

    /** The underlying collection being used by the list. */
    protected var _itemsToShow :ArrayCollection = new ArrayCollection();
}
}
