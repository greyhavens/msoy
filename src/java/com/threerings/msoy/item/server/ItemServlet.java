//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

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
        final AvatarRecord avatar = repo.loadItem(avatarId);
        if (avatar == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        if (avatar.ownerId != memrec.memberId) {
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }

        // keep an unmodified copy around for our later call to itemUpdated()
        final AvatarRecord oavatar = (AvatarRecord)avatar.clone();

        avatar.scale = newScale;
        repo.updateScale(avatarId, newScale);

        // let the item system know that we've updated this item
        _itemLogic.itemUpdated(oavatar, avatar);
    }

    // TODO: this is dormant right now, but we might need something like it when we
    // enable listing purchased remixables.
    // from interface ItemService
//     public Item remixItem (final ItemIdent iident)
//         throws ServiceException
//     {
//         MemberRecord memrec = requireAuthedUser();
//         ItemRepository<ItemRecord> repo = _itemMan.getRepository(iident.type);

//         // load a copy of the clone to modify
//         final ItemRecord item = repo.loadClone(iident.itemId);
//         if (item == null) {
//             throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
//         }
//         if (item.ownerId != memrec.memberId) {
//             throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
//         }
//         // TODO: make sure item is remixable

//         // prep the item for remixing and insert it as a new original item
//         int originalId = item.sourceId;
//         item.prepareForRemixing();
//         repo.insertOriginalItem(item, false);

//         // delete the old clone
//         repo.deleteItem(iident.itemId);

//         // copy tags from the original to the new item
//         repo.getTagRepository().copyTags(
//             originalId, item.itemId, item.ownerId, System.currentTimeMillis());

//         // inform interested parties that we've created a new item
//         _itemLogic.itemUpdated(null, item); // TODO: need original

//         return item.toItem();
//    }

    // from interface ItemService
    public float rateItem (ItemIdent iident, byte rating, boolean isFirstRating)
        throws ServiceException
    {
        // Ensure the rating is within bounds
        if (rating < 1 || rating > 5) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        MemberRecord memrec = requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);

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
    }

    // from interface ItemService
    public List<String> getTags (ItemIdent iident)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
        List<TagNameRecord> trecs = repo.getTagRepository().getTags(iident.itemId);
        return Lists.newArrayList(Iterables.transform(trecs, TagNameRecord.TO_TAG));
    }

    // from interface ItemService
    public List<TagHistory> getTagHistory (final ItemIdent iident)
        throws ServiceException
    {
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
    }

    // from interface ItemService
    public List<TagHistory> getRecentTags ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        MemberName name = memrec.getName();

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

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
        long now = System.currentTimeMillis();

        ItemRecord item = repo.loadItem(iident.itemId);
        if (item == null) {
            log.warning("Missing item for tagItem", "who", memrec.who(), "ident", iident);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
    }

    // from interface ItemService
    public void wrapItem (ItemIdent iident, boolean wrap)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        byte type = iident.type;
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        final ItemRecord item = repo.loadItem(iident.itemId);
        if (item == null) {
            log.warning("Trying to " + (wrap ? "" : "un") + "wrap non-existent item " +
                        "[for=" + who(memrec) + ", item=" + iident + "]");
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        final ItemRecord oitem = (ItemRecord)item.clone();
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

        // let the item system know that we've updated this item
        _itemLogic.itemUpdated(oitem, item);
    }

    // from interface ItemService
    public void setMature (ItemIdent iident, boolean value)
        throws ServiceException
    {
        requireSupportUser();

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
        // TODO: If things get really tight, this could use updatePartial() later.
        ItemRecord item = repo.loadItem(iident.itemId);
        if (item == null) {
            log.warning("Missing item for setFlags [id=" + iident + ", value=" + value + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        item.mature = value;
        repo.updateOriginalItem(item, false);
    }

    // from interface ItemService
    public void setFlags (ItemIdent iident, byte mask, byte value)
        throws ServiceException
    {
        requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
        // TODO: If things get really tight, this could use updatePartial() later.
        ItemRecord item = repo.loadItem(iident.itemId);
        if (item == null) {
            log.warning("Missing item for setFlags() [item=" + iident + ", mask=" + mask +
                        ", value=" + value + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        item.flagged = (byte) ((item.flagged & ~mask) | value);
        repo.updateOriginalItem(item, false);
    }

    // from ItemService interface
    public void setFavorite (byte itemType, int catalogId, boolean favorite)
        throws ServiceException
    {
        MemberRecord member = requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
        CatalogRecord record = repo.loadListing(catalogId, false);
        if (record == null) {
            log.warning("Could not set favorite, no catalog record.",
                        "catalogId", catalogId, "itemType", itemType);
        } else {
            _itemLogic.setFavorite(member.memberId, itemType, record, favorite);
        }
    }

    // from interface ItemService
    public List<Photo> loadPhotos ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        List<Photo> photos = Lists.newArrayList();
        for (PhotoRecord record : _photoRepo.loadOriginalItems(memrec.memberId, 0)) {
            photos.add((Photo)record.toItem());
        }
        for (PhotoRecord record : _photoRepo.loadClonedItems(memrec.memberId, 0)) {
            photos.add((Photo)record.toItem());
        }
        Collections.sort(photos);
        return photos;
    }

    // from interface ItemService
    public ItemListResult loadItemList (ItemListQuery query) throws ServiceException
    {
        ItemListResult result = new ItemListResult();
        result.items = _itemLogic.loadItemList(query);
        if (query.needsCount) {
            result.totalCount = _itemLogic.getItemListSize(query.listId, query.itemType);
        }
        return result;
    }

    // our dependencies
    @Inject protected ItemManager _itemMan;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected StatLogic _statLogic;
    @Inject protected PhotoRepository _photoRepo;
}
