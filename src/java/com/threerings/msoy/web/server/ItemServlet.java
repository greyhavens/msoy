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

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.gwt.ItemDetail;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.RatingRecord;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.presents.data.InvocationCodes;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class ItemServlet extends MsoyServiceServlet
    implements ItemService
{
    // from interface ItemService
    public int createItem (WebIdent ident, Item item, ItemIdent parent)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);

        // validate the item
        if (!item.isConsistent()) {
            log.warning("Got inconsistent item for upload? [from=" + memrec.getName() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // TODO: determine this item's suite id
        int suiteId = 0;

        // TODO: validate anything else?

        // configure the item's creator and owner
        item.creatorId = memrec.memberId;
        item.ownerId = memrec.memberId;

        // write the item to the database
        final ItemRecord record = ItemRecord.newRecord(item);
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(item.getType());
        try {
            repo.insertOriginalItem(record, false);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to create item " + item + ".", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // let the item manager know that we've created this item
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.itemCreated(record);
            }
        });

        return record.itemId;
    }

    // from interface ItemService
    public void updateItem (WebIdent ident, Item item)
        throws ServiceException
    {
        // TODO: validate this user's ident

        // validate the item
        if (!item.isConsistent()) {
            // TODO?
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // TODO: validate anything else?

        // write the item to the database
        final ItemRecord record = ItemRecord.newRecord(item);
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(item.getType());
        try {
            repo.updateOriginalItem(record);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update item " + item + ".", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // let the item manager know that we've updated this item
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.itemUpdated(record);
            }
        });
    }

    // from interface ItemService
    public Item loadItem (WebIdent ident, ItemIdent item)
        throws ServiceException
    {
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(item.type);
        try {
            ItemRecord irec = repo.loadItem(item.itemId);
            return (irec == null) ? null : irec.toItem();

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load item [id=" + item + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public ItemDetail loadItemDetail (WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);
        int memberId = (mrec == null) ? 0 : mrec.memberId;
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);

        try {
            ItemRecord record = repo.loadItem(iident.itemId);
            if (record == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // the detail contains the item...
            ItemDetail detail = new ItemDetail();
            detail.item = record.toItem();

            // its ratings...
            if (memberId != 0) {
                RatingRecord<ItemRecord> rr = repo.getRating(iident.itemId, memberId);
                detail.memberRating = (rr == null) ? 0 : rr.rating;
            }

            // the creator's name
            MemberRecord crrec = MsoyServer.memberRepo.loadMember(record.creatorId);
            if (crrec == null) {
                log.warning("Item missing creator " + record + ".");
            } else {
                detail.creator = crrec.getName();
            }

            // and the owner's name
            if (record.ownerId != 0) {
                crrec = MsoyServer.memberRepo.loadMember(record.ownerId);
                if (crrec != null) {
                    detail.owner = crrec.getName();
                } else {
                    log.warning("Item missing owner " + record + ".");
                }
            }

            return detail;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load item detail [id=" + iident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void scaleAvatar (WebIdent ident, int avatarId, float newScale)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        AvatarRepository repo = MsoyServer.itemMan.getAvatarRepository();
        try {
            final AvatarRecord avatar = repo.loadItem(avatarId);
            if (avatar == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
            if (avatar.ownerId != memrec.memberId) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            avatar.scale = newScale;
            repo.updateScale(avatarId, newScale);

            // let the item manager know that we've updated this item
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.itemUpdated(avatar);
                }
            });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to scale avatar [for=" + memrec.memberId +
                    ", aid=" + avatarId + ", scale=" + newScale + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Item remixItem (WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);

        try {
            // load a copy of the clone to modify
            final ItemRecord item = repo.loadClone(iident.itemId);
            if (item == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
            if (item.ownerId != memrec.memberId) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }
            // TODO: make sure item is remixable

            // prep the item for remixing and insert it as a new original item
            int originalId = item.sourceId;
            item.prepareForRemixing();
            repo.insertOriginalItem(item, false);

            // delete the old clone
            repo.deleteItem(iident.itemId);

            // copy tags from the original to the new item
            repo.getTagRepository().copyTags(
                originalId, item.itemId, item.ownerId, System.currentTimeMillis());

            // let the item manager know that we've created a new item
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.itemCreated(item);
                }
            });

            return item.toItem();

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to remix item [item=" + iident +
                    ", for=" + memrec.memberId + "]", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void deleteItem (final WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);

        try {
            final ItemRecord item = repo.loadItem(iident.itemId);
            if (item == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
            if (item.ownerId != memrec.memberId) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }
            if (item.used != 0) {
                throw new ServiceException(ItemCodes.E_ITEM_IN_USE);
            }
            if (item.catalogId != 0) {
                throw new ServiceException(ItemCodes.E_ITEM_LISTED);
            }
            repo.deleteItem(iident.itemId);

            // let the item manager know that we've deleted this item
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.itemDeleted(item);
                }
            });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to delete item [item=" + iident +
                    ", for=" + memrec.memberId + "]", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public float rateItem (WebIdent ident, ItemIdent iident, byte rating)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);

        try {
            ItemRecord item = repo.loadItem(iident.itemId);
            if (item == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            int originalId;
            if (item.sourceId != 0) {
                // it's a clone: use the source id
                originalId = item.sourceId;
            } else {
                // not a clone; make sure we're not trying to rate a mutable
                if (item.ownerId != 0) {
                    log.warning("Can't rate mutable item [id=" + iident + ", rating=" + rating +
                                ", for=" + memrec.memberId + "].");
                    throw new ServiceException(ItemCodes.INTERNAL_ERROR);
                }
                // and use our real ID
                originalId = iident.itemId;
            }

            // record this player's rating and obtain the new summarized rating
            return repo.rateItem(originalId, memrec.memberId, rating);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to rate item [item=" + iident +
                    ", rating=" + rating + ", for=" + memrec.memberId + "]", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Collection<String> getTags (WebIdent ident, final ItemIdent item)
        throws ServiceException
    {
        final ServletWaiter<Collection<String>> waiter =
            new ServletWaiter<Collection<String>>("getTags[" + item + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getTags(item, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Collection<TagHistory> getTagHistory (WebIdent ident, final ItemIdent item)
        throws ServiceException
    {
        final ServletWaiter<Collection<TagHistory>> waiter =
            new ServletWaiter<Collection<TagHistory>>("getTagHistory[" + item + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getTagHistory(item, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public Collection<TagHistory> getRecentTags (WebIdent ident)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(ident);
        final ServletWaiter<Collection<TagHistory>> waiter =
            new ServletWaiter<Collection<TagHistory>>("getTagHistory[" + mrec.memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.getRecentTags(mrec.memberId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public TagHistory tagItem (final WebIdent ident, final ItemIdent item, final String tag,
                               final boolean set)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        final ServletWaiter<TagHistory> waiter = new ServletWaiter<TagHistory>(
            "tagItem[" + item + ", " + set + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.tagItem(item, memrec.memberId, tag, set, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface ItemService
    public void wrapItem (WebIdent ident, ItemIdent iident, boolean wrap)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        byte type = iident.type;
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        try {
            ItemRecord item = repo.loadItem(iident.itemId);
            if (item == null) {
                log.warning("Trying to " + (wrap ? "" : "un") + "wrap non-existent item " +
                            "[ident=" + ident + ", item=" + iident + "]");
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
            if (wrap) {
                if (item.ownerId != memrec.memberId) {
                    log.warning("Trying to wrap un-owned item [ident=" + ident +
                                ", item=" + iident + "]");
                    throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
                }
                repo.updateOwnerId(item, 0);

            } else {
                if (item.ownerId != 0) {
                    log.warning("Trying to unwrap owned item [ident=" + ident +
                                ", item=" + iident + "]");
                    throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
                }
                repo.updateOwnerId(item, memrec.memberId);
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to wrap item [item=" + iident +
                    ", wrap=" + wrap + "]", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void setMature (WebIdent ident, ItemIdent iident, boolean value)
        throws ServiceException
    {
        MemberRecord mRec = requireAuthedUser(ident);
        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);
        try {
            // TODO: If things get really tight, this could use updatePartial() later.
            ItemRecord item = repo.loadItem(iident.itemId);
            if (item == null) {
                log.warning("Missing item for setFlags [id=" + iident + ", value=" + value + "].");
                throw new ServiceException(ItemCodes.INTERNAL_ERROR);
            }
            item.mature = value;
            repo.updateOriginalItem(item);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING,
                "Failed to set flags [item=" + iident + ", value=" + value + "]", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void setFlags (WebIdent ident, ItemIdent iident, byte mask, byte value)
        throws ServiceException
    {
        requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);
        try {
            // TODO: If things get really tight, this could use updatePartial() later.
            ItemRecord item = repo.loadItem(iident.itemId);
            if (item == null) {
                log.warning("Missing item for setFlags() [item=" + iident + ", mask=" + mask +
                            ", value=" + value + "].");
                throw new ServiceException(ItemCodes.INTERNAL_ERROR);
            }
            item.flagged = (byte) ((item.flagged & ~mask) | value);
            repo.updateOriginalItem(item);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to set flags [item=" + iident + ", mask=" + mask +
                    ", value=" + value + "]", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public List getFlaggedItems (WebIdent ident, int count)
        throws ServiceException
    {
        MemberRecord mRec = requireAuthedUser(ident);
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
                for (ItemRecord record : repo.loadFlaggedItems(count)) {
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
    public Integer deleteItemAdmin (WebIdent ident, ItemIdent iident, String subject, String body)
        throws ServiceException
    {
        MemberRecord mRec = requireAuthedUser(ident);
        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }
        byte type = iident.type;
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        try {
            ItemRecord item = repo.loadOriginalItem(iident.itemId);
            IntSet owners = new ArrayIntSet();

            int deletionCount = 0;
            owners.add(item.creatorId);

            // if this is the prototype for a listed item, delist it
            if (item.catalogId != 0) {
                CatalogRecord catrec = repo.loadListing(item.catalogId, false);
                if (catrec != null && catrec.listedItemId == item.itemId) {
                    repo.removeListing(catrec);
                }
            }

            // then delete any potential clones
            for (CloneRecord record : repo.loadCloneRecords(item.itemId)) {
                repo.deleteItem(record.itemId);
                deletionCount ++;
                owners.add(record.ownerId);
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
            log.log(Level.WARNING, "Admin item delete failed [item=" + iident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }
}
