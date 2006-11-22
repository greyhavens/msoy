//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class CatalogServlet extends RemoteServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public List loadCatalog (WebCreds creds, byte type)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load catalog for invalid item type " +
                        "[who=" + creds + ", type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }

        // load their catalog via the catalog manager
        ServletWaiter<List<CatalogListing>> waiter =
            new ServletWaiter<List<CatalogListing>>("loadCatalog[" + type + "]");
        MsoyServer.itemMan.loadCatalog(type, waiter);
        return waiter.waitForResult();
    }

    // from interface CatalogService
    public Item purchaseItem (WebCreds creds, ItemIdent item)
        throws ServiceException
    {
        // TODO: validate this user's creds

        ItemIdent ident = ItemServlet.toIdent(creds, item, "purchaseItem");
        ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "purchaseItem[" + creds.memberId + ", " + item + "]");
        MsoyServer.itemMan.purchaseItem(creds.memberId, ident, waiter);
        return waiter.waitForResult();
    }

    // from interface CatalogService
    public CatalogListing listItem (WebCreds creds, ItemIdent item)
        throws ServiceException
    {
        // TODO: validate this user's creds

        ItemIdent ident = ItemServlet.toIdent(creds, item, "listItem");
        ServletWaiter<CatalogListing> waiter =
            new ServletWaiter<CatalogListing>("listItem[" + item + "]");
        MsoyServer.itemMan.listItem(ident, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Map<String, Integer> getPopularTags (WebCreds creds, byte type, int rows)
        throws ServiceException
    {
        ServletWaiter<Map<String, Integer>> waiter =
            new ServletWaiter<Map<String, Integer>>("getPopularTags[" + type + "]");
        MsoyServer.itemMan.getPopularTags(type, rows, waiter);
        return waiter.waitForResult();
    }
}
