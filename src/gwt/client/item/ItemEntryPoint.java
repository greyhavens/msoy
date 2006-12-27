//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.client.CatalogServiceAsync;

import client.shell.MsoyEntryPoint;

/**
 * Configures our {@link ItemContext} for item-derived pages.
 */
public abstract class ItemEntryPoint extends MsoyEntryPoint
{
    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();
        ItemContext ictx = (ItemContext)_gctx;

        // wire up our remote services
        ictx.itemsvc = (ItemServiceAsync)GWT.create(ItemService.class);
        ((ServiceDefTarget)ictx.itemsvc).setServiceEntryPoint("/itemsvc");
        ictx.catalogsvc = (CatalogServiceAsync)GWT.create(CatalogService.class);
        ((ServiceDefTarget)ictx.catalogsvc).setServiceEntryPoint("/catalogsvc");

        // load up our translation dictionaries
        ictx.imsgs = (ItemMessages)GWT.create(ItemMessages.class);
    }
}
