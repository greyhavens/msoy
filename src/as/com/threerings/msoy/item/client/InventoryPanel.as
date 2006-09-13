package com.threerings.msoy.item.client {

import mx.core.ClassFactory;

import mx.collections.ArrayCollection;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.List;

import com.threerings.msoy.item.web.Item;

public class InventoryPanel extends FloatingPanel
{
    public function InventoryPanel (ctx :MsoyContext)
    {
        super(ctx, ctx.xlate("item", "t.inventory"));
        showCloseButton = true;

        open();

        // shoot off a request to get our inventory
        var svc :ItemService =
            (_ctx.getClient().requireService(ItemService) as ItemService);

        svc.getInventory(_ctx.getClient(), new ResultWrapper(
            function (cause :String) :void {
                // place failure cause in the status
                status = _ctx.xlate(null, cause);
            }, inventoryRetrieved));
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _list = new List(_ctx);
        _list.dragEnabled = true;
        _list.maxHeight = 400;
        _list.minWidth = 300;
        _list.itemRenderer = new ClassFactory(ItemRenderer);
        _list.dataProvider = _itemsToShow;
        addChild(_list);
    }

    /**
     * Called to process the results of our service request for items.
     */
    protected function inventoryRetrieved (items :Array) :void
    {
        for each (var item :Item in items) {
            _itemsToShow.addItem(item);
        }
    }

    protected var _itemsToShow :ArrayCollection = new ArrayCollection();

    protected var _list :List;
}
}
