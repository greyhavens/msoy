//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The asynchronous version of {@link CatalogService#loadCatalog}.
     */
    public void loadCatalog (int memberId, byte type, byte sortBy, String search, String tag,
                             int offset, int rows, AsyncCallback callback);
    
    /**
     *  The asynchronous version of {@link CatalogService#purchaseItem}
     */
    public void purchaseItem (WebCreds creds, ItemIdent item, AsyncCallback callback);
    
    /**
     *  The asynchronous version of {@link CatalogService#listItem}
     */
    public void listItem (WebCreds creds, ItemIdent item, int rarity, boolean list,
                          AsyncCallback callback);

    /**
     *  The asynchronous version of {@link CatalogService#returnItem}
     */
    public void returnItem (WebCreds creds, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link CatalogService#getPopularTags}.
     */
    public void getPopularTags (byte type, int count, AsyncCallback callback);
}
