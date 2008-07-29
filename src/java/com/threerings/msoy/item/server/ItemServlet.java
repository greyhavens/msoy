//
// $Id$

package com.threerings.msoy.item.server;

import static com.threerings.msoy.Log.log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.server.MailLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

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
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        return _itemLogic.createItem(memrec, item, parent);
    }

    // from interface ItemService
    public void updateItem (WebIdent ident, Item item)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        // make sure the item in question is consistent as far as the item is concerned
        if (!item.isConsistent()) {
            log.warning("Requested to update item with invalid version [who=" + memrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(item.getType());
        try {
            // load up the old version of the item
            final ItemRecord record = repo.loadItem(item.itemId);
            if (record == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // make sure they own it and created it, or are support+
            if (((record.ownerId != memrec.memberId) || (record.creatorId != memrec.memberId)) &&
                    !memrec.isSupport()) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // update it with data from the supplied runtime record
            record.fromItem(item);

            // write it back to the database
            repo.updateOriginalItem(record);

            // let the item manager know that we've updated this item
            postDObjectAction(new Runnable() {
                public void run () {
                    _itemMan.itemUpdated(record);
                }
            });

        } catch (PersistenceException pe) {
            log.warning("Failed to update item " + item + ".", pe);
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
            public void doOp (CloneRecord<ItemRecord> record, ItemRecord orig,
                              ItemRepository<ItemRecord, ?, ?, ?> repo)
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
    public List<Item> loadInventory (WebIdent ident, byte type, int suiteId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + ident + ", type=" + type + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(type);
        try {
            List<Item> items = Lists.newArrayList();
            for (ItemRecord record : repo.loadOriginalItems(memrec.memberId, suiteId)) {
                items.add(record.toItem());
            }
            for (ItemRecord record : repo.loadClonedItems(memrec.memberId, suiteId)) {
                items.add(record.toItem());
            }
            Collections.sort(items);
            return items;

        } catch (PersistenceException pe) {
            log.warning("loadInventory failed [for=" + memrec.memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Item loadItem (WebIdent ident, ItemIdent item)
        throws ServiceException
    {
        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(item.type);
        try {
            ItemRecord irec = repo.loadItem(item.itemId);
            return (irec == null) ? null : irec.toItem();

        } catch (PersistenceException pe) {
            log.warning("Failed to load item [id=" + item + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public ItemService.DetailOrIdent loadItemDetail (WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);

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
                    return new ItemService.DetailOrIdent(null,
                        new ItemIdent(iident.type, record.catalogId));
                } else {
                    throw new ServiceException(ItemCodes.E_ACCESS_DENIED); // fall back to error
                }
            }

            ItemDetail detail = new ItemDetail();
            detail.item = record.toItem();
            detail.creator = ((mrec != null) && (record.creatorId == mrec.memberId)) ?
                mrec.getName() : // shortcut for items we created
                _memberRepo.loadMemberName(record.creatorId); // normal lookup
            if (mrec != null) {
                detail.memberItemInfo.memberRating = repo.getRating(iident.itemId, mrec.memberId);
                detail.memberItemInfo.favorite = _itemMan.isFavorite(mrec.memberId, iident);
            }
            switch (detail.item.used) {
            case Item.USED_AS_FURNITURE:
            case Item.USED_AS_PET:
            case Item.USED_AS_BACKGROUND:
                detail.useLocation = _sceneRepo.identifyScene(detail.item.location);
                break;
            }
            return new ItemService.DetailOrIdent(detail, null);

        } catch (PersistenceException pe) {
            log.warning("Failed to load item detail [id=" + iident + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void scaleAvatar (WebIdent ident, int avatarId, float newScale)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        AvatarRepository repo = _itemMan.getAvatarRepository();
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
            postDObjectAction(new Runnable() {
                public void run () {
                    _itemMan.itemUpdated(avatar);
                }
            });

        } catch (PersistenceException pe) {
            log.warning("Failed to scale avatar [for=" + memrec.memberId +
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
//        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
//        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);
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
//            postDObjectAction(new Runnable() {
//                public void run () {
//                    _itemMan.itemCreated(item);
//                }
//            });
//
//            return item.toItem();
//
//        } catch (PersistenceException pe) {
//            log.warning("Failed to remix item [item=" + iident +
//                    ", for=" + memrec.memberId + "]", pe);
//            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//        }
//    }

    // from interface ItemService
    public void deleteItem (final WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);

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
            postDObjectAction(new Runnable() {
                public void run () {
                    _itemMan.itemDeleted(item);
                }
            });

        } catch (PersistenceException pe) {
            log.warning("Failed to delete item [item=" + iident +
                    ", for=" + memrec.memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public float rateItem (WebIdent ident, ItemIdent iident, byte rating)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);

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
            log.warning("Failed to rate item [item=" + iident +
                    ", rating=" + rating + ", for=" + memrec.memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Collection<String> getTags (WebIdent ident, ItemIdent iident)
        throws ServiceException
    {
        try {
            ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);
            List<String> result = Lists.newArrayList();
            for (TagNameRecord tagName : repo.getTagRepository().getTags(iident.itemId)) {
                result.add(tagName.tag);
            }
            return result;
        } catch (PersistenceException pe) {
            log.warning("Failed to get tags [item=" + iident + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Collection<TagHistory> getTagHistory (WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        try {
            ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);
            List<TagHistoryRecord> records =
                repo.getTagRepository().getTagHistoryByTarget(iident.itemId);
            IntMap<MemberName> names = _memberRepo.loadMemberNames(
                records, TagHistoryRecord.GET_MEMBER_ID);

            List<TagHistory> list = Lists.newArrayList();
            for (TagHistoryRecord threc : records) {
                TagNameRecord tag = repo.getTagRepository().getTag(threc.tagId);
                TagHistory history = new TagHistory();
                history.member = names.get(threc.memberId);
                history.tag = tag.tag;
                history.action = threc.action;
                history.time = new Date(threc.time.getTime());
                list.add(history);
            }
            return list;

        } catch (PersistenceException pe) {
            log.warning("Failed to get tag history [item=" + iident + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Collection<TagHistory> getRecentTags (WebIdent ident)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        MemberName name = memrec.getName();

        try {
            List<TagHistory> list = Lists.newArrayList();
            for (byte type : _itemMan.getRepositoryTypes()) {
                ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(type);
                for (TagHistoryRecord record :
                         repo.getTagRepository().getTagHistoryByMember(memrec.memberId)) {
                    TagNameRecord tag = (record.tagId == -1) ? null :
                        repo.getTagRepository().getTag(record.tagId);
                    TagHistory history = new TagHistory();
                    history.member = name;
                    history.tag = (tag == null) ? null : tag.tag;
                    history.action = record.action;
                    history.time = new Date(record.time.getTime());
                    list.add(history);
                }
            }
            return list;

        } catch (PersistenceException pe) {
            log.warning("Failed to get recent tags [ident=" + ident + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public TagHistory tagItem (WebIdent ident, ItemIdent iident, String rawTagName, boolean set)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        // sanitize the tag name
        final String tagName = rawTagName.trim().toLowerCase();

        // the client should protect us from invalid names, but we double check
        if (!TagNameRecord.VALID_TAG.matcher(tagName).matches()) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);
            long now = System.currentTimeMillis();

            ItemRecord item = repo.loadItem(iident.itemId);
            if (item == null) {
                throw new PersistenceException("Missing item for tagItem [item=" + iident + "]");
            }
            int originalId = (item.sourceId != 0) ? item.sourceId : iident.itemId;

            // map tag to tag id
            TagNameRecord tag = repo.getTagRepository().getOrCreateTag(tagName);

            // do the actual work
            TagHistoryRecord historyRecord = set ?
                repo.getTagRepository().tag(originalId, tag.tagId, memrec.memberId, now) :
                repo.getTagRepository().untag(originalId, tag.tagId, memrec.memberId, now);
            if (historyRecord == null) {
                return null;
            }

            // report on this history event
            TagHistory history = new TagHistory();
            history.member = memrec.getName();
            history.tag = tag.tag;
            history.action = historyRecord.action;
            history.time = new Date(historyRecord.time.getTime());
            return history;

        } catch (PersistenceException pe) {
            log.warning("Failed to tag item", "ident", ident, "item", iident, "tag", tagName,
                        "set", set, pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void wrapItem (WebIdent ident, ItemIdent iident, boolean wrap)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        byte type = iident.type;
        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(type);
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
            log.warning("Failed to wrap item [item=" + iident + ", wrap=" + wrap + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void setMature (WebIdent ident, ItemIdent iident, boolean value)
        throws ServiceException
    {
        MemberRecord mRec = _mhelper.requireAuthedUser(ident);
        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);
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
            log.warning("Failed to set flags [item=" + iident + ", value=" + value + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void setFlags (WebIdent ident, ItemIdent iident, byte mask, byte value)
        throws ServiceException
    {
        _mhelper.requireAuthedUser(ident);
        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(iident.type);
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
            log.warning("Failed to set flags [item=" + iident + ", mask=" + mask +
                    ", value=" + value + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public List<ItemDetail> getFlaggedItems (WebIdent ident, int count)
        throws ServiceException
    {
        MemberRecord mRec = _mhelper.requireAuthedUser(ident);
        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }
        List<ItemDetail> items = Lists.newArrayList();
        // it'd be nice to round-robin the item types or something, so the first items in the queue
        // aren't always from the same type... perhaps we'll just do something clever in the UI
        try {
            for (byte type : _itemMan.getRepositoryTypes()) {
                ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(type);
                for (ItemRecord record : repo.loadFlaggedItems(count)) {
                    Item item = record.toItem();

                    // get auxiliary info and construct an ItemDetail
                    ItemDetail detail = new ItemDetail();
                    detail.item = item;
                    detail.creator = _memberRepo.loadMemberName(record.creatorId);

                    // add the detail to our result and see if we're done
                    items.add(detail);
                    if (items.size() == count) {
                        return items;
                    }
                }
            }
            return items;

        } catch (PersistenceException pe) {
            log.warning("Getting flagged items failed.", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public Integer deleteItemAdmin (WebIdent ident, ItemIdent iident, String subject, String body)
        throws ServiceException
    {
        MemberRecord admin = _mhelper.requireAuthedUser(ident);
        if (!admin.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }

        byte type = iident.type;
        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(type);
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
                MemberRecord owner = _memberRepo.loadMember(ownerId);
                if (owner != null) {
                    _mailLogic.startConversation(admin, owner, subject, body, null);
                }
            }

            return Integer.valueOf(deletionCount);

        } catch (PersistenceException pe) {
            log.warning("Admin item delete failed [item=" + iident + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // defines ItemService interface
    public void setFavorite (WebIdent ident, ItemIdent item, boolean favorite)
        throws ServiceException
    {
        MemberRecord member = _mhelper.requireAuthedUser(ident);

        try {
            if(favorite) {
                _itemMan.addFavorite(member.memberId, item);
            }
            else {
                _itemMan.removeFavorite(member.memberId, item);
            }
        } catch(PersistenceException pex) {
            log.warning("Could not set favorite for [member="+member.memberId+", item=" + item + "].", pex);
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
            public void doOp (CloneRecord<ItemRecord> record, ItemRecord orig,
                              ItemRepository<ItemRecord, ?, ?, ?> repo)
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
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(itemIdent.type);
        try {
            // load up the old version of the item
            CloneRecord<ItemRecord> record = repo.loadCloneRecord(itemIdent.itemId);
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
            postDObjectAction(new Runnable() {
                public void run () {
                    _itemMan.itemUpdated(orig);
                }
            });

            return orig;

        } catch (PersistenceException pe) {
            log.warning("Failed to edit clone " + itemIdent + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * A small helper interface for editClone.
     */
    protected static interface CloneEditOp
    {
        public void doOp (CloneRecord<ItemRecord> record, ItemRecord orig,
                          ItemRepository<ItemRecord, ?, ?, ?> repo)
            throws PersistenceException;
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ItemManager _itemMan;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MsoySceneRepository _sceneRepo;
}
