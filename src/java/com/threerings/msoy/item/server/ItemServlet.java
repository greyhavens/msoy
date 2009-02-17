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

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PhotoRecord;
import com.threerings.msoy.item.server.persist.PhotoRepository;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.TagHistory;
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
    public RatingResult rateItem (ItemIdent iident, byte rating)
        throws ServiceException
    {
        MemberRecord memrec = requireValidatedUser();
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
        if (item.creatorId != memrec.memberId &&
            repo.getRatingRepository().getRating(originalId, memrec.memberId) == 0) {
            _statLogic.incrementStat(memrec.memberId, StatType.ITEMS_RATED, 1);
        }

        // record this player's rating and obtain the new summarized rating
        Tuple<RatingResult, Boolean> result =
            repo.getRatingRepository().rate(originalId, memrec.memberId, rating);

        float newAverage = result.left.getRating();
        int newCount = result.left.ratingSum;
        // The average without counting this rating
        float oldAverage = (newCount > 1) ? (newCount*newAverage - rating)/(newCount - 1) : 0;
        boolean newSolid = (newCount == MIN_SOLID_RATINGS && newAverage >= 4) ||
            (newCount > MIN_SOLID_RATINGS && newAverage >= 4 && (!result.right || oldAverage < 4));

        // If this is a potentially new "solid" rating, update the stat
        if (newSolid) {
            _statLogic.addToSetStat(item.creatorId, StatType.SOLID_4_STAR_RATINGS, originalId);
        }

        return result.left;
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
    public void addFlag (ItemIdent iitem, ItemFlag.Kind kind, String comment)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        _itemLogic.addFlag(memrec.memberId, iitem, kind, comment);
    }

    // from ItemService
    public void removeAllFlags (ItemIdent iitem)
        throws ServiceException
    {
        requireSupportUser();
        _itemFlagRepo.removeItemFlags(iitem.type, iitem.itemId);
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
    public ItemListResult loadItemList (ItemListQuery query)
        throws ServiceException
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
    @Inject protected ItemFlagRepository _itemFlagRepo;

    protected static final int MIN_SOLID_RATINGS = 20;
}
