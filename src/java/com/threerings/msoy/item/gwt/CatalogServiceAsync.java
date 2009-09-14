//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.group.gwt.BrandDetail;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.CatalogListing.DerivedItem;

/**
 * The asynchronous (client-side) version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The asynchronous version of {@link CatalogService#loadShopData}.
     */
    void loadJumble (int offset, int rows, AsyncCallback<List<ListingCard>> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadCatalog}.
     */
    void loadCatalog (CatalogQuery query, int offset, int rows,
                      AsyncCallback<CatalogService.CatalogResult> callback);

    /**
     * The asynchronous version of {@link CatalogService#purchaseItem}
     */
    void purchaseItem (byte itemType, int catalogId, Currency currency, int authedCost,
                       String memories, AsyncCallback<PurchaseResult<Item>> callback);

    /**
     * The asynchronous version of {@link CatalogService#listItem}
     */
    void listItem (ItemIdent item, byte rating, int pricing, int salesTarget,
                   Currency currency, int cost, int basisId, int brandId,
                   AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadListing}
     */
    void loadListing (byte itemType, int catalogId, boolean forDisplay,
                      AsyncCallback<CatalogListing> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadAllDerivedItems}
     */
    void loadAllDerivedItems (byte itemType, int catalogId, AsyncCallback<DerivedItem[]> callback);

    /**
     * The asynchronous version of {@link CatalogService#updateListing}
     */
    void updateListing (ItemIdent item, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link CatalogService#updatePricing}
     */
    void updatePricing (byte itemType, int catalogId, int pricing, int salesTarget,
                        Currency currency, int cost, int basisId, int brandId,
                        AsyncCallback<Void> callback);

    /**
     * Removes the specified catalog listing.
     */
    void removeListing (byte itemType, int catalogId, AsyncCallback<Void> callback);

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
     * The asynchronous version of {@link CatalogService#loadPotentialBasisItems}.
     */
    void loadPotentialBasisItems (byte itemType, AsyncCallback<List<ListingCard>> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadBrands}.
     */
    void loadBrands (AsyncCallback<List<BrandDetail>> callback);

    /**
     * The asynchronous version of {@link CatalogService#loadSuite}.
     */
    void loadSuite (byte itemType, int catalogId,
                    AsyncCallback<CatalogService.SuiteResult> callback);
}
