//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collection;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.TagHistory;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class ItemServlet extends MsoyServiceServlet
    implements ItemService
{
    // from interface ItemService
    public int createItem (WebCreds creds, final Item item)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // validate the item
        if (!item.isConsistent()) {
            // TODO?
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // TODO: validate anything else?

        // configure the item's creator and owner
        item.creatorId = creds.memberId;
        item.ownerId = creds.memberId;

        // pass the buck to the item manager to do the dirty work
        final ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "insertItem[" + creds + ", " + item + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.insertItem(item, waiter);
            }
        });
        return waiter.waitForResult().itemId;
    }

    // from interface ItemService
    public void updateItem (WebCreds creds, final Item item)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // validate the item
        if (!item.isConsistent()) {
            // TODO?
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // TODO: validate anything else?

        // pass the buck to the item manager to do the dirty work
        final ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "updateItem[" + creds + ", " + item + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.updateItem(item, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from interface ItemService
    public Item loadItem (WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final ServletWaiter<Item> waiter = new ServletWaiter<Item>("loadItem[" + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getItem(ident, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public ItemDetail loadItemDetail (WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final ServletWaiter<ItemDetail> waiter = new ServletWaiter<ItemDetail>(
            "loadItem[" + ident + "]");
        final int memberId = (creds == null) ? -1 : creds.memberId;
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getItemDetail(ident, memberId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Item remixItem (WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final ServletWaiter<Item> waiter = new ServletWaiter<Item>("remixItem[" + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.remixItem(ident, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public void deleteItem (final WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>("deleteItem[" + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.deleteItemFor(creds.memberId, ident, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from interface ItemService
    public byte getRating (WebCreds creds, final ItemIdent ident, final int memberId)
        throws ServiceException
    {
        final ServletWaiter<Byte> waiter = new ServletWaiter<Byte>("getRating[" + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getRating(ident, memberId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public float rateItem (final WebCreds creds, final ItemIdent ident, final byte rating)
        throws ServiceException
    {
        final ServletWaiter<Float> waiter = new ServletWaiter<Float>("rateItem[" + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.rateItem(ident, creds.memberId, rating, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Collection<String> getTags (WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final ServletWaiter<Collection<String>> waiter =
            new ServletWaiter<Collection<String>>("getTags[" + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getTags(ident, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Collection<TagHistory> getTagHistory (WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final ServletWaiter<Collection<TagHistory>> waiter =
            new ServletWaiter<Collection<TagHistory>>("getTagHistory[" + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getTagHistory(ident, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Collection<TagHistory> getRecentTags (WebCreds creds)
        throws ServiceException
    {
        final int memberId = creds.memberId;
        final ServletWaiter<Collection<TagHistory>> waiter =
            new ServletWaiter<Collection<TagHistory>>("getTagHistory[" + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getRecentTags(memberId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public TagHistory tagItem (final WebCreds creds, final ItemIdent ident, final String tag,
                               final boolean set)
        throws ServiceException
    {
        final ServletWaiter<TagHistory> waiter = new ServletWaiter<TagHistory>(
            "tagItem[" + ident + ", " + set + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.tagItem(ident, creds.memberId, tag, set, waiter);
            }
        });
        return waiter.waitForResult();
    }
}
