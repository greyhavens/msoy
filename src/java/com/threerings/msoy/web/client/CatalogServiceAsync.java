//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.web.data.CatalogQuery;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The asynchronous version of {@link CatalogService#loadCatalog}.
     */
    public void loadCatalog (WebIdent ident, CatalogQuery query, int offset, int rows,
                             boolean includeCount, AsyncCallback callback);
    
    /**
     * The asynchronous version of {@link CatalogService#purchaseItem}
     */
    public void purchaseItem (WebIdent ident, byte itemType, int catalogId, AsyncCallback callback);
    
    /**
     * The asynchronous version of {@link CatalogService#listItem}
     */
    public void listItem (WebIdent ident, ItemIdent item, String descrip, int pricing,
                          int salesTarget, int flowCost, int goldCost, AsyncCallback callback);

    /**
     * The asynchronous version of {@link CatalogService#loadListing}
     */
    public void loadListing (byte itemType, int catalogId, boolean loadListedItem,
                             AsyncCallback callback);

    /**
     * The asynchronous version of {@link CatalogService#updateListing}
     */
    public void updateListing (WebIdent ident, ItemIdent item, String descrip,
                               AsyncCallback callback);

    /**
     * The asynchronous version of {@link CatalogService#updatePricing}
     */
    public void updatePricing (WebIdent ident, byte itemType, int catalogId, int pricing,
                               int salesTarget, int flowCost, int goldCost, AsyncCallback callback);

    /**
     * Removes the specified catalog listing.
     */
    public void removeListing (WebIdent ident, byte itemType, int catalogId,
                               AsyncCallback callback);

    /**
     * The asynchronous version of {@link CatalogService#returnItem}
     */
    public void returnItem (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link CatalogService#getPopularTags}.
     */
    public void getPopularTags (byte type, int count, AsyncCallback callback);
}
