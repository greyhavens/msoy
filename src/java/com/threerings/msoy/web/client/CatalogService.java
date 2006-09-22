//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides digital items related services.
 */
public interface CatalogService extends RemoteService
{
    /** Loads all catalogue items of the specified type. */
    public ArrayList loadCatalog (WebCreds creds, String type)
        throws ServiceException;
    
    /** Purchases the item of the specified id and type. */
    public Item purchaseItem (WebCreds creds, int itemId, String type)
        throws ServiceException;
    
    /** Lists the specified item in the catalog. */
    public CatalogListing listItem (WebCreds creds, int itemId, String type)
        throws ServiceException;
} 
