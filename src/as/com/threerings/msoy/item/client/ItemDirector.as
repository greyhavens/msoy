//
// $Id$

package com.threerings.msoy.item.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationAdapter;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.Item;

public class ItemDirector extends BasicDirector
{
    public function ItemDirector (ctx :WorldContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    /**
     * Initiate the load of inventory for the selected item type.
     */
    public function loadInventory (type :int) :void
    {
        if (_mctx.getClientObject().isInventoryLoaded(type) ||
                _loading[type]) {
            return;
        }

        try {
            var f :Function = function (cause :String) :void {
                loadInventoryFailed(type, cause);
            };

            var svc :ItemService =
                (_mctx.getClient().requireService(ItemService) as ItemService);
            svc.getInventory(_ctx.getClient(), type, new InvocationAdapter(f));

        } finally {
            _loading[type] = true;
        }
    }

    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);

        // erase all our memory for what's loaded
        _loading = new Object();
    }

    /**
     * Callback when the loadInventory fails for some reason.
     */
    protected function loadInventoryFailed (type :int, cause :String) :void
    {
        // mark that we've not loaded that type
        delete _loading[type];

        Log.getLog(this).warning("Inventory load failure [cause=" + cause +
            "].");
    }

    /** Our context, casted. */
    protected var _mctx :WorldContext;

    /** An associative hash containing the item types that are currently
     * being loaded. */
    protected var _loading :Object = new Object();
}
}
