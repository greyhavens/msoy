//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.data.all.Currency;

/**
 * The asynchronous (client-side) version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The asynchronous version of {@link CatalogService#loadShopData}.
     */
    void loadShopData (AsyncCallback<ShopData> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadCatalog}.
     */
    public void loadCatalog (CatalogQuery query, int offset, int rows, boolean includeCount,
                             AsyncCallback<CatalogService.CatalogResult> callback);

    /**
     * The asynchronous version of {@link CatalogService#purchaseItem}
     */
    public void purchaseItem (byte itemType, int catalogId, int authedFlowCost, int authedGoldCost,
                              AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link CatalogService#purchaseGameContent}
     */
    public void purchaseGameContent (int gameId, byte itemType, int catalogId, int authedFlowCost,
                                     int authedBarsCost, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link CatalogService#listItem}
     */
    public void listItem (ItemIdent item, String descrip, int pricing, int salesTarget,
                          Currency currency, int cost, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadListing}
     */
    public void loadListing (byte itemType, int catalogId, AsyncCallback<CatalogListing> callback);

    /**
     * The asynchronous version of {@link CatalogService#updateListing}
     */
    public void updateListing (ItemIdent item, String descrip, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link CatalogService#updatePricing}
     */
    public void updatePricing (byte itemType, int catalogId, int pricing, int salesTarget,
                               Currency currency, int cost, AsyncCallback<Void> callback);

    /**
     * Removes the specified catalog listing.
     */
    public void removeListing (byte itemType, int catalogId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link CatalogService#getPopularTags}.
     */
    void getPopularTags (byte type, int count, AsyncCallback<Map<String, Integer>> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadFavorites}.
     */
    void loadFavorites (int memberId, byte itemType,
                        AsyncCallback<CatalogService.FavoritesResult> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadGameSuiteInfo}.
     */
    void loadGameSuiteInfo (int gameId, AsyncCallback<CatalogService.SuiteInfo> callback);
}
