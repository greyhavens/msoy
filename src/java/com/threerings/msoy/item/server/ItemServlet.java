//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.Tuple;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.RatingHistoryResult;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.server.ThemeLogic;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeAvatarLineupRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.server.RatingLogic;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.TagLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRepository;
import com.threerings.msoy.underwire.server.SupportLogic;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceCodes;
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

        // if they are the creator of the item, they're not allowed to re-rate it (otherwise they
        // could circumvent the minimum listing fee)
        byte oldRating = repo.getRatingRepository().getRating(originalId, memrec.memberId);
        if (item.creatorId == memrec.memberId && oldRating != 0) {
            throw new ServiceException(ItemCodes.E_NO_RERATE_OWN_ITEM);
        }

        // if this is the first time the player has rated this item, increment items rated stat
        if (item.creatorId != memrec.memberId && oldRating == 0) {
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

        // If this is a potentially new "solid" rating and the item is listed, update the stat
        if (newSolid && item.catalogId != 0) {
            _statLogic.addToSetStat(item.creatorId, StatType.SOLID_4_STAR_RATINGS, originalId);
        }

        return result.left;
    }

    // from interface ItemService
    public RatingHistoryResult getRatingHistory (final ItemIdent iident)
        throws ServiceException
    {
        RatingRepository ratingRepo = _itemLogic.getRepository(iident.type).getRatingRepository();
        return _ratingLogic.getRatingHistory(iident.itemId, ratingRepo, 0, 0);
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
        TagRepository tagRepo = _itemLogic.getRepository(iident.type).getTagRepository();
        return _tagLogic.getTagHistory(iident.itemId, tagRepo, 0, 0);
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

        if (memrec.isTroublemaker() && memrec.memberId != item.creatorId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // map tag to tag id
        TagNameRecord tag = repo.getTagRepository().getOrCreateTag(tagName);
        int originalId = (item.sourceId != 0) ? item.sourceId : iident.itemId;

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
    public void setFavorite (MsoyItemType itemType, int catalogId, boolean favorite)
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

    // from interface ItemService
    public GroupName[] loadLineups (int catalogId)
        throws ServiceException
    {
        Set<Integer> groupIds = Sets.newHashSet();
        for (ThemeAvatarLineupRecord rec : _themeRepo.loadLineups(catalogId)) {
            groupIds.add(rec.groupId);
        }
        return _groupRepo.loadGroupNames(groupIds).values().toArray(new GroupName[0]);
    }

    // from interface ItemService
    public void stampItem (ItemIdent ident, int groupId, boolean doStamp)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if (!_themeLogic.isTheme(groupId) ||
                _groupRepo.getMembership(groupId, memrec.memberId).left != Rank.MANAGER) {
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(ident.type);
        ItemRecord rec = repo.loadItem(ident.itemId);
        if (rec == null) {
            log.warning("Couldn't find item to stamp", "item", ident, "stamper", memrec.who());
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        int stampItemId = (rec.sourceId != 0) ? rec.sourceId : rec.itemId;

        if (doStamp) {
            if (!repo.stampItem(stampItemId, groupId, memrec.memberId)) {
                log.warning("Item was already stamped!", "item", ident, "stampItemId", stampItemId,
                    "theme", groupId);
            }

        } else {
            if (ident.type == MsoyItemType.AVATAR) {
                // make sure this is not the template item for a lineup avatar listing
                List<CatalogRecord> catRecs =
                    repo.loadCatalogByListedItems(ImmutableList.of(stampItemId), false);
                if (!catRecs.isEmpty() &&
                        _themeRepo.isAvatarInLineup(groupId, catRecs.get(0).catalogId)) {
                    log.warning("Tried to unstamp a lineup avatar", "item", ident, "theme", groupId);
                    throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
                }
            }

            if (!repo.unstampItem(stampItemId, groupId)) {
                log.warning("Item was not stamped!", "item", ident, "stampItemId", stampItemId,
                    "theme", groupId);
            }
        }
    }

    // from interface ItemService
    public void setAvatarInLineup (int catalogId, int groupId, boolean doAdd)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if (_groupRepo.getMembership(groupId, memrec.memberId).left != Rank.MANAGER) {
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }
        if (doAdd) {
            CatalogRecord catRec = _avaRepo.loadListing(catalogId, false);

            // this is not tested for in the GWT page
            if (catRec.basisId != 0) {
                throw new ServiceException(ItemCodes.E_CANT_LINEUP_DERIVED);
            }

            // sanity check: the item must be branded by the theme group
            if (catRec.brandId != groupId) {
                log.warning("Intended lineup listing not branded by theme", "catalogId", catalogId,
                    "theme", "groupId");
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // sanity check: the item must be stamped!
            if (!_avaRepo.isThemeStamped(groupId, catRec.listedItemId)) {
                log.warning("Intended lineup listing not stamped", "catalogId", catalogId,
                    "theme", "groupId");
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            if (!_themeRepo.setAvatarInLineup(groupId, catalogId)) {
                log.warning("Avatar was already in lineup!", "avatar", catalogId, "theme", groupId);
            }

        } else {
            if (!_themeRepo.removeAvatarFromLineup(groupId, catalogId)) {
                log.warning("Avatar was not in lineup!", "avatar", catalogId, "theme", groupId);
            }
        }
    }

    @Override // from ItemService
    public void complainTag (ItemIdent iident, String tag, String reason)
        throws ServiceException
    {
        MemberRecord complainer = requireAuthedUser();
        TagRepository tagRepo = _itemLogic.getRepository(iident.type).getTagRepository();

        int tagId = tagRepo.getTagId(tag);
        if (tagId == 0) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRecord item = _itemLogic.getRepository(iident.type).loadItem(iident.itemId);
        if (item == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        TagHistoryRecord hist = tagRepo.getLastAddition(iident.itemId, tagId);
        StringBuilder message = new StringBuilder("[");
        int targetId;
        if (hist == null) {
            message.append("tag not added to listed item ");
            targetId = item.creatorId;
        } else {
            message.append("tag added to listed item at ").append(hist.time);
            targetId = hist.memberId;
        }
        message.append('"').append(item.name).append('"');
        message.append("]\n").append("Tag: ").append(tag);

        _supportLogic.addMessageComplaint(complainer.getName(), targetId, message.toString(),
            reason, Pages.STUFF.makeURL("d", iident.type, iident.itemId));
    }

    // our dependencies
    @Inject protected AvatarRepository _avaRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemFlagRepository _itemFlagRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ItemManager _itemMan;
    @Inject protected RatingLogic _ratingLogic;
    @Inject protected StatLogic _statLogic;
    @Inject protected SupportLogic _supportLogic;
    @Inject protected TagLogic _tagLogic;
    @Inject protected ThemeLogic _themeLogic;
    @Inject protected ThemeRepository _themeRepo;

    protected static final int MIN_SOLID_RATINGS = 20;
}
