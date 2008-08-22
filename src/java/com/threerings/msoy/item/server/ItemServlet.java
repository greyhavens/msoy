//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PhotoRecord;
import com.threerings.msoy.item.server.persist.PhotoRepository;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class ItemServlet extends MsoyServiceServlet
    implements ItemService
{
    // from interface ItemService
    public void scaleAvatar (int avatarId, float newScale)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        AvatarRepository repo = _itemLogic.getAvatarRepository();
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
//    public Item remixItem (final ItemIdent iident)
//        throws ServiceException
//    {
//        MemberRecord memrec = requireAuthedUser();
//        ItemRepository<ItemRecord> repo = _itemMan.getRepository(iident.type);
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
    public float rateItem (ItemIdent iident, byte rating, boolean isFirstRating)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);

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

            // if this is the first time the player has rated this item, increment the stat.
            if (isFirstRating && item.creatorId != memrec.memberId) {
                _statLogic.incrementStat(memrec.memberId, StatType.ITEMS_RATED, 1);
            }

            // record this player's rating and obtain the new summarized rating
            Tuple<Float, Boolean> ratingResult = repo.rateItem(originalId, memrec.memberId, rating);
            // if this qualifies as a new "solid 4+ rating", we have a stat to update
            if (ratingResult.right) {
                _statLogic.addToSetStat(item.creatorId, StatType.SOLID_4_STAR_RATINGS, originalId);
            }
            return ratingResult.left;

        } catch (PersistenceException pe) {
            log.warning("Failed to rate item [item=" + iident +
                    ", rating=" + rating + ", for=" + memrec.memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public List<String> getTags (ItemIdent iident)
        throws ServiceException
    {
        try {
            ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
            List<TagNameRecord> trecs = repo.getTagRepository().getTags(iident.itemId);
            return Lists.newArrayList(Iterables.transform(trecs, TagNameRecord.TO_TAG));
        } catch (PersistenceException pe) {
            log.warning("Failed to get tags [item=" + iident + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public List<TagHistory> getTagHistory (final ItemIdent iident)
        throws ServiceException
    {
        try {
            ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
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
    public List<TagHistory> getRecentTags ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        MemberName name = memrec.getName();

        try {
            List<TagHistory> list = Lists.newArrayList();
            for (byte type : _itemLogic.getRepositoryTypes()) {
                ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
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
            log.warning("Failed to get recent tags [for=" + who(memrec) + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public TagHistory tagItem (ItemIdent iident, String rawTagName, boolean set)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        // sanitize the tag name
        final String tagName = rawTagName.trim().toLowerCase();

        // the client should protect us from invalid names, but we double check
        if (!TagNameRecord.VALID_TAG.matcher(tagName).matches()) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
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
            log.warning("Failed to tag item", "for", who(memrec), "item", iident, "tag", tagName,
                        "set", set, pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void wrapItem (ItemIdent iident, boolean wrap)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        byte type = iident.type;
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        try {
            final ItemRecord item = repo.loadItem(iident.itemId);
            if (item == null) {
                log.warning("Trying to " + (wrap ? "" : "un") + "wrap non-existent item " +
                            "[for=" + who(memrec) + ", item=" + iident + "]");
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
            if (wrap) {
                if (item.ownerId != memrec.memberId) {
                    log.warning("Trying to wrap un-owned item [for=" + who(memrec) +
                                ", item=" + iident + "]");
                    throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
                }
                repo.updateOwnerId(item, 0);

            } else {
                if (item.ownerId != 0) {
                    if (item.ownerId == memrec.memberId) {
                        // if the owner is already correct, let it pass
                        log.warning("Unwrapped item already belongs to me [for=" + who(memrec) +
                                    ", item=" + iident + "]");
                        return;
                    }
                    log.warning("Trying to unwrap owned item [for=" + who(memrec) +
                                ", item=" + iident + "]");
                    throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
                }
                repo.updateOwnerId(item, memrec.memberId);
            }

            // let the item manager know that we've updated this item
            final int memId = memrec.memberId;
            postDObjectAction(new Runnable() {
                public void run () {
                    _itemMan.itemUpdated(item, memId);
                }
            });

        } catch (PersistenceException pe) {
            log.warning("Failed to wrap item [item=" + iident + ", wrap=" + wrap + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public void setMature (ItemIdent iident, boolean value)
        throws ServiceException
    {
        MemberRecord mRec = requireAuthedUser();
        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
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
    public void setFlags (ItemIdent iident, byte mask, byte value)
        throws ServiceException
    {
        requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
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

    // from ItemService interface
    public void setFavorite (int catalogId, byte itemType, boolean favorite)
        throws ServiceException
    {
        MemberRecord member = requireAuthedUser();

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
        try {
            // load up the catalog record
            CatalogRecord record = repo.loadListing(catalogId, false);

            if (record == null) {
                log.warning("Could not set favorite. Could not find catalog record.", "catalogId",
                    catalogId, "itemType", itemType);
            } else {
                ItemIdent ident = new ItemIdent(itemType, record.listedItemId);
                if (favorite) {
                    _itemLogic.addFavorite(member.memberId, ident);
                    _itemLogic.incrementFavoriteCount(record, itemType);
                } else {
                    _itemLogic.removeFavorite(member.memberId, ident);
                    _itemLogic.decrementFavoriteCount(record, itemType);
                }
            }

        } catch (PersistenceException pex) {
            log.warning("Could not set favorite.", "catalogId", catalogId, "type", itemType, pex);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public List<Photo> loadPhotos ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        try {
            List<Photo> photos = Lists.newArrayList();
            for (PhotoRecord record : _photoRepo.loadOriginalItems(memrec.memberId, 0)) {
                photos.add((Photo)record.toItem());
            }
            for (PhotoRecord record : _photoRepo.loadClonedItems(memrec.memberId, 0)) {
                photos.add((Photo)record.toItem());
            }
            Collections.sort(photos);
            return photos;

        } catch (PersistenceException pe) {
            log.warning("loadInventory failed [for=" + memrec.memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
    public ItemListResult loadItemList (ItemListQuery query) throws ServiceException
    {
        try {
            ItemListResult result = new ItemListResult();
            result.items = _itemLogic.loadItemList(query);
            if (query.needsCount) {
                result.totalCount = _itemLogic.getItemListSize(query.listId, query.itemType);
            }
            return result;

        } catch (PersistenceException pex) {
            log.warning("Could not load item list.", "query", query, pex);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ItemService
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
    @Inject protected PhotoRepository _photoRepo;
    @Inject protected StatLogic _statLogic;
}
