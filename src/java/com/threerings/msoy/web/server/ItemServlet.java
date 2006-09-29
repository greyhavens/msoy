//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.item.data.ItemIdent;
import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemGIdent;
import com.threerings.msoy.item.web.TagHistory;
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

        // validate the item
        if (!item.isConsistent()) {
            // TODO?
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }

        // TODO: validate anything else?

        // configure the item's creator and owner
        item.creatorId = creds.memberId;
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
        ItemEnum itype = ItemEnum.valueOf(type);
        if (type == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + creds + ", type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }

        // load their inventory via the item manager
        ServletWaiter<ArrayList<Item>> waiter =
            new ServletWaiter<ArrayList<Item>>(
                "loadInventory[" + creds.memberId + ", " + itype + "]");
        MsoyServer.itemMan.loadInventory(creds.memberId, itype, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Item loadItem (WebCreds creds, ItemGIdent item)
        throws ServiceException
    {
        ItemIdent ident = toIdent(creds, item, "loadItem");
        ServletWaiter<Item> waiter =
            new ServletWaiter<Item>("loadItem[" + item + "]");
        MsoyServer.itemMan.getItem(ident, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Item remixItem (WebCreds creds, ItemGIdent item)
        throws ServiceException
    {
        ItemIdent ident = toIdent(creds, item, "remixItem");
        ServletWaiter<Item> waiter =
            new ServletWaiter<Item>("remixItem[" + item + "]");
        MsoyServer.itemMan.remixItem(ident, waiter);
        return waiter.waitForResult();
    }
    
    // from interface ItemService    
    public byte getRating (WebCreds creds, ItemGIdent item, int memberId)
            throws ServiceException
    {
        ItemIdent ident = toIdent(creds, item, "getRating");
        ServletWaiter<Byte> waiter =
            new ServletWaiter<Byte>("rateItem[" + item + "]");
        MsoyServer.itemMan.getRating(ident, memberId, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Item rateItem (WebCreds creds, ItemGIdent item, byte rating)
            throws ServiceException
    {
        ItemIdent ident = toIdent(creds, item, "rateItem");
        ServletWaiter<Item> waiter =
            new ServletWaiter<Item>("rateItem[" + item + "]");
        MsoyServer.itemMan.rateItem(ident, creds.memberId, rating, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Collection<TagHistory> getTagHistory (
        WebCreds creds, ItemGIdent item)
            throws ServiceException
    {
        ItemIdent ident = toIdent(creds, item, "getTagHistory");
        ServletWaiter<Collection<TagHistory>> waiter =
            new ServletWaiter<Collection<TagHistory>>(
                "getTagHistory[" + item + "]");
        MsoyServer.itemMan.getTagHistory(ident, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public TagHistory tagItem (WebCreds creds, ItemGIdent item, String tag)
        throws ServiceException
    {
        ItemIdent ident = toIdent(creds, item, "tagItem");
        ServletWaiter<TagHistory> waiter =
            new ServletWaiter<TagHistory>("tagItem[" + item + "]");
        MsoyServer.itemMan.tagItem(ident, creds.memberId, tag, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public TagHistory untagItem (WebCreds creds, ItemGIdent item, String tag)
        throws ServiceException
    {
        ItemIdent ident = toIdent(creds, item, "untagItem");
        ServletWaiter<TagHistory> waiter =
            new ServletWaiter<TagHistory>("untagItem[" + item + "]");
        MsoyServer.itemMan.untagItem(ident, creds.memberId, tag, waiter);
        return waiter.waitForResult();
    }

    protected static ItemIdent toIdent (WebCreds creds, ItemGIdent item,
                                        String where)
        throws ServiceException
    {
        ItemEnum type = ItemEnum.valueOf(item.type);
        if (type == null) {
            log.warning("Rejecting invalid item type [where=" + where +
                        ", who=" + creds + ", type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
        return new ItemIdent(type, item.itemId);
    }
}
