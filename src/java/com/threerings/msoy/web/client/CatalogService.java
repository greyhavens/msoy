//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    /** Loads all catalogue items of the specified type. */
    public List loadCatalog (WebCreds creds, byte type)
        throws ServiceException;
    
    /** Purchases the item of the specified id and type. */
    public Item purchaseItem (WebCreds creds, ItemIdent item)
        throws ServiceException;
    
    /** Lists the specified item in the catalog. */
    public CatalogListing listItem (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the N most-used tags for a given item type.
     */
    public HashMap getPopularTags(WebCreds creds, byte type, int rows)
        throws ServiceException;
} 
