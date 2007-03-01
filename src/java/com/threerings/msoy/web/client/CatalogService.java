//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides digital items related services.
 */
public interface CatalogService extends RemoteService
{
    /**
     * Loads all catalogue items of the specified type. If memberId == 0, it's a guest request.
     */
    public List loadCatalog (int memberId, byte type, byte sortBy, String search, String tag,
                             int offset, int rows)
        throws ServiceException;

    /**
     * Purchases the item of the specified id and type.
     */
    public Item purchaseItem (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Lists or delists the specified item in the catalog.
     */
    public CatalogListing listItem (WebCreds creds, ItemIdent item, boolean list)
        throws ServiceException;

    /**
     * Executes an item return, potentially for a (potentially partial) refund.
     * Returns a two-element array containing { flow refunded, gold refunded }.
     */
    public int[] returnItem (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the N most-used tags for a given item type.
     */
    public Map getPopularTags (byte type, int rows)
        throws ServiceException;
}
