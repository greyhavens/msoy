//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides digital items related services.
 */
public interface CatalogService extends RemoteService
{
    /** Provides results for {@link #loadCatalog}. */
    public static class CatalogResult implements IsSerializable
    {
        /** The total count of listings matching the query. */
        public int listingCount;

        /**
         * The particular set of listings requested.
         */
        public List<ListingCard> listings;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/catalogsvc";

    /**
     * Loads the featured items shown on the top-level catalog page.
     */
    public ShopData loadShopData ()
        throws ServiceException;

    /**
     * Loads all catalogue items of the specified type.
     *
     * @param includeCount if true, the count of all listings matching the query terms will also be
     * computed and included in the result.
     */
    public CatalogResult loadCatalog (CatalogQuery query, int offset, int rows, boolean includeCount)
        throws ServiceException;

    /**
     * Purchases the item of the specified id and type.
     */
    public Item purchaseItem (byte itemType, int catalogId, int authedFlowCost, int authedGoldCost)
        throws ServiceException;

    /**
     * Lists the specified item in the catalog.
     *
     * @return the catalog id of the newly listed item.
     */
    public int listItem (ItemIdent item, String descrip, int pricing, int salesTarget,
                         int flowCost, int goldCost)
        throws ServiceException;

    /**
     * Loads and returns the specified catalog listing.
     */
    public CatalogListing loadListing (byte itemType, int catalogId)
        throws ServiceException;

    /**
     * Updates the catalog listing associated with the supplied catalog original.
     */
    public void updateListing (ItemIdent item, String descrip)
        throws ServiceException;

    /**
     * Updates the specified catalog listing's price.
     */
    public void updatePricing (byte itemType, int catalogId, int pricing, int salesTarget,
                               int flowCost, int goldCost)
        throws ServiceException;

    /**
     * Removes the specified catalog listing.
     */
    public void removeListing (byte itemType, int catalogId)
        throws ServiceException;

    /**
     * Fetches the N most-used tags for a given item type.
     */
    public Map<String, Integer> getPopularTags (byte type, int rows)
        throws ServiceException;
}
