//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.logging.Level;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class ItemServlet extends RemoteServiceServlet
    implements ItemService
{
    // from interface ItemService
    public int createItem (WebCreds creds, Item item)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // TODO: validate anything else?

        // configure the item's owner
        item.ownerId = creds.memberId;

        // pass the buck to the item manager to do the dirty work
        ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "insertItem[" + creds + ", " + item + "]");
        MsoyServer.itemMan.insertItem(item, waiter);
        return waiter.waitForResult().itemId;
    }

    // from interface ItemService
    public ArrayList loadInventory (WebCreds creds, String type)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // convert the string they supplied to an item enumeration
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + creds + ", type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }

        // load their inventory via the item manager
        ServletWaiter<ArrayList<Item>> waiter =
            new ServletWaiter<ArrayList<Item>>(
                "loadInventory[" + creds.memberId + ", " + etype + "]");
        MsoyServer.itemMan.loadInventory(creds.memberId, etype, waiter);
        return waiter.waitForResult();
    }
}
