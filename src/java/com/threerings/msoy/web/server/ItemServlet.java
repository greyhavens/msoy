//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.TagHistory;
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

    // from interface ItemService
    public Item remixItem (WebCreds creds, int itemId, String type)
        throws ServiceException
    {
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to remix item of invalid item type " +
                "[who=" + creds + ", itemId=" + itemId +
                "type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
        ServletWaiter<Item> waiter = new ServletWaiter<Item>(
                "remixItem[" + itemId + ", " + etype + "]");
        MsoyServer.itemMan.remixItem(itemId, etype, waiter);
        return waiter.waitForResult();
    }
    
    // from interface ItemService    
    public byte getRating (
        WebCreds creds, int itemId, String type, int memberId)
            throws ServiceException
    {
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to rate item of invalid item type " +
                "[who=" + creds + ", itemId=" + itemId +
                "type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
        ServletWaiter<Byte> waiter = new ServletWaiter<Byte>(
                "rateItem[" + itemId + ", " + etype + "]");
        MsoyServer.itemMan.getRating(itemId, etype, memberId, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Item rateItem (WebCreds creds, int itemId, String type, byte rating)
            throws ServiceException
    {
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to rate item of invalid item type " +
                "[who=" + creds + ", itemId=" + itemId +
                "type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
        ServletWaiter<Item> waiter = new ServletWaiter<Item>(
                "rateItem[" + itemId + ", " + etype + "]");
        MsoyServer.itemMan.rateItem(
            itemId, etype, creds.memberId, rating, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Collection<TagHistory> getTagHistory (
        WebCreds creds, int itemId, String type)
            throws ServiceException
    {
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to get tag history of invalid item type " +
                "[who=" + creds + ", itemId=" + itemId +
                "type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
        ServletWaiter<Collection<TagHistory>> waiter =
            new ServletWaiter<Collection<TagHistory>>(
                "getTagHistory[" + itemId + ", " + etype + "]");
        MsoyServer.itemMan.getTagHistory(itemId, etype, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public TagHistory tagItem (
        WebCreds creds, int itemId, String type, String tag)
            throws ServiceException
    {
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to tag item of invalid item type " +
                "[who=" + creds + ", itemId=" + itemId +
                "type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
        ServletWaiter<TagHistory> waiter = new ServletWaiter<TagHistory>(
                "tagItem[" + itemId + ", " + etype + "]");
        MsoyServer.itemMan.tagItem(
            itemId, etype, creds.memberId, tag, waiter);
        return waiter.waitForResult();
    }

    // from interface ItemService
    public TagHistory untagItem (
        WebCreds creds, int itemId, String type, String tag)
            throws ServiceException
    {
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to untag item of invalid item type " +
                "[who=" + creds + ", itemId=" + itemId +
                "type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
        ServletWaiter<TagHistory> waiter = new ServletWaiter<TagHistory>(
                "untagItem[" + itemId + ", " + etype + "]");
        MsoyServer.itemMan.untagItem(
            itemId, etype, creds.memberId, tag, waiter);
        return waiter.waitForResult();
    }
}
