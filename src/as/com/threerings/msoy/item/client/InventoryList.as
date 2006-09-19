package com.threerings.msoy.item.client {

import flash.display.DisplayObjectContainer;

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

        _type = type;
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null && !_retrieved) {
            trace("isVis(" + _type + "): " + visible);
            var svc :ItemService =
                (_ctx.getClient().requireService(ItemService) as ItemService);
            svc.getInventory(_ctx.getClient(), _type, new ResultWrapper(
                function (cause :String) :void {
                    // report status somewhere?
                    Log.getLog(this).warning("Error retrieving inventory: " +
                        cause);
                }, addItems));
            _retrieved = true;
        }
    }

    /** The type of item we're showing in this particular list. */
    protected var _type :String;
    protected var _retrieved :Boolean;
}
}
