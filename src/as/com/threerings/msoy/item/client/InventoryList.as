package com.threerings.msoy.item.client {

import flash.display.DisplayObjectContainer;

import mx.events.FlexEvent;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.MsoyContext;

/**
 * Lists one particular type of item from a user's inventory.
 * Allows dragging to the room for editing.
 */
public class InventoryList extends ItemList
{
    public function InventoryList (ctx :MsoyContext, type :String = "FURNITURE")
    {
        super(ctx);

        dragEnabled = true;

        var svc :ItemService =
            (_ctx.getClient().requireService(ItemService) as ItemService);
        svc.getInventory(_ctx.getClient(), type, new ResultWrapper(
            function (cause :String) :void {
                // report status somewhere?
                Log.getLog(this).warning("Error retrieving inventory: " +
                    cause);
            }, function (items :Array) :void {
                clearItems();
                if (items.length == 0) {
                    _itemsToShow.addItem(ctx.xlate("item", "m.no_items"));

                } else {
                    addItems(items);
                }
            }));

        // add a status item
        _itemsToShow.addItem(ctx.xlate("item", "m.retrieving"));
    }
}
}
