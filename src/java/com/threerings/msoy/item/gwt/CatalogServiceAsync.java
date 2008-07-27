//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The asynchronous version of {@link CatalogService#loadShopData}.
     */
    void loadShopData (WebIdent ident, AsyncCallback<ShopData> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadCatalog}.
     */
    public void loadCatalog (WebIdent ident, CatalogQuery query, int offset, int rows,
                             boolean includeCount,
                             AsyncCallback<CatalogService.CatalogResult> callback);

    /**
     * The asynchronous version of {@link CatalogService#purchaseItem}
     */
    public void purchaseItem (
        WebIdent ident, byte itemType, int catalogId, int authedFlowCost, int authedGoldCost,
        AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link CatalogService#listItem}
     */
    public void listItem (WebIdent ident, ItemIdent item, String descrip, int pricing,
                          int salesTarget, int flowCost, int goldCost,
                          AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadListing}
     */
    public void loadListing (WebIdent ident, byte itemType, int catalogId,
                             AsyncCallback<CatalogListing> callback);

    /**
     * The asynchronous version of {@link CatalogService#updateListing}
     */
    public void updateListing (WebIdent ident, ItemIdent item, String descrip,
                               AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link CatalogService#updatePricing}
     */
    public void updatePricing (WebIdent ident, byte itemType, int catalogId, int pricing,
                               int salesTarget, int flowCost, int goldCost,
                               AsyncCallback<Void> callback);

    /**
     * Removes the specified catalog listing.
     */
    public void removeListing (WebIdent ident, byte itemType, int catalogId,
                               AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link CatalogService#returnItem}
     */
    void returnItem (WebIdent ident, ItemIdent item, AsyncCallback<int[]> callback);

    /**
     * The asynchronous version of {@link CatalogService#getPopularTags}.
     */
    void getPopularTags (byte type, int count, AsyncCallback<Map<String, Integer>> callback);
}
