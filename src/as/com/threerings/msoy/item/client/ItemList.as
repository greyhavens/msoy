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
    public function ItemList (
        ctx :MsoyContext, rendererClass :Class = null)
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

    /** The underlying collection being used by the list. */
    protected var _itemsToShow :ArrayCollection = new ArrayCollection();
}
}
