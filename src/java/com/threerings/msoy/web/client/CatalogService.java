//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

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
         *
         * @gwt.typeArgs <com.threerings.msoy.item.data.gwt.CatalogListing>
         */
        public List listings;
    }

    /**
     * Loads all catalogue items of the specified type. If memberId == 0, it's a guest request.
     *
     * @param includeCount if true, the count of all listings matching the query terms will also be
     * computed and included in the result.
     */
    public CatalogResult loadCatalog (int memberId, byte type, byte sortBy, String search,
                                      String tag, int creator, int offset, int rows,
                                      boolean includeCount)
        throws ServiceException;

    /**
     * Purchases the item of the specified id and type.
     */
    public Item purchaseItem (WebIdent ident, byte itemType, int catalogId)
        throws ServiceException;

    /**
     * Lists the specified item in the catalog.
     */
    public CatalogListing listItem (WebIdent ident, ItemIdent item, String descrip, int rarity)
        throws ServiceException;

    /**
     * Removes the specified catalog listing.
     */
    public void removeListing (WebIdent ident, byte itemType, int catalogId)
        throws ServiceException;

    /**
     * Executes an item return, potentially for a (potentially partial) refund.
     * Returns a two-element array containing { flow refunded, gold refunded }.
     */
    public int[] returnItem (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the N most-used tags for a given item type.
     *
     * @gwt.typeArgs <java.lang.String, java.lang.Integer>
     */
    public Map getPopularTags (byte type, int rows)
        throws ServiceException;
}
