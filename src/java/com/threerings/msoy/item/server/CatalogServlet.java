//
// $Id$

package com.threerings.msoy.item.server;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.persist.TagPopularityRecord;

import com.threerings.msoy.item.data.ItemCodes;
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
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.SubItemRecord;

import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CatalogService}.
 */
public class CatalogServlet extends MsoyServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public ShopData loadShopData (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            ShopData data = new ShopData();

            // load up our top and featured items
            data.topAvatars = loadTopItems(mrec, Item.AVATAR);
            data.topFurniture = loadTopItems(mrec, Item.FURNITURE);
            ListingCard[] pets = loadTopItems(mrec, Item.PET);
            data.featuredPet = (pets.length > 0) ? RandomUtil.pickRandom(pets) : null;
            ListingCard[] toys = loadTopItems(mrec, Item.TOY);
            data.featuredToy = (toys.length > 0) ? RandomUtil.pickRandom(toys) : null;

            // resolve the creator names for these listings
            List<ListingCard> list = Lists.newArrayList();
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

        } catch (PersistenceException pe) {
            log.warning("Failed to load shop data [for=" + ident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public CatalogResult loadCatalog (WebIdent ident, CatalogQuery query, int offset, int rows,
                                      boolean includeCount)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(query.itemType);
        CatalogResult result = new CatalogResult();
        List<ListingCard> list = Lists.newArrayList();

        // if the type in question is not salable, return an empty list
        if (!isSalable(query.itemType)) {
            result.listings = list;
            return result;
        }

        try {
            TagNameRecord tagRecord = (query.tag != null) ?
                repo.getTagRepository().getTag(query.tag) : null;
            int tagId = (tagRecord != null) ? tagRecord.tagId : 0;

            // fetch catalog records and loop over them
            for (CatalogRecord record : repo.loadCatalog(query.sortBy, showMature(mrec),
                                                         query.search, tagId, query.creatorId,
                                                         null, offset, rows)) {
                // convert them to listings
                list.add(record.toListingCard());
            }

            // resolve the creator names for these listings
            _itemLogic.resolveCardNames(list);

            // if they want the total number of matches, compute that as well
            if (includeCount) {
                result.listingCount = repo.countListings(
                    showMature(mrec), query.search, tagId, query.creatorId, null);
            }

        } catch (PersistenceException pe) {
            log.warning("Failed to load catalog [for=" + ident + ", query=" + query +
                    ", offset=" + offset + ", rows=" + rows + "].", pe);
        }
        result.listings = list;
        return result;
    }

    // from interface CatalogService
    public Item purchaseItem (
        WebIdent ident, byte itemType, int catalogId, int authedFlowCost, int authedGoldCost)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(itemType);

        try {
            CatalogRecord listing = repo.loadListing(catalogId, true);
            if (listing == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // double-check against the price that the user thinks they're spending
            if (authedFlowCost < listing.flowCost || authedGoldCost < listing.goldCost) {
                throw new CostUpdatedException(listing.flowCost, listing.goldCost);
            }

            // determine the flow cost for this item
            int flowCost = listing.flowCost;
            if (mrec.isSupport()) {
                // let support+ spend flow they have before they get stuff for free
                flowCost = Math.min(listing.flowCost, mrec.flow);
            }

            // if they don't have flow enough for the item, let them know
            if (mrec.flow < flowCost) {
                throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
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

            // create the clone row in the database
            ItemRecord newClone = repo.insertClone(
                listing.item, mrec.memberId, flowCost, listing.goldCost);

            // note the new purchase for the item
            repo.nudgeListing(catalogId, true);

            if (flowCost > 0) {
                // if the creator of this item is purchasing it, just deduct 70% of the cost
                // instead of taking 100% and paying them 30% and inflating their flowEarned
                int creatorPortion = (3 * listing.flowCost) / 10;
                if (mrec.memberId == listing.item.creatorId) {
                    flowCost -= creatorPortion;
                    creatorPortion = 0;
                }

                // take flow from purchaser
                MemberFlowRecord flowRec = _memberRepo.getFlowRepository().spendFlow(
                    new UserActionDetails(mrec.memberId, UserAction.BOUGHT_ITEM, itemType,
                                          catalogId), flowCost);
                // update member's new flow
                MemberNodeActions.flowUpdated(flowRec);

                // give 30% of it to the creator
                if (creatorPortion > 0) {
                    // TODO: hold this in escrow
                    flowRec = _memberRepo.getFlowRepository().grantFlow(
                        new UserActionDetails(listing.item.creatorId, UserAction.RECEIVED_PAYOUT,
                                              mrec.memberId, itemType, catalogId),
                        creatorPortion);
                    MemberNodeActions.flowUpdated(flowRec);
                }
            }

            // update player stats if the seller wasn't also the purchaser
            if (mrec.memberId != listing.item.creatorId) {
                _statLogic.incrementStat(mrec.memberId, StatType.ITEMS_PURCHASED, 1);
                _statLogic.incrementStat(listing.item.creatorId, StatType.ITEMS_SOLD, 1);
            }

            // update their runtime inventory as appropriate
            final Item nitem = newClone.toItem();
            postDObjectAction(new Runnable() {
                public void run () {
                    _itemMan.itemPurchased(nitem);
                }
            });

            return nitem;

        } catch (PersistenceException pe) {
            log.warning("Purchase failed [type=" + itemType + ", catId=" + catalogId + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public int listItem (WebIdent ident, ItemIdent item, String descrip, int pricing,
                         int salesTarget, int flowCost, int goldCost)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(item.type);
        try {
            // load a copy of the original item
            ItemRecord originalItem = repo.loadOriginalItem(item.itemId);
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

            // compute the listing cost and check that they have it
            int price = getCheckListingPrice(mrec, true);

            // we will modify the original item (it's a clone, no need to worry) to create the new
            // catalog listing prototype item
            int originalItemId = originalItem.itemId;
            ItemRecord listItem = originalItem;
            listItem.prepareForListing(null);

            // if this item has a suite id (it's part of another item's suite), we need to
            // configure its listed suite as the catalog id of the suite master item
            if (originalItem instanceof SubItemRecord) {
                SubItem sitem = (SubItem)originalItem.toItem();
                ItemRepository<ItemRecord> mrepo = _itemMan.getRepository(sitem.getSuiteMasterType());
                ItemRecord suiteMaster = mrepo.loadOriginalItem(
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
            long now = System.currentTimeMillis();
            repo.getTagRepository().copyTags(originalItemId, listItem.itemId, mrec.memberId, now);

            // sanitize the sales target
            salesTarget = Math.max(salesTarget, CatalogListing.MIN_SALES_TARGET);

            // create & insert the catalog record
            CatalogRecord record = repo.insertListing(
                listItem, originalItemId, pricing, salesTarget, flowCost, goldCost, now);

            // record the listing action and charge the flow
            UserActionDetails info = new UserActionDetails(
                mrec.memberId, UserAction.LISTED_ITEM, repo.getItemType(), originalItemId);
            if (price > 0) {
                MemberFlowRecord flowRec = _memberRepo.getFlowRepository().spendFlow(info, price);
                MemberNodeActions.flowUpdated(flowRec);
            } else {
                logUserAction(info);
            }

            // publish to the member's feed if it's not hidden
            if (pricing != CatalogListing.PRICING_HIDDEN) {
                _feedRepo.publishMemberMessage(mrec.memberId,
                    FeedMessageType.FRIEND_LISTED_ITEM, listItem.name + "\t" +
                    String.valueOf(repo.getItemType()) + "\t" + String.valueOf(record.catalogId) +
                    "\t" + MediaDesc.mdToString(listItem.getThumbMediaDesc()));
            }

            // update player stats
            _statLogic.incrementStat(mrec.memberId, StatType.ITEMS_LISTED, 1);

            return record.catalogId;

        } catch (PersistenceException pe) {
            log.warning("List item failed [item=" + item + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogServlet
    public CatalogListing loadListing (WebIdent ident, byte itemType, int catalogId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(itemType);
        try {
            // load up the old catalog record
            CatalogRecord record = repo.loadListing(catalogId, true);
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

            // finally convert the listing to a runtime record
            CatalogListing clrec = record.toListing();
            clrec.detail.creator = _memberRepo.loadMemberName(record.item.creatorId);
            if (mrec != null) {
                clrec.detail.memberItemInfo.memberRating =
                    repo.getRating(record.item.itemId, mrec.memberId);
                clrec.detail.memberItemInfo.favorite =
                    _itemMan.isFavorite(mrec.memberId, record.item.toItem());
            }
            return clrec;

        } catch (PersistenceException pe) {
            log.warning("Load listing failed [type=" + itemType +
                ", catId=" + catalogId + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public void updateListing (WebIdent ident, ItemIdent item, String descrip)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(item.type);
        try {
            // load a copy of the original item
            ItemRecord originalItem = repo.loadOriginalItem(item.itemId);
            if (originalItem == null) {
                log.warning("Can't find item for listing update [item= " + item + "]");
                throw new ServiceException(ItemCodes.INTERNAL_ERROR);
            }

            // make sure we own this item
            requireIsUser(mrec, originalItem.ownerId, "updateListing", originalItem);

            // load up the old catalog record
            CatalogRecord record = repo.loadListing(originalItem.catalogId, false);
            if (record == null) {
                log.warning("Missing listing for update [who=" + mrec.who() + ", item=" + item +
                            ", catId=" + originalItem.catalogId + "].");
                throw new ServiceException(ItemCodes.INTERNAL_ERROR);
            }

            // load up the old list item
            ItemRecord oldListItem = repo.loadItem(record.listedItemId);

            // compute the listing update cost and check that they have it
            int price = getCheckListingPrice(mrec, false);

            // we will modify the original item (it's a clone, no need to worry) to create the new
            // catalog listing prototype item
            int originalItemId = originalItem.itemId;
            final ItemRecord listItem = originalItem;
            listItem.prepareForListing(oldListItem);

            // use the updated description (the client should prevent this from being too long, but
            // we'll trim the description rather than fail the insert if something is haywire)
            listItem.description = StringUtil.truncate(descrip, Item.MAX_DESCRIPTION_LENGTH);

            // update our catalog prototype item
            repo.updateOriginalItem(listItem);

            // record the listing action
            UserActionDetails info = new UserActionDetails(
                mrec.memberId, UserAction.UPDATED_LISTING, repo.getItemType(), originalItemId);

            if (price > 0) {
                MemberFlowRecord flowRec = _memberRepo.getFlowRepository().spendFlow(info, price);
                MemberNodeActions.flowUpdated(flowRec);
            } else {
                logUserAction(info);
            }

            // kick off a notification that the list item was updated to e.g. reload game lobbies
            postDObjectAction(new Runnable() {
                public void run () {
                    _itemMan.itemUpdated(listItem);
                }
            });

        } catch (PersistenceException pe) {
            log.warning("List item failed [item=" + item + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public void updatePricing (WebIdent ident, byte itemType, int catalogId, int pricing,
                               int salesTarget, int flowCost, int goldCost)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(itemType);
        try {
            // load up the listing we're updating
            CatalogRecord record = repo.loadListing(catalogId, false);
            if (record == null) {
                log.warning("Missing listing for update [who=" + mrec.who() + ", type=" + itemType +
                            ", catId=" + catalogId + "].");
                throw new ServiceException(ItemCodes.INTERNAL_ERROR);
            }

            // load a copy of the original item
            ItemRecord originalItem = repo.loadOriginalItem(record.originalItemId);
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
            UserActionDetails info = new UserActionDetails(
                mrec.memberId, UserAction.UPDATED_PRICING, itemType, catalogId);
            logUserAction(info);

        } catch (PersistenceException pe) {
            log.warning("Update pricing failed [type=" + itemType +
                    ", catId=" + catalogId + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public void removeListing (WebIdent ident, byte itemType, int catalogId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(itemType);
        try {
            CatalogRecord listing = repo.loadListing(catalogId, true);
            if (listing == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // make sure we're the creator of the listed item
            requireIsUser(mrec, listing.item.creatorId, "removeListing", listing.item);

            // go ahead and remove the user
            repo.removeListing(listing);

        } catch (PersistenceException pe) {
            log.warning("Remove listing failed [type=" + itemType +
                ", catId=" + catalogId + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public int[] returnItem (WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        final MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(iident.type);

        try {
            CloneRecord cRec = repo.loadCloneRecord(iident.itemId);
            if (cRec == null) {
                log.warning("Failed to find clone record [item=" + iident + "]");
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }

            // note the return for the catalog listing
            ItemRecord item = repo.loadOriginalItem(cRec.originalItemId);
            if (item == null) {
                log.warning("Failed to find prototype record [id=" + cRec.originalItemId + "]");
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
            repo.nudgeListing(item.catalogId, false);

            long mSec = System.currentTimeMillis() - cRec.purchaseTime.getTime();
            int daysLeft = 5 - ((int) (mSec / (24 * 3600 * 1000)));
            if (daysLeft < 1 || cRec.flowPaid == 0) {
                return new int[] { 0, 0 };
            }

            int flowRefund = (cRec.flowPaid * daysLeft) / 5;
            int goldRefund = (0 /* TODO */ * daysLeft) / 5;

            UserActionDetails returnInfo = new UserActionDetails(
                mrec.memberId, UserAction.RETURNED_ITEM, iident.type, iident.itemId);
            MemberFlowRecord flowRec =
                _memberRepo.getFlowRepository().refundFlow(returnInfo, flowRefund);
            MemberNodeActions.flowUpdated(flowRec);

            // now we have to take 30% of the refund away from the creator
            // TODO: when escrow works, this will blessedly go away
            int creatorPortion = (3 * flowRefund) / 10;
            UserActionDetails payoutInfo =
                new UserActionDetails(item.creatorId, UserAction.RECEIVED_PAYOUT,
                        mrec.memberId, iident.type, iident.itemId);
            flowRec = _memberRepo.getFlowRepository().spendFlow(payoutInfo, creatorPortion);
            MemberNodeActions.flowUpdated(flowRec);

            return new int[] { flowRefund, goldRefund };

        } catch (PersistenceException pe) {
            log.warning("Purchase failed [item=" + iident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public Map<String, Integer> getPopularTags (byte type, int rows)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(type);
        Map<String, Integer> result = Maps.newHashMap();
        try {
            for (TagPopularityRecord record : repo.getTagRepository().getPopularTags(rows)) {
                result.put(record.tag, record.count);
            }
        } catch (PersistenceException pe) {
            log.warning("Load popular tags failed [type=" + type + "].", pe);
        }
        return result;
    }

    /**
     * Helper function for {@link #loadShopData}.
     */
    protected ListingCard[] loadTopItems (MemberRecord mrec, byte type)
        throws PersistenceException, ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(type);
        List<ListingCard> cards = Lists.newArrayList();
        for (CatalogRecord crec : repo.loadCatalog(CatalogQuery.SORT_BY_RATING, showMature(mrec),
                null, 0, 0, null, 0, ShopData.TOP_ITEM_COUNT)) {
            cards.add(crec.toListingCard());
        }
        return cards.toArray(new ListingCard[cards.size()]);
    }

    /**
     * Helper function for {@link #listItem}.
     */
    protected int getCheckListingPrice (MemberRecord mrec, boolean newListing)
        throws ServiceException
    {
        // TEMP: admins don't have to pay to list; when this gets factored into
        // FinancialAction then admins/support can not pay for anything
        if (mrec.isAdmin()) {
            return 0;
        }

        // TODO: fixed price for first time listing and another for updating?
        int price = 0;

        if (mrec.flow < price) {
            throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
        }
        return price;
    }

    /**
     * Returns true if the specified item type is salable, false if not.
     */
    protected boolean isSalable (byte itemType)
        throws ServiceException
    {
        try {
            Item item = (Item)Item.getClassForType(itemType).newInstance();
            return (!(item instanceof SubItem) || ((SubItem)item).isSalable());
        } catch (Exception e) {
            log.warning("Failed to check salability [type=" + itemType + "].", e);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Ensures that the specified user or a support user is taking the requested action.
     */
    protected void requireIsUser (MemberRecord mrec, int targetId, String action, ItemRecord item)
        throws ServiceException
    {
        if (mrec == null || (mrec.memberId != targetId && !mrec.isSupport())) {
            String who = (mrec == null ? "null" : mrec.who());
            log.warning("Access denied for catalog action [who=" + who + ", wanted=" + targetId +
                        ", action=" + action + ", item=" + item + "].");
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }
    }

    protected boolean showMature (MemberRecord mrec)
    {
        return (mrec == null) ? false : mrec.isSet(MemberRecord.Flag.SHOW_MATURE);
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ItemManager _itemMan;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected StatLogic _statLogic;
}
