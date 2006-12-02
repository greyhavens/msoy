package com.threerings.msoy.item.client {

import flash.events.Event;
import flash.events.EventDispatcher;

import mx.collections.ArrayCollection;

import com.threerings.util.Util;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.web.Item;

public class InventoryCollectionView extends ArrayCollection
{
    /**
     * Construct a new InventoryCollectionView.
     */
    public function InventoryCollectionView (
        ctx :MsoyContext, soleType :int = Item.NOT_A_TYPE,
        showUsed :Boolean = false)
    {
        _ctx = ctx;
        _showUsed = showUsed;
        this.filterFunction = filterCategories;

        var allTypes :Array = Item.getTypes();
        for each (var type :int in allTypes) {
            var showList :ArrayCollection = new ArrayCollection();
            _showItems[type] = showList;
            showList.addItem(Msgs.ITEM.get("m.retrieving"));
            addItem({
                label: Msgs.ITEM.get("t.items_" + Item.getTypeName(type)),
                itemType: type, children: showList
            });
        }

        if (soleType != Item.NOT_A_TYPE) {
            _show = [ soleType ];
            loadType(soleType);

        } else {
            _show = allTypes;
        }
    }

    public function typeOpened (thing :Object) :void
    {
        var type :int = int(thing.itemType);
        if (type != Item.NOT_A_TYPE) {
            loadType(type);
        }
    }

    public function loadType (type :int) :void
    {
        var rawList :Array = (_rawItems[type] as Array);
        if (rawList != null) {
            // we've already loaded, so nothing to do
            return;
        }

        // assign it, so that we at least know that the loading is in progress
        // place an empty array in the spot to "reserve" the fact that we've
        // begun loading that type
        _rawItems[type] = [];

        // otherwise, that category is not yet loaded
        var svc :ItemService =
            (_ctx.getClient().requireService(ItemService) as ItemService);
        svc.getInventory(_ctx.getClient(), type,
            new ResultWrapper(
            function (cause :String) :void {
                // TODO: report status?
                Log.getLog(this).warning("Error retrieving inventory: " +
                    cause);
                // forget about the fact that we tried to load this type
                _rawItems[type] = undefined;
            },
            function (items :Array) :void {
                itemsLoaded(type, items);
            }));
    }

    public function isShowUsed () :Boolean
    {
        return _showUsed;
    }

    public function setShowUsed (showUsed :Boolean) :void
    {
        if (_showUsed == showUsed) {
            return; // no change
        }

        _showUsed = showUsed;
        regenerateAllShown();
    }

    public function setShownTypes (types :Array) :void
    {
        if (Util.equals(types, _show)) {
            return; // no change
        }

        _show = types;
        regenerateAllShown();
        this.refresh(); // we've changed the filter
    }

    /**
     * Called when we've loaded the raw items for a particular item type.
     */
    protected function itemsLoaded (type :int, items :Array) :void
    {
        // sort the items according to itemId
        items.sort(sortItems);
        // assign the raw items in sorted order (may change)
        _rawItems[type] = items;

        // regenerate and display
        regenerateShown(type);
    }

    /**
     * Regenerate all the shown lists for all showing item categories.
     */
    protected function regenerateAllShown () :void
    {
        for each (var type :int in _show) {
            regenerateShown(type);
        }
    }

    /**
     * Completely regenerate the shown list for a particular item type.
     * Dispatch no events...
     */
    protected function regenerateShown (type :int) :void
    {
        var rawList :Array = (_rawItems[type] as Array);
        if (rawList == null) {
            return;
        }
        var showList :ArrayCollection = (_showItems[type] as ArrayCollection);

        showList.disableAutoUpdate();
        try {
            showList.removeAll();

            for each (var item :Item in rawList) {
                if (_showUsed || !item.isUsed()) {
                    showList.addItem(item);
                }
            }

            // if nothing was generated, be sure to add a "nuffink" label
            if (showList.length == 0) {
                showList.addItem(Msgs.ITEM.get((rawList.length > 0)
                    ? "m.no_filtered_items" : "m.no_items"));
            }

        } finally {
            showList.enableAutoUpdate();
        }
    }

    /**
     * Filter function for this top-level collection.
     */
    protected function filterCategories (thing :Object) :Boolean
    {
        var type :int = int(thing.itemType);
        return (-1 != _show.indexOf(type));
    }

    protected function sortItems (a :Item, b :Item, fields :Array = null) :int
    {
        // for now, we simply sort by itemId to give a semi-age order
        if (a.itemId > b.itemId) {
            return -1;
        } else if (a.itemId < b.itemId) {
            return 1;
        } else {
            return 0;
        }
    }

    /** The context, holy giver of life. */
    protected var _ctx :MsoyContext;

    /** Whether or not to show 'used' items. */
    protected var _showUsed :Boolean;

    /** Contains the categories we're currently showing. */
    protected var _show :Array;

    /** Contains an Array for each category, containing the raw items
     * for that category. */
    protected var _rawItems :Array = [];

    /** Contains an ArrayCollection for each category, containing the
     * Currently shown items for that category. */
    protected var _showItems :Array = [];
}
}
