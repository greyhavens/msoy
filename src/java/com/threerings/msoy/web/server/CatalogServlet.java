//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;
import java.util.Map;

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
 * Provides the server implementation of {@link CatalogService}.
 */
public class CatalogServlet extends RemoteServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public List loadCatalog (WebCreds creds, final byte type, final byte sortBy,
                             final String search, final int offset, final int rows)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load catalog for invalid item type " +
                        "[who=" + creds + ", type=" + type + "].");
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // load their catalog via the catalog manager
        final ServletWaiter<List<CatalogListing>> waiter = new ServletWaiter<List<CatalogListing>>(
                "loadCatalog[" + type + ", " + sortBy + ", " + offset + ", " + rows + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.loadCatalog(type, sortBy, search, offset, rows, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface CatalogService
    public Item purchaseItem (final WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        // TODO: validate this user's creds
        final ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "purchaseItem[" + creds.memberId + ", " + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.purchaseItem(creds.memberId, ident, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface CatalogService
    public CatalogListing listItem (WebCreds creds, final ItemIdent ident, final boolean list)
        throws ServiceException
    {
        // TODO: validate this user's creds
        final ServletWaiter<CatalogListing> waiter = new ServletWaiter<CatalogListing>(
            "listItem[" + ident + ", " + list + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.listItem(ident, list, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface CatalogService
    public Map<String, Integer> getPopularTags (WebCreds creds, final byte type, final int rows)
        throws ServiceException
    {
        final ServletWaiter<Map<String, Integer>> waiter = new ServletWaiter<Map<String, Integer>>(
            "getPopularTags[" + type + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getPopularTags(type, rows, waiter);
            }
        });
        return waiter.waitForResult();
    }
}
