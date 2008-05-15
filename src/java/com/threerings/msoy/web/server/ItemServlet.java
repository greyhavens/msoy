//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.data.gwt.ItemDetail;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.SubItemRecord;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class ItemServlet extends MsoyServiceServlet
    implements ItemService
{
    // from interface ItemService
    public Item createItem (WebIdent ident, Item item, ItemIdent parent)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo;

        // validate the item
        if (!item.isConsistent()) {
            log.warning("Got inconsistent item for upload? [from=" + memrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // create the persistent item record
        repo = MsoyServer.itemMan.getRepository(item.getType());
        final ItemRecord record = repo.newItemRecord(item);

        // configure the item's creator and owner
        record.creatorId = memrec.memberId;
        record.ownerId = memrec.memberId;

        // determine this item's suite id if it is a subitem
        if (item instanceof SubItem) {
            if (parent == null) {
                log.warning("Requested to create sub-item with no parent [who=" + memrec.who() +
                            ", item=" + item + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            ItemRepository<ItemRecord, ?, ?, ?> prepo =
                MsoyServer.itemMan.getRepository(parent.type);
            ItemRecord prec = null;
            try {
                prec = prepo.loadItem(parent.itemId);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to load parent in createItem [who=" + memrec.who() +
                        ", item=" + item.getIdent() + ", parent=" + parent + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            if (prec == null) {
                log.warning("Requested to make item with missing parent [who=" + memrec.who() +
                            ", parent=" + parent + ", item=" + item + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            if (prec.ownerId != memrec.memberId) {
                log.warning("Requested to make item with invalid parent [who=" + memrec.who() +
                            ", parent=" + prec + ", item=" + item + "].");
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // if everything is kosher, we can initialize the subitem with info from its parent
            ((SubItemRecord)record).initFromParent(prec);
        }

        // TODO: validate anything else?

        // write the item to the database
        try {
            repo.insertOriginalItem(record, false);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to create item " + item + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the item manager know that we've created this item
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.itemCreated(record);
            }
        });

        return record.toItem();
    }

    // from interface ItemService
    public void updateItem (WebIdent ident, Item item)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        // make sure the item in question is consistent as far as the item is concerned
        if (!item.isConsistent()) {
            log.warning("Requested to update item with invalid version [who=" + memrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(item.getType());
        try {
            // load up the old version of the item
            final ItemRecord record = repo.loadItem(item.itemId);
            if (record == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // make sure they own it (or are admin)
            if (record.ownerId != memrec.memberId && !memrec.isAdmin()) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // update it with data from the supplied runtime record
            record.fromItem(item);

            // write it back to the database
            repo.updateOriginalItem(record);

            // let the item manager know that we've updated this item
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.itemUpdated(record);
                }
            });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update item " + item + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Item remixItem (WebIdent ident, Item item)
        throws ServiceException
    {
        if (item.sourceId == 0) {
            // it's an original being remixed, it's the same as updateItem
            updateItem(ident, item);
            return item;

        } else {
            return remixClone(ident, item.getIdent(), item);
        }
    }

    // from interface ItemService
    public Item revertRemixedClone (WebIdent ident, ItemIdent itemIdent)
        throws ServiceException
    {
        return remixClone(ident, itemIdent, null);
    }

    // from interface ItemService
    public String renameClone (WebIdent ident, ItemIdent itemIdent, String newName)
        throws ServiceException
    {
        if (newName != null) {
            newName = newName.trim();
            if (newName.length() > Item.MAX_NAME_LENGTH) {
                // this'll only happen with a hacked client
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        final String fname = newName;
        ItemRecord rec = editClone(ident, itemIdent, new CloneEditOp() {
            public void doOp (CloneRecord record, ItemRecord orig, ItemRepository repo)
                throws PersistenceException
            {
                if (StringUtil.isBlank(fname) || fname.equals(orig.name)) {
                    record.name = null; // revert
                } else {
                    record.name = fname;
                }

                // save the updated info
                repo.updateCloneName(record);
            }
        });
        return rec.name;
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
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public IsSerializable loadItemDetail (WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);

        try {
            ItemRecord record = repo.loadItem(iident.itemId);
            if (record == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // if you're not the owner or support+, you cannot view original items
            if (record.ownerId != 0 && record.itemId > 0 &&
                    (mrec == null || (!mrec.isSupport() && mrec.memberId != record.ownerId))) {
                // if it's listed, send them to the catalog
                if (record.catalogId != 0) {
                    return new ItemIdent(iident.type, record.catalogId);
                } else {
                    throw new ServiceException(ItemCodes.E_ACCESS_DENIED); // fall back to error
                }
            }

            ItemDetail detail = new ItemDetail();
            detail.item = record.toItem();
            detail.creator = ((mrec != null) && (record.creatorId == mrec.memberId))
                ? mrec.getName() // shortcut for items we created
                : MsoyServer.memberRepo.loadMemberName(record.creatorId); // normal lookup
            if (mrec != null) {
                detail.memberRating = repo.getRating(iident.itemId, mrec.memberId);
            }
            switch (detail.item.used) {
            case Item.USED_AS_FURNITURE:
            case Item.USED_AS_PET:
            case Item.USED_AS_BACKGROUND:
                detail.useLocation = MsoyServer.sceneRepo.identifyScene(detail.item.location);
                break;
            }
            return detail;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load item detail [id=" + iident + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // TODO: this is dormant right now, but we might need something like it when we
    // enable listing purchased remixables.
//    // from interface ItemService
//    public Item remixItem (WebIdent ident, final ItemIdent iident)
//        throws ServiceException
//    {
//        MemberRecord memrec = requireAuthedUser(ident);
//        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(iident.type);
//
//        try {
//            // load a copy of the clone to modify
//            final ItemRecord item = repo.loadClone(iident.itemId);
//            if (item == null) {
//                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
//            }
//            if (item.ownerId != memrec.memberId) {
//                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
//            }
//            // TODO: make sure item is remixable
//
//            // prep the item for remixing and insert it as a new original item
//            int originalId = item.sourceId;
//            item.prepareForRemixing();
//            repo.insertOriginalItem(item, false);
//
//            // delete the old clone
//            repo.deleteItem(iident.itemId);
//
//            // copy tags from the original to the new item
//            repo.getTagRepository().copyTags(
//                originalId, item.itemId, item.ownerId, System.currentTimeMillis());
//
//            // let the item manager know that we've created a new item
//            MsoyServer.omgr.postRunnable(new Runnable() {
//                public void run () {
//                    MsoyServer.itemMan.itemCreated(item);
//                }
//            });
//
//            return item.toItem();
//
//        } catch (PersistenceException pe) {
//            log.log(Level.WARNING, "Failed to remix item [item=" + iident +
//                    ", for=" + memrec.memberId + "]", pe);
//            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//        }
//    }

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
            if (item.isCatalogOriginal()) {
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
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
                    throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
                }
                // and use our real ID
                originalId = iident.itemId;
            }

            // record this player's rating and obtain the new summarized rating
            return repo.rateItem(originalId, memrec.memberId, rating);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to rate item [item=" + iident +
                    ", rating=" + rating + ", for=" + memrec.memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
                    if (item.ownerId == memrec.memberId) {
                        // if the owner is already correct, let it pass
                        log.warning("Unwrapped item already belongs to me [ident=" + ident +
                            ", item=" + iident + "]");
                        return;
                    }
                    log.warning("Trying to unwrap owned item [ident=" + ident +
                        ", item=" + iident + "]");
                    throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
                }
                repo.updateOwnerId(item, memrec.memberId);
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to wrap item [item=" + iident +
                    ", wrap=" + wrap + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            item.mature = value;
            repo.updateOriginalItem(item, false);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING,
                "Failed to set flags [item=" + iident + ", value=" + value + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            item.flagged = (byte) ((item.flagged & ~mask) | value);
            repo.updateOriginalItem(item, false);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to set flags [item=" + iident + ", mask=" + mask +
                    ", value=" + value + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
        // it'd be nice to round-robin the item types or something, so the first items in the queue
        // aren't always from the same type... perhaps we'll just do something clever in the UI
        try {
            for (byte type : MsoyServer.itemMan.getRepositoryTypes()) {
                ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
                for (ItemRecord record : repo.loadFlaggedItems(count)) {
                    Item item = record.toItem();

                    // get auxillary info and construct an ItemDetail
                    ItemDetail detail = new ItemDetail();
                    detail.item = item;
                    detail.memberRating = 0; // not populated
                    detail.creator = MsoyServer.memberRepo.loadMemberName(record.creatorId);

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
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Integer deleteItemAdmin (WebIdent ident, ItemIdent iident, String subject, String body)
        throws ServiceException
    {
        MemberRecord admin = requireAuthedUser(ident);
        if (!admin.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }

        byte type = iident.type;
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        try {
            ItemRecord item = repo.loadOriginalItem(iident.itemId);
            IntSet owners = new ArrayIntSet();

            int deletionCount = 0;
            owners.add(item.creatorId);

            // we've loaded the original item, if it represents the original listing
            // or a prototype item, we want to squish the original catalog listing.
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

            // notify the owners of the deletion
            for (int ownerId : owners) {
                if (ownerId == admin.memberId) {
                    continue; // admin deleting their own item? sure, whatever!
                }
                MemberRecord owner = MsoyServer.memberRepo.loadMember(ownerId);
                if (owner != null) {
                    MsoyServer.mailMan.startConversation(admin, owner, subject, body, null);
                }
            }

            return Integer.valueOf(deletionCount);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Admin item delete failed [item=" + iident + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Helper method for remixItem and revertRemixedClone.
     * @param item the updated item, or null to revert to the original mix.
     */
    protected Item remixClone (WebIdent ident, ItemIdent itemIdent, final Item item)
        throws ServiceException
    {
        // make sure the item isn't boochy
        if (item != null && !item.isConsistent()) {
            log.warning("Requested to remix item with invalid version [who=" + ident +
                ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRecord rec = editClone(ident, itemIdent, new CloneEditOp() {
            public void doOp (CloneRecord record, ItemRecord orig, ItemRepository repo)
                throws PersistenceException
            {
                if (item == null) {
                    record.mediaHash = null; // we're reverting

                } else {
                    // in all probability, the primary media is different now
                    MediaDesc primary = item.getPrimaryMedia();
                    byte[] primaryHash = (primary == null) ? null : primary.hash;
                    if (Arrays.equals(primaryHash, orig.getPrimaryMedia())) {
                        record.mediaHash = null; // a revert here, strange, but ok
                    } else {
                        record.mediaHash = primaryHash;
                    }
                }

                // save the updated info
                repo.updateCloneMedia(record);
            }
        });
        return rec.toItem();
    }

    /**
     * Helper method for editing clones.
     */
    protected ItemRecord editClone (WebIdent ident, ItemIdent itemIdent, CloneEditOp op)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(itemIdent.type);
        try {
            // load up the old version of the item
            CloneRecord record = repo.loadCloneRecord(itemIdent.itemId);
            if (record == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // make sure they own it (or are admin)
            if (record.ownerId != memrec.memberId && !memrec.isAdmin()) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // load up the original record so we can see what changed
            final ItemRecord orig = repo.loadOriginalItem(record.originalItemId);
            if (orig == null) {
                log.warning("Unable to locate original of remixed clone [who=" + memrec.who() +
                    ", item=" + itemIdent + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            // do the operation
            op.doOp(record, orig, repo);

            // create the proper ItemRecord representing the clone
            orig.initFromClone(record);

            // let the item manager know that we've updated this item
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.itemUpdated(orig);
                }
            });

            return orig;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to edit clone " + itemIdent + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * A small helper interface for editClone.
     */
    protected static interface CloneEditOp
    {
        public void doOp (CloneRecord record, ItemRecord orig, ItemRepository repo)
            throws PersistenceException;
    }
}
