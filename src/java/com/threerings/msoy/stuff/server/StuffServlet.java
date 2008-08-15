//
// $Id$

package com.threerings.msoy.stuff.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;

import com.threerings.msoy.stuff.gwt.StuffService;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link StuffService}.
 */
public class StuffServlet extends MsoyServiceServlet
    implements StuffService
{
    // from interface StuffService
    public Item createItem (Item item, ItemIdent parent)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        item = _itemLogic.createItem(memrec, item, parent);

        // if the item is a game, create a group for it if it doesn't have one
        if (item instanceof Game && ((Game)item).groupId < 0) {
            createGameGroup((Game)item);
        }
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

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.getType());
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

        // if the item is a game, create a group for it if it doesn't have one
        if (item instanceof Game && ((Game)item).groupId < 0) {
            createGameGroup((Game)item);
        }
    }

    /**
     * Create a new group for the given game and connect it.
     */
    protected void createGameGroup (Game game)
        throws ServiceException
    {
        // if game was just created the gameId may be blank; fetch it from the repo
        if (game.gameId == 0) {
            ItemRepository<ItemRecord> repo = _itemLogic.getRepository(game.getType());
            try {
                GameRecord gameRecord = (GameRecord) repo.loadItem(game.itemId);
                game.gameId = gameRecord.gameId;
            } catch (PersistenceException pe) {
                log.warning("Failed to fetch gameId for new game " + game + ".", pe);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        Group group = Group.fromGame(game);
        MemberRecord mrec = requireAuthedUser();

        // check for an existing group with the same (unique) name.
        try {
            GroupRecord existingGroupRecord = _groupRepo.loadGroupByName(group.name);
            for (int i = 1; i < 10; i++) {
                if (existingGroupRecord == null) {
                    break;
                }
                // if player is a manager of the existing group, use that instead
                GroupMembershipRecord membership =
                    _groupRepo.getMembership(existingGroupRecord.groupId, mrec.memberId);
                // on the 9th try, give up and use the group anyway
                if (i == 9 ||
                    (membership != null && membership.rank >= GroupMembership.RANK_MANAGER)) {
                    game.groupId = existingGroupRecord.groupId;
                    updateItem(game);
                    return;
                }
                // not a manager of the existing group; change the name and search again
                if (i > 1 || group.name.length() >= GroupName.LENGTH_MAX - 1) {
                    group.name = group.name.substring(0, group.name.length() - 2);
                }
                group.name = group.name.concat(" " + (i+1));
                existingGroupRecord = _groupRepo.loadGroupByName(group.name);
            }
        } catch (PersistenceException pe) {
            log.warning("Failed to create group for game " + game + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // create a new group
        GroupExtras extras = GroupExtras.fromGame(game);
        group = _groupLogic.createGroup(requireAuthedUser(), group, extras);

        // update the game record again with the new groupId
        game.groupId = group.groupId;
        updateItem(game);
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

    // from interface StuffService
    public List<Item> loadInventory (byte type, int suiteId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + who(memrec) + ", type=" + type + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
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

    // from interface StuffService
    public Item loadItem (ItemIdent item)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.type);
        try {
            ItemRecord irec = repo.loadItem(item.itemId);
            return (irec == null) ? null : irec.toItem();

        } catch (PersistenceException pe) {
            log.warning("Failed to load item [id=" + item + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface StuffService
    public DetailOrIdent loadItemDetail (final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);

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
            if (mrec != null) {
                detail.memberItemInfo.memberRating = repo.getRating(iident.itemId, mrec.memberId);
                detail.memberItemInfo.favorite = _itemLogic.isFavorite(mrec.memberId, iident);
            }
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

        } catch (PersistenceException pe) {
            log.warning("Failed to load item detail [id=" + who(mrec) + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface StuffService
    public void deleteItem (final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);

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

    // from interface StuffService
    public int getSize (ItemListQuery query) throws ServiceException
    {
        try {
            return _itemLogic.getSize(query.listId, query.itemType);
        } catch (PersistenceException pex) {
            log.warning("Could not get size of item list.", "listId", query.listId, "itemType",
                query.itemType, pex);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface StuffService
    public ItemListResult loadItemList (ItemListQuery query) throws ServiceException
    {
        ItemListResult result = new ItemListResult();

        try {
            result.items = _itemLogic.loadItemList(query);
            if (query.needsCount) {
                result.totalCount = getSize(query);
            }
        } catch (PersistenceException pex) {
            log.warning("Could not load item list.", "query", query, pex);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return result;
    }

    // from interface StuffService
    public ItemListInfo getFavoriteListInfo (int memberId) throws ServiceException
    {
        try {
            return _itemLogic.getFavoriteListInfo (memberId);
        } catch (PersistenceException pex) {
            log.warning("Could not get favorite list info.", "memberId", memberId, pex);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ItemManager _itemMan;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
}
