//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.web.ItemGIdent;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The asynchronous version of {@link CatalogService#loadCatalog}.
     */
    public void loadCatalog (WebCreds creds, byte type,
                             AsyncCallback callback);
    
    /**
     *  The asynchronous version of {@link CatalogService#purchaseItem}
     */
    public void purchaseItem (WebCreds creds, ItemGIdent item,
                              AsyncCallback callback);
    
    /**
     *  The asynchronous version of {@link CatalogService#listItem}
     */
    public void listItem (WebCreds creds, ItemGIdent item,
                          AsyncCallback callback);
}
