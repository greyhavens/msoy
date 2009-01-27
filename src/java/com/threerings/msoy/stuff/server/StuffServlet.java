//
// $Id$

package com.threerings.msoy.stuff.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.UploadUtil;

import com.threerings.msoy.stuff.gwt.StuffService;

/**
 * Provides the server implementation of {@link StuffService}.
 */
public class StuffServlet extends MsoyServiceServlet
    implements StuffService
{
    // from interface ItemService
    public MediaDesc publishExternalMedia (String data, byte mimeType)
        throws ServiceException
    {
        ExternalUploadFile file = new ExternalUploadFile(data, mimeType);
        try {
            UploadUtil.publishUploadFile(file);
            return new MediaDesc(file.getHash(), file.getMimeType(), MediaDesc.NOT_CONSTRAINED);
        } catch (IOException ioe) {
            log.warning("Unable to publish external media file", ioe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface StuffService
    public Item createItem (Item item, ItemIdent parent)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        item = _itemLogic.createItem(memrec.memberId, item, parent).toItem();

        // Some items have a stat that may need updating
        if (item instanceof Avatar) {
            _statLogic.ensureIntStatMinimum(
                memrec.memberId, StatType.AVATARS_CREATED, StatType.ITEM_UPLOADED);
        } else if (item instanceof Furniture) {
            _statLogic.ensureIntStatMinimum(
                memrec.memberId, StatType.FURNITURE_CREATED, StatType.ITEM_UPLOADED);
        } else if (item instanceof Decor) {
            _statLogic.ensureIntStatMinimum(
                memrec.memberId, StatType.BACKDROPS_CREATED, StatType.ITEM_UPLOADED);
        }

        // note in the event log that an item was uploaded
        _eventLog.itemUploaded(memrec.memberId, memrec.visitorId);

        return item;
    }

    // from interface StuffService
    public void updateItem (Item item)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        // make sure the item in question is consistent as far as the item is concerned
        if (!item.isConsistent()) {
            log.warning("Requested to update item with invalid version [who=" + memrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // load up the old version of the item
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.getType());
        ItemRecord record = repo.loadItem(item.itemId);
        if (record == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // make sure they own it and created it, or are support+
        if (((record.ownerId != memrec.memberId) || (record.creatorId != memrec.memberId)) &&
            !memrec.isSupport()) {
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }

        // make a copy of this for later
        ItemRecord oldrec = (ItemRecord)record.clone();

        // update it with data from the supplied runtime record
        record.fromItem(item);

        // make sure these modifications are copacetic
        _itemLogic.validateItem(memrec.memberId, oldrec, record);

        // write it back to the database
        repo.updateOriginalItem(record);

        // note that we've update our bits
        _itemLogic.itemUpdated(oldrec, record);
    }

    // from interface StuffService
    public Item remixItem (Item item)
        throws ServiceException
    {
        if (item.sourceId == 0) {
            // it's an original being remixed, it's the same as updateItem
            updateItem(item);
            return item;

        } else {
            return remixClone(item.getIdent(), item);
        }
    }

    // from interface StuffService
    public Item revertRemixedClone (ItemIdent itemIdent)
        throws ServiceException
    {
        return remixClone(itemIdent, null);
    }

    // from interface StuffService
    public String renameClone (ItemIdent itemIdent, String newName)
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
        MemberRecord mrec = requireAuthedUser();
        ItemRecord rec = _itemLogic.editClone(mrec, itemIdent, new ItemLogic.CloneEditOp() {
            public void doOp (CloneRecord record, ItemRecord orig, ItemRepository<ItemRecord> repo)
                throws Exception
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

    // from interface StuffService
    public List<Item> loadInventory (int memberId, byte type, String query)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if (memrec.memberId != memberId && !memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        // make sure they supplied a valid item type
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + who(memrec) + ", type=" + type + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        List<Item> items = Lists.newArrayList();
        Function<ItemRecord, Item> toItem = new ItemRecord.ToItem<Item>();
        if (StringUtil.isBlank(query)) {
            items.addAll(Lists.transform(repo.loadOriginalItems(memberId, 0), toItem));
            items.addAll(Lists.transform(repo.loadClonedItems(memberId, 0), toItem));
        } else {
            items.addAll(Lists.transform(repo.findItems(memberId, query), toItem));
        }
        Collections.sort(items);

        return items;
    }

    // from interface StuffService
    public List<Item> loadSubInventory (int memberId, byte type, int suiteId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if (memrec.memberId != memberId && !memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        // make sure they supplied a valid item type
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type",
                        "who", who(memrec), "type", type);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // first load things up normally
        List<Item> items = Lists.newArrayList();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        Function<ItemRecord, Item> toItem = new ItemRecord.ToItem<Item>();
        items.addAll(Lists.transform(repo.loadOriginalItems(memberId, suiteId), toItem));
        boolean hasOriginals = (items.size() > 0);
        items.addAll(Lists.transform(repo.loadClonedItems(memberId, suiteId), toItem));

        // if we found no originals owned by the caller and the suite id references an original
        // item and the caller is support+ then we want to do some jiggery pokery to load up the
        // originals owned by the parent item's owner; this allows support+ to see an item with
        // subitems as the owner sees it which is useful and reduces confusion
        if (!hasOriginals && suiteId > 0 && memrec.isSupport()) {
            // load up the parent item (parent item id is the suite id in this case)
            ItemRecord parent = _itemLogic.getRepository(getSuiteMasterType(type)).loadItem(suiteId);
            if (parent != null) {
                items.addAll(
                    Lists.transform(repo.loadOriginalItems(parent.ownerId, suiteId), toItem));
            }
        }

        Collections.sort(items);
        return items;
    }

    // from interface StuffService
    public Item loadItem (ItemIdent item)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.type);
        ItemRecord irec = repo.loadItem(item.itemId);
        // we only return the item metadata if they own it, it's a catalog master, or for agents
        boolean accessValid = (irec != null) &&
            ((irec.ownerId == mrec.memberId) || irec.isCatalogMaster() || mrec.isSupport());
        return accessValid ? irec.toItem() : null;
    }

    // from interface StuffService
    public DetailOrIdent loadItemDetail (final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);

        ItemRecord record = repo.loadItem(iident.itemId);
        if (record == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // if you're not the owner or support+, you cannot view original items
        if (record.ownerId != 0 && record.itemId > 0 &&
            (mrec == null || (!mrec.isSupport() && mrec.memberId != record.ownerId))) {
            // if it's listed, send them to the catalog
            if (record.catalogId != 0) {
                return new DetailOrIdent(null, new ItemIdent(iident.type, record.catalogId));
            } else {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED); // fall back to error
            }
        }

        ItemDetail detail = new ItemDetail();
        detail.item = record.toItem();
        detail.creator = ((mrec != null) && (record.creatorId == mrec.memberId)) ?
            mrec.getName() : // shortcut for items we created
            _memberRepo.loadMemberName(record.creatorId); // normal lookup
        detail.memberItemInfo = _itemLogic.getMemberItemInfo(mrec, detail.item);
        switch (detail.item.used) {
        case Item.USED_AS_FURNITURE:
        case Item.USED_AS_PET:
        case Item.USED_AS_BACKGROUND:
            detail.useLocation = _sceneRepo.identifyScene(detail.item.location);
            break;
        }
        List<TagNameRecord> trecs = repo.getTagRepository().getTags(iident.itemId);
        detail.tags = Lists.newArrayList(Iterables.transform(trecs, TagNameRecord.TO_TAG));

        return new DetailOrIdent(detail, null);
    }

    // from interface StuffService
    public void deleteItem (final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        _itemLogic.deleteItem(memrec, iident);
    }

    /**
     * Helper method for remixItem and revertRemixedClone.
     * @param item the updated item, or null to revert to the original mix.
     */
    protected Item remixClone (ItemIdent itemIdent, final Item item)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure the item isn't boochy
        if (item != null && !item.isConsistent()) {
            log.warning("Requested to remix item with invalid version [who=" + who(mrec) +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRecord rec = _itemLogic.editClone(mrec, itemIdent, new ItemLogic.CloneEditOp() {
            public void doOp (CloneRecord record, ItemRecord orig, ItemRepository<ItemRecord> repo)
                throws Exception
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
     * Helpy helper function.
     */
    protected static byte getSuiteMasterType (byte type)
        throws ServiceException
    {
        // determine the master type for this subitem type
        Item proto = null;
        try {
            proto = Item.getClassForType(type).newInstance();
        } catch (Exception e) {
            // no problem, we'll fail below
        }
        if (!(proto instanceof SubItem)) {
            log.warning("Requested suite master for non-SubItem type", "type", type);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        return ((SubItem)proto).getSuiteMasterType();
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected StatLogic _statLogic;
}
