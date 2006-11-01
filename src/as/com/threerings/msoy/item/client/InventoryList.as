package com.threerings.msoy.item.client {

import flash.display.DisplayObjectContainer;

import flash.events.DataEvent;

import mx.events.FlexEvent;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.web.Item;

/**
 * Lists one particular type of item from a user's inventory.
 * Allows dragging to the room for editing.
 *
 * Dispatches a DataEvent when data is loaded.
 */
public class InventoryList extends ItemList
{
    public function InventoryList (
        ctx :MsoyContext, type :int,
        showUsed :Boolean = false, showUnused :Boolean = true)
    {
        super(ctx);

        dragEnabled = true;

        var svc :ItemService =
            (_ctx.getClient().requireService(ItemService) as ItemService);
        svc.getInventory(_ctx.getClient(), type,
            new ResultWrapper(
            function (cause :String) :void {
                // report status somewhere?
                Log.getLog(this).warning("Error retrieving inventory: " +
                    cause);
            }, function (items :Array) :void {
                clearItems(); // clear the status

                // possibly filter unwanted items
                if (!showUsed || !showUnused) {
                    items = items.filter(
                        function (elem :*, index :int, arr :Array) :Boolean {
                            return (Item(elem).used == Item.UNUSED) ?
                                showUnused : showUsed;
                        });
                }
                if (items.length == 0) {
                    _itemsToShow.addItem(Msgs.ITEM.get("m.no_items"));

                } else {
                    addItems(items);
                }
                dispatchEvent(new DataEvent(DataEvent.DATA));
            }));

        // add a status item
        _itemsToShow.addItem(Msgs.ITEM.get("m.retrieving"));
    }
}
}
