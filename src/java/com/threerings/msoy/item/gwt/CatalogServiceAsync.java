//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;
import java.util.Map;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.group.gwt.BrandDetail;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;

/**
 * Provides the asynchronous version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The async version of {@link CatalogService#loadJumble}.
     */
    void loadJumble (int themeId, int offset, int rows, AsyncCallback<CatalogService.CatalogResult> callback);

    /**
     * The async version of {@link CatalogService#loadCatalog}.
     */
    void loadCatalog (CatalogQuery query, int offset, int rows, AsyncCallback<CatalogService.CatalogResult> callback);

    /**
     * The async version of {@link CatalogService#purchaseItem}.
     */
    void purchaseItem (byte itemType, int catalogId, Currency currency, int authedCost, String memories, AsyncCallback<PurchaseResult<Item>> callback);

    /**
     * The async version of {@link CatalogService#listItem}.
     */
    void listItem (ItemIdent item, byte rating, int pricing, int salesTarget, Currency currency, int cost, int basisId, int brandId, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link CatalogService#loadListing}.
     */
    void loadListing (byte itemType, int catalogId, boolean forDisplay, AsyncCallback<CatalogListing> callback);

    /**
     * The async version of {@link CatalogService#loadAllDerivedItems}.
     */
    void loadAllDerivedItems (byte itemType, int catalogId, AsyncCallback<CatalogListing.DerivedItem[]> callback);

    /**
     * The async version of {@link CatalogService#updateListing}.
     */
    void updateListing (ItemIdent item, AsyncCallback<Void> callback);

    /**
     * The async version of {@link CatalogService#updatePricing}.
     */
    void updatePricing (byte itemType, int catalogId, int pricing, int salesTarget, Currency currency, int cost, int basisId, int brandId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link CatalogService#removeListing}.
     */
    void removeListing (byte itemType, int catalogId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link CatalogService#getPopularTags}.
     */
    void getPopularTags (byte type, int rows, AsyncCallback<Map<String, Integer>> callback);

    /**
     * The async version of {@link CatalogService#loadFavorites}.
     */
    void loadFavorites (int memberId, byte itemType, AsyncCallback<CatalogService.FavoritesResult> callback);

    /**
     * The async version of {@link CatalogService#loadPotentialBasisItems}.
     */
    void loadPotentialBasisItems (byte itemType, AsyncCallback<List<ListingCard>> callback);

    /**
     * The async version of {@link CatalogService#loadManagedBrands}.
     */
    void loadManagedBrands (AsyncCallback<List<BrandDetail>> callback);

    /**
     * The async version of {@link CatalogService#loadSuite}.
     */
    void loadSuite (byte itemType, int suiteId, AsyncCallback<CatalogService.SuiteResult> callback);
}
