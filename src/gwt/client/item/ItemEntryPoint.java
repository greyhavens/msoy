//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.client.CatalogServiceAsync;

import client.shell.Page;

/**
 * Configures {@link CItem} for item-derived pages.
 */
public abstract class ItemEntryPoint extends Page
{
    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CItem.itemsvc = (ItemServiceAsync)GWT.create(ItemService.class);
        ((ServiceDefTarget)CItem.itemsvc).setServiceEntryPoint("/itemsvc");
        CItem.catalogsvc = (CatalogServiceAsync)GWT.create(CatalogService.class);
        ((ServiceDefTarget)CItem.catalogsvc).setServiceEntryPoint("/catalogsvc");

        // load up our translation dictionaries
        CItem.imsgs = (ItemMessages)GWT.create(ItemMessages.class);
    }
}
