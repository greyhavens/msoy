//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.gwt.BrandDetail;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogListing.DerivedItem;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;

/**
 * Provides digital items related services.
 */
@RemoteServiceRelativePath(CatalogService.REL_PATH)
public interface CatalogService extends RemoteService
{
    /** Provides results for {@link #loadCatalog}. */
    public static class CatalogResult
        implements IsSerializable
    {
        /** The particular set of listings requested. */
        public List<ListingCard> listings;

        /** The theme, if any, constraining the returned listings. */
        public GroupName theme;

        public CatalogResult (List<ListingCard> items, GroupName theme)
        {
            this.listings = items;
            this.theme = theme;
        }

        public CatalogResult ()
        {
        }
    }

    /** Provides results for {@link #loadFavorites}. */
    public static class FavoritesResult
        implements IsSerializable
    {
        /** The member for whom we're returning favorites. */
        public MemberName noter;

        /** The listing information for said favorites. */
        public List<ListingCard> favorites;
    }

    /** Returned by {@link #loadSuite}. */
    public static class SuiteResult
        implements IsSerializable
    {
        /** The name of the suite. For game suites, this is the game's name. */
        public String name;

        /** The id of the suite in question. */
        public int suiteId;

        /** The id of the creator of the items in this suite. */
        public int creatorId;

        /** The tag that identifies non-sub-items in this suite. */
        public String suiteTag;

        /** The listings of items in this suite. */
        public List<ListingCard> listings;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/catalogsvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + CatalogService.ENTRY_POINT;

    /**
     * Loads the featured items shown on the top-level catalog page, possibly constrained
     * to them given theme.
     */
    CatalogResult loadJumble (int themeId, int offset, int rows)
        throws ServiceException;

    /**
     * Loads all catalogue items of the specified type.
     */
    CatalogResult loadCatalog (CatalogQuery query, int offset, int rows)
        throws ServiceException;

    /**
     * Purchases the item of the specified id and type.
     */
    PurchaseResult<Item> purchaseItem (
        MsoyItemType itemType, int catalogId, Currency currency, int authedCost, String memories)
        throws ServiceException;

    /**
     * Lists the specified item in the catalog.
     *
     * @return the catalog id of the newly listed item.
     */
    int listItem (ItemIdent item, byte rating, int pricing, int salesTarget,
                  Currency currency, int cost, int basisId, int brandId)
        throws ServiceException;

    /**
     * Loads and returns the specified catalog listing. If <code>forDisplay</code> is set, some
     * additional listing fields are filled in that are only needed for display.
     */
    CatalogListing loadListing (MsoyItemType itemType, int catalogId, boolean forDisplay)
        throws ServiceException;

    /**
     * Loads all derived items for a given item. The user interface shows a short list when the
     * listing is loaded, then this may be called at the user's request.
     */
    DerivedItem[] loadAllDerivedItems (MsoyItemType itemType, int catalogId)
        throws ServiceException;

    /**
     * Updates the catalog listing associated with the supplied catalog original.
     */
    void updateListing (ItemIdent item)
        throws ServiceException;

    /**
     * Updates the specified catalog listing's price.
     */
    void updatePricing (MsoyItemType itemType, int catalogId, int pricing, int salesTarget,
                        Currency currency, int cost, int basisId, int brandId)
        throws ServiceException;

    /**
     * Removes the specified catalog listing.
     */
    void removeListing (MsoyItemType itemType, int catalogId)
        throws ServiceException;

    /**
     * Fetches the N most-used tags for a given item type.
     */
    Map<String, Integer> getPopularTags (MsoyItemType type, int rows)
        throws ServiceException;

    /**
     * Loads up the favorite items of the specified member of the specified type.
     */
    FavoritesResult loadFavorites (int memberId, MsoyItemType itemType)
        throws ServiceException;

    /**
     * Using the authenticated member's favorites, returns the subset that are suitable for use as
     * a basis for the given item type. It's not possible for this method to filter based on the
     * cost of the items, the caller should use {@link CatalogListing#getMinimumDerivedCost}.
     */
    List<ListingCard> loadPotentialBasisItems (MsoyItemType itemType)
        throws ServiceException;

    /**
     * Load the groups where the current player has a non-zero amount of brand shares and return
     * the details for each such brand.
     */
    List<BrandDetail> loadManagedBrands ()
        throws ServiceException;

    /**
     * Loads the specified suite.
     */
    SuiteResult loadSuite (MsoyItemType itemType, int suiteId)
        throws ServiceException;
}
