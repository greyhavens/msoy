//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.msoy.person.server.persist.MailMessageRecord;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.ItemIdent;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.TagHistory;

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
        item.creatorId = creds.getMemberId();
        item.ownerId = creds.getMemberId();

        final MemberName name = creds.name;
        // pass the buck to the item manager to do the dirty work
        final ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "insertItem[" + creds + ", " + item + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.insertItem(item, waiter);
                MsoyServer.memberMan.logUserAction(name, UserAction.CREATED_ITEM, item.toString());
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
        final int memberId = (creds == null) ? -1 : creds.getMemberId();
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
                MsoyServer.itemMan.deleteItemFor(creds.getMemberId(), ident, waiter);
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
                MsoyServer.itemMan.rateItem(ident, creds.getMemberId(), rating, waiter);
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
        final int memberId = creds.getMemberId();
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
                MsoyServer.itemMan.tagItem(ident, creds.getMemberId(), tag, set, waiter);
            }
        });
        return waiter.waitForResult();
    }
    
    // from interface ItemService
    public void setFlags (final WebCreds creds, final ItemIdent ident, final byte mask,
                          final byte value)
        throws ServiceException
    {
        MemberRecord mRec = requireAuthedUser(creds);
        if (!mRec.isSupport() && (mask & Item.FLAG_MATURE) != 0) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
                "setFlags[" + ident + ", " + mask + ", " + value + "]");
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.setFlags(ident, mask, value, waiter);
                }
            });
            waiter.waitForResult();
    }

    // from interface ItemService
    public List getFlaggedItems (WebCreds creds, int count)
        throws ServiceException
    {
        MemberRecord mRec = requireAuthedUser(creds);
        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }
        List<ItemDetail> items = new ArrayList<ItemDetail>();
        // it'd be nice to round-robin the item types or something, so the first items in
        // the queue aren't always from the same type... perhaps we'll just do something
        // clever in the UI
        try {
            for (byte type : MsoyServer.itemMan.getRepositoryTypes()) {
                ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
                byte mask = (byte) (Item.FLAG_FLAGGED_COPYRIGHT | Item.FLAG_FLAGGED_MATURE);
                for (ItemRecord record : repo.loadItemsByFlag(mask, false, count)) {
                    Item item = record.toItem();
                    
                    // get auxillary info and construct an ItemDetail
                    ItemDetail detail = new ItemDetail();
                    detail.item = item;
                    detail.memberRating = 0; // not populated
                    MemberRecord memRec = MsoyServer.memberRepo.loadMember(record.creatorId);
                    detail.creator = memRec.getName();
                    detail.owner = null; // not populated

                    // add the detail to our result and see if we're done
                    items.add(detail);
                    if (items.size() == count) {
                        return items;
                    }
                }
            }
            return items;
            
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting flagged items failed.", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Integer deleteItemAdmin (WebCreds creds, ItemIdent ident, String subject, String body)
        throws ServiceException
    {
        MemberRecord mRec = requireAuthedUser(creds);
        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }
        byte type = ident.type;
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        try {
            ItemRecord item = repo.loadOriginalItem(ident.itemId);
            IntSet owners = new ArrayIntSet();

            int deletionCount = 0;
            owners.add(item.creatorId);
            if (item.ownerId == 0) {
                // if this is a listed item, unlist it
                repo.removeListing(item.itemId);

                // then delete all the clones
                for (CloneRecord record : repo.loadCloneRecords(item.itemId)) {
                    repo.deleteItem(record.itemId);
                    deletionCount ++;
                    owners.add(record.ownerId);
                }
            }
            // finally delete the actual item
            repo.deleteItem(item.itemId);
            deletionCount ++;

            // build a message record
            MailMessageRecord record = new MailMessageRecord();
            record.senderId = 0;
            record.folderId = MailFolder.INBOX_FOLDER_ID;
            record.subject = subject;
            record.sent = new Timestamp(System.currentTimeMillis());
            record.bodyText = body;
            record.unread = true;

            // and notify everybody
            for (int ownerId : owners) {
                record.ownerId = ownerId;
                record.recipientId = ownerId;
                MsoyServer.mailMan.getRepository().fileMessage(record);
            }
            return Integer.valueOf(deletionCount);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Admin item delete failed [item=" + ident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }
}
