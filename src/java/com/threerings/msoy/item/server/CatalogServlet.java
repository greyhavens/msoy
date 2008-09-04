//
// $Id$

package com.threerings.msoy.item.server;

import static com.threerings.msoy.Log.log;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagPopularityRecord;
import com.threerings.msoy.server.persist.UserActionRepository;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CostUpdatedException;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.ShopData;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.SubItemRecord;

import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.money.server.MoneyNodeActions;
import com.threerings.msoy.money.server.MoneyResult;
import com.threerings.msoy.money.server.NotEnoughMoneyException;
import com.threerings.msoy.money.server.NotSecuredException;

/**
 * Provides the server implementation of {@link CatalogService}.
 */
public class CatalogServlet extends MsoyServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public ShopData loadShopData ()
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser();
        final ShopData data = new ShopData();

        // load up our top and featured items
        data.topAvatars = loadTopItems(mrec, Item.AVATAR);
        data.topFurniture = loadTopItems(mrec, Item.FURNITURE);
        final ListingCard[] pets = loadTopItems(mrec, Item.PET);
        data.featuredPet = (pets.length > 0) ? RandomUtil.pickRandom(pets) : null;
        final ListingCard[] toys = loadTopItems(mrec, Item.TOY);
        data.featuredToy = (toys.length > 0) ? RandomUtil.pickRandom(toys) : null;

        // resolve the creator names for these listings
        final List<ListingCard> list = Lists.newArrayList();
        CollectionUtil.addAll(list, data.topAvatars);
        CollectionUtil.addAll(list, data.topFurniture);
        if (data.featuredPet != null) {
            list.add(data.featuredPet);
        }
        if (data.featuredToy != null) {
            list.add(data.featuredToy);
        }
        _itemLogic.resolveCardNames(list);

        return data;
    }

    // from interface CatalogService
    public CatalogResult loadCatalog (final CatalogQuery query, final int offset, final int rows,
        final boolean includeCount)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser();
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(query.itemType);
        final CatalogResult result = new CatalogResult();
        final List<ListingCard> list = Lists.newArrayList();

        // if the type in question is not salable, return an empty list
        if (!isSalable(query.itemType)) {
            result.listings = list;
            return result;
        }

        final TagNameRecord tagRecord = (query.tag != null) ?
            repo.getTagRepository().getTag(query.tag) : null;
        final int tagId = (tagRecord != null) ? tagRecord.tagId : 0;

        // fetch catalog records and loop over them
        list.addAll(Lists.transform(
                        repo.loadCatalog(query.sortBy, showMature(mrec), query.search, tagId,
                                         query.creatorId, null, offset, rows),
                        CatalogRecord.TO_CARD));

        // resolve the creator names for these listings
        _itemLogic.resolveCardNames(list);

        // if they want the total number of matches, compute that as well
        if (includeCount) {
            result.listingCount = repo.countListings(
                showMature(mrec), query.search, tagId, query.creatorId, null);
        }
        result.listings = list;
        return result;
    }

    // from interface CatalogService
    public Item purchaseItem (final byte itemType, final int catalogId, final int authedFlowCost,
        final int authedGoldCost)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);

        final CatalogRecord listing = repo.loadListing(catalogId, true);
        if (listing == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // make sure we haven't hit our limited edition count
        if (listing.pricing == CatalogListing.PRICING_LIMITED_EDITION &&
            listing.purchases >= listing.salesTarget) {
            throw new ServiceException(ItemCodes.E_HIT_SALES_LIMIT);
        }

        // make sure they're not seeing a stale record for a hidden item
        if (listing.pricing == CatalogListing.PRICING_HIDDEN) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // Update money as appropriate.
        MoneyResult result;
        try {
            result = _moneyLogic.buyItem(mrec, new CatalogIdent(itemType, catalogId),
                                         Currency.COINS, listing.flowCost, Currency.COINS, authedFlowCost);
        } catch (final NotEnoughMoneyException neme) {
            throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
        } catch (final NotSecuredException nse) {
            throw new CostUpdatedException(listing.flowCost, listing.goldCost);
        }

        // create the clone row in the database
        final ItemRecord newClone = repo.insertClone(
            listing.item, mrec.memberId, (int)result.getMemberTransaction().getAmount(),
            listing.goldCost);

        // note the new purchase for the item
        repo.nudgeListing(catalogId, true);

        _moneyNodeActions.moneyUpdated(result.getNewMemberMoney());
        if (result.getNewCreatorMoney() != null) {
            _moneyNodeActions.moneyUpdated(result.getNewCreatorMoney());

            final int creatorId = listing.item.creatorId;
            final int creatorAmount = (int)result.getCreatorTransaction().getAmount();
            if (mrec.memberId != creatorId && creatorAmount > 0) {
                _statLogic.incrementStat(
                    creatorId, StatType.COINS_EARNED_SELLING, creatorAmount);

                // Some items have a stat that may need updating
                if (itemType == Item.AVATAR) {
                    _statLogic.ensureIntStatMinimum(
                        creatorId, StatType.AVATARS_CREATED, StatType.ITEM_SOLD);
                } else if (itemType == Item.FURNITURE) {
                    _statLogic.ensureIntStatMinimum(
                        creatorId, StatType.FURNITURE_CREATED, StatType.ITEM_SOLD);
                } else if (itemType == Item.DECOR) {
                    _statLogic.ensureIntStatMinimum(
                        creatorId, StatType.BACKDROPS_CREATED, StatType.ITEM_SOLD);
                }
            }
        }

        // update their runtime inventory as appropriate
        final Item nitem = newClone.toItem();
        postDObjectAction(new Runnable() {
            public void run () {
                _itemMan.itemPurchased(nitem);
            }
        });

        // update their stat set, if they aren't buying something from themselves.
        final MoneyHistory transaction = result.getMemberTransaction();
        if (mrec.memberId != listing.item.creatorId &&
            transaction.getCurrency() == Currency.COINS) {
            _statLogic.incrementStat(
                mrec.memberId, StatType.COINS_SPENT, (int)transaction.getAmount());
        }

        return nitem;
    }

    // from interface CatalogService
    public int listItem (final ItemIdent item, final String descrip, final int pricing,
        int salesTarget, final int flowCost, final int goldCost)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        // load a copy of the original item
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.type);
        final ItemRecord originalItem = repo.loadOriginalItem(item.itemId);
        if (originalItem == null) {
            log.warning("Can't find item to list [item= " + item + "]");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // make sure we own AND created this item
        requireIsUser(mrec, originalItem.ownerId, "listItem", originalItem);
        requireIsUser(mrec, originalItem.creatorId, "listItem", originalItem);

        // make sure this item is not already listed
        if (originalItem.catalogId != 0) {
            log.warning("Requested to list already listed item [who=" + mrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // we will modify the original item (it's a clone, no need to worry) to create the new
        // catalog listing prototype item
        final int originalItemId = originalItem.itemId;
        final ItemRecord listItem = originalItem;
        listItem.prepareForListing(null);

        // if this item has a suite id (it's part of another item's suite), we need to
        // configure its listed suite as the catalog id of the suite master item
        if (originalItem instanceof SubItemRecord) {
            final SubItem sitem = (SubItem)originalItem.toItem();
            final ItemRepository<ItemRecord> mrepo =
                _itemLogic.getRepository(sitem.getSuiteMasterType());
            final ItemRecord suiteMaster = mrepo.loadOriginalItem(
                ((SubItemRecord)originalItem).suiteId);
            if (suiteMaster == null) {
                log.warning("Failed to locate suite master item [item=" + item + "].");
                throw new ServiceException(ItemCodes.INTERNAL_ERROR);
            }
            if (suiteMaster.catalogId == 0) {
                throw new ServiceException(ItemCodes.SUPER_ITEM_NOT_LISTED);
            }
            ((SubItemRecord)listItem).suiteId = -suiteMaster.catalogId;
        }

        // use the updated description (the client should prevent this from being too long, but
        // we'll trim the description rather than fail the insert if something is haywire)
        listItem.description = StringUtil.truncate(descrip, Item.MAX_DESCRIPTION_LENGTH);

        // create our new immutable catalog prototype item
        repo.insertOriginalItem(listItem, true);

        // copy tags from the original item to the new listing item
        final long now = System.currentTimeMillis();
        repo.getTagRepository().copyTags(originalItemId, listItem.itemId, mrec.memberId, now);

        // sanitize the sales target
        salesTarget = Math.max(salesTarget, CatalogListing.MIN_SALES_TARGET);

        // create & insert the catalog record
        final CatalogRecord record = repo.insertListing(
            listItem, originalItemId, pricing, salesTarget, flowCost, goldCost, now);

        // record the listing action and charge the flow
        final UserActionDetails info = new UserActionDetails(
            mrec.memberId, UserAction.LISTED_ITEM, repo.getItemType(), originalItemId);
        _userActionRepo.logUserAction(info);

        // publish to the member's feed if it's not hidden
        if (pricing != CatalogListing.PRICING_HIDDEN) {
            _feedRepo.publishMemberMessage(
                mrec.memberId, FeedMessageType.FRIEND_LISTED_ITEM, listItem.name + "\t" +
                String.valueOf(repo.getItemType()) + "\t" + String.valueOf(record.catalogId) +
                "\t" + MediaDesc.mdToString(listItem.getThumbMediaDesc()));
        }

        // some items are related to a stat that may need updating.  Use originalItem.creatorId
        // so that agents and admins don't get credit for listing someone elses stuff.
        if (item.type == Item.AVATAR) {
            _statLogic.ensureIntStatMinimum(
                originalItem.creatorId, StatType.AVATARS_CREATED, StatType.ITEM_LISTED);
        } else if (item.type == Item.FURNITURE) {
            _statLogic.ensureIntStatMinimum(
                originalItem.creatorId, StatType.FURNITURE_CREATED, StatType.ITEM_LISTED);
        } else if (item.type == Item.DECOR) {
            _statLogic.ensureIntStatMinimum(
                originalItem.creatorId, StatType.BACKDROPS_CREATED, StatType.ITEM_LISTED);
        }

        return record.catalogId;
    }

    // from interface CatalogServlet
    public CatalogListing loadListing (final byte itemType, final int catalogId)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser();

        // load up the old catalog record
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
        final CatalogRecord record = repo.loadListing(catalogId, true);
        if (record == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // if we're not the creator of the listing (who has to download it to update it) do
        // some access control checks
        if (mrec == null || (record.item.creatorId != mrec.memberId && !mrec.isAdmin())) {
            // if the type in question is not salable, reject the request
            if (!isSalable(itemType)) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // if this listing is not meant for general sale, no lookey
            if (record.pricing == CatalogListing.PRICING_HIDDEN) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }
        }

        // Secure the current price of the item for this member.
        if (mrec != null) {
            _moneyLogic.securePrice(
                mrec.memberId, new CatalogIdent(itemType, catalogId), Currency.COINS,
                record.flowCost, record.item.creatorId, 0, record.item.name);
        }

        // finally convert the listing to a runtime record
        final CatalogListing clrec = record.toListing();
        clrec.detail.creator = _memberRepo.loadMemberName(record.item.creatorId);
        clrec.detail.memberItemInfo = _itemLogic.getMemberItemInfo(mrec, record.item.toItem());
        return clrec;
    }

    // from interface CatalogService
    public void updateListing (final ItemIdent item, final String descrip)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        // load a copy of the original item
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.type);
        final ItemRecord originalItem = repo.loadOriginalItem(item.itemId);
        if (originalItem == null) {
            log.warning("Can't find item for listing update [item= " + item + "]");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // make sure we own this item
        requireIsUser(mrec, originalItem.ownerId, "updateListing", originalItem);

        // load up the old catalog record
        final CatalogRecord record = repo.loadListing(originalItem.catalogId, false);
        if (record == null) {
            log.warning("Missing listing for update [who=" + mrec.who() + ", item=" + item +
                        ", catId=" + originalItem.catalogId + "].");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // load up the old list item
        final ItemRecord oldListItem = repo.loadItem(record.listedItemId);

        // we will modify the original item (it's a clone, no need to worry) to create the new
        // catalog listing prototype item
        final int originalItemId = originalItem.itemId;
        final ItemRecord listItem = originalItem;
        listItem.prepareForListing(oldListItem);

        // use the updated description (the client should prevent this from being too long, but
        // we'll trim the description rather than fail the insert if something is haywire)
        listItem.description = StringUtil.truncate(descrip, Item.MAX_DESCRIPTION_LENGTH);

        // update our catalog prototype item
        repo.updateOriginalItem(listItem);

        // record the listing action
        final UserActionDetails info = new UserActionDetails(
            mrec.memberId, UserAction.UPDATED_LISTING, repo.getItemType(), originalItemId);
        _userActionRepo.logUserAction(info);

        // kick off a notification that the list item was updated to e.g. reload game lobbies
        postDObjectAction(new Runnable() {
            public void run () {
                _itemMan.itemUpdated(listItem);
            }
        });
    }

    // from interface CatalogService
    public void updatePricing (final byte itemType, final int catalogId, final int pricing,
        int salesTarget, final int flowCost, final int goldCost)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        // load up the listing we're updating
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
        final CatalogRecord record = repo.loadListing(catalogId, false);
        if (record == null) {
            log.warning("Missing listing for update [who=" + mrec.who() + ", type=" + itemType +
                        ", catId=" + catalogId + "].");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // load a copy of the original item
        final ItemRecord originalItem = repo.loadOriginalItem(record.originalItemId);
        if (originalItem == null) {
            log.warning("Can't find original for pricing update [who=" + mrec.who() +
                        ", type=" + itemType + ", catId=" + catalogId +
                        ", itemId=" + record.originalItemId + "]");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // make sure we own this item
        requireIsUser(mrec, originalItem.ownerId, "updatePricing", originalItem);

        // sanitize the sales target
        salesTarget = Math.max(salesTarget, CatalogListing.MIN_SALES_TARGET);

        // now we can update the record
        repo.updatePricing(
            catalogId, pricing, salesTarget, flowCost, goldCost, System.currentTimeMillis());

        // record the update action
        final UserActionDetails info = new UserActionDetails(
            mrec.memberId, UserAction.UPDATED_PRICING, itemType, catalogId);
        _userActionRepo.logUserAction(info);
    }

    // from interface CatalogService
    public void removeListing (final byte itemType, final int catalogId)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        // load up the listing to be removed
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
        final CatalogRecord listing = repo.loadListing(catalogId, true);
        if (listing == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // make sure we're the creator of the listed item
        requireIsUser(mrec, listing.item.creatorId, "removeListing", listing.item);

        // go ahead and remove the user
        repo.removeListing(listing);
    }

    // from interface CatalogService
    public Map<String, Integer> getPopularTags (final byte type, final int rows)
        throws ServiceException
    {
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        final Map<String, Integer> result = Maps.newHashMap();
        for (final TagPopularityRecord record : repo.getTagRepository().getPopularTags(rows)) {
            result.put(record.tag, record.count);
        }
        return result;
    }

    // from interface CatalogService
    public FavoritesResult loadFavorites (final int memberId, final byte itemType)
        throws ServiceException
    {
        final FavoritesResult result = new FavoritesResult();
        // look up the party in question, if they don't exist, return null
        result.noter = _memberRepo.loadMemberName(memberId);
        if (result.noter == null) {
            return null;
        }
        result.favorites = _itemLogic.resolveFavorites(
            _faveRepo.loadFavorites(memberId, itemType));
        return result;
    }

    /**
     * Helper function for {@link #loadShopData}.
     */
    protected ListingCard[] loadTopItems (final MemberRecord mrec, final byte type)
        throws ServiceException
    {
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        final List<ListingCard> cards = Lists.newArrayList();
        for (final CatalogRecord crec : repo.loadCatalog(CatalogQuery.SORT_BY_RATING,
            showMature(mrec), null, 0, 0, null, 0, ShopData.TOP_ITEM_COUNT)) {
            cards.add(crec.toListingCard());
        }
        return cards.toArray(new ListingCard[cards.size()]);
    }

    /**
     * Returns true if the specified item type is salable, false if not.
     */
    protected boolean isSalable (final byte itemType)
        throws ServiceException
    {
        try {
            final Item item = Item.getClassForType(itemType).newInstance();
            return (!(item instanceof SubItem) || ((SubItem)item).isSalable());
        } catch (final Exception e) {
            log.warning("Failed to check salability [type=" + itemType + "].", e);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Ensures that the specified user or a support user is taking the requested action.
     */
    protected void requireIsUser (final MemberRecord mrec, final int targetId, final String action,
        final ItemRecord item)
        throws ServiceException
    {
        if (mrec == null || (mrec.memberId != targetId && !mrec.isSupport())) {
            final String who = (mrec == null ? "null" : mrec.who());
            log.warning("Access denied for catalog action [who=" + who + ", wanted=" + targetId +
                        ", action=" + action + ", item=" + item + "].");
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }
    }

    protected boolean showMature (final MemberRecord mrec)
    {
        return (mrec == null) ? false : mrec.isSet(MemberRecord.Flag.SHOW_MATURE);
    }

    // our dependencies
    @Inject protected ItemManager _itemMan;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected FavoritesRepository _faveRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyNodeActions _moneyNodeActions;
    @Inject protected UserActionRepository _userActionRepo;
    @Inject protected StatLogic _statLogic;
}
