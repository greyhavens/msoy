//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.BuyResult;
import com.threerings.msoy.money.server.MoneyExchange;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;

import static com.threerings.msoy.Log.log;

/**
 * Contains shared catalog logic.
 */
@Singleton
public class CatalogLogic
{
    /** Used by {@link #loadCatalog} variants. */
    public static class Query
    {
        public MsoyItemType itemType;
        public byte sortBy;
        public String tag;
        public int tagId;
        public String search;
        public int creatorId;
        public int gameId;
        public int themeId;

        public Query (MsoyItemType itemType, byte sortBy) {
            this.itemType = itemType;
            this.sortBy = sortBy;
        }

        public Query (CatalogQuery query, int gameId) {
            this.itemType = query.itemType;
            this.sortBy = query.sortBy;
            this.tag = query.tag;
            this.search = query.search;
            this.creatorId = query.creatorId;
            this.themeId = query.themeGroupId;
            this.gameId = gameId;
        }
    }

    /**
     * Purchase a particular catalog listing at its current listed price.
     *
     * <p> Note: this does not increment any of the stats we keep track of for the Passport. </p>
     */
    public BuyResult<Item> purchaseItem (MemberRecord buyer, MsoyItemType itemType, int catalogId)
        throws ServiceException
    {
        CatalogRecord listing = _itemLogic.requireListing(itemType, catalogId, true);
        return purchaseItem(buyer, itemType, listing, listing.currency, listing.cost, null);
    }

    /**
     * Handles the purchase of a particular catalog listing.
     *
     * <p> Note: this does not increment any of the stats we keep track of for the Passport. </p>
     */
    public BuyResult<Item> purchaseItem (MemberRecord buyer, MsoyItemType itemType, int catalogId,
                                         Currency currency, int authedCost, String memories)
        throws ServiceException
    {
        return purchaseItem(buyer, itemType, _itemLogic.requireListing(itemType, catalogId, true),
                            currency, authedCost, memories);
    }

    /**
     * A simplified method for loading catalog listings. The caller must pass the results to {@link
     * ItemLogic#resolveCardNames} if they want the creator names resolved.
     */
    public List<ListingCard> loadCatalog (MemberRecord mrec, MsoyItemType itemType, byte sortBy, int rows)
        throws ServiceException
    {
        return Lists.transform(loadCatalog(mrec, new Query(itemType, sortBy), 0, rows),
                               CatalogRecord.TO_CARD);
    }

    /**
     * Loads raw catalog listings.
     */
    public List<CatalogRecord> loadCatalog (
        MemberRecord mrec, Query query, int offset, int rows)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(query.itemType);

        // resolve the id of the tag if one is needed
        if (query.tagId == 0 && query.tag != null) {
            query.tagId = repo.getTagRepository().getTagId(query.tag);
            if (query.tagId == 0) {
                // if a tag was requested and it doesn't exist, return no matches
                return Collections.emptyList();
            }
        }

        // build our word search once and share it for loadCatalog() and countListings()
        ItemRepository<ItemRecord>.WordSearch context = repo.buildWordSearch(query.search);

        // If this item type is usable anywhere, ignore the theme
        int themeId = query.itemType.isUsableAnywhere() ? 0 : query.themeId;

        // load the records themselves
        List<CatalogRecord> records = repo.loadCatalog(
            query.sortBy, showMature(mrec), context, query.tagId, query.creatorId, null,
            themeId, query.gameId, offset, rows, _exchange.getRate());

        return records;
    }

    protected BuyResult<Item> purchaseItem (
        final MemberRecord buyer, final MsoyItemType itemType, final CatalogRecord listing,
        Currency currency, int authedCost, String memories)
        throws ServiceException
    {
        // load up the primary listing and its bases
        final List<CatalogRecord> listings = Lists.newArrayList(listing);
        int basisId;
        while ((basisId = listings.get(listings.size() - 1).basisId) > 0) {
            listings.add(_itemLogic.requireListing(itemType, basisId, true));
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

        // prepare the MemoriesRecord- catch errors here
        final MemoriesRecord memrec = (memories == null) ? null : new MemoriesRecord(memories);

        // pass an op to the money services that will create the item on purchase success
        return _moneyLogic.buyItem(buyer, listings, currency, authedCost,
                                   new MoneyLogic.BuyOperation<Item>() {
            public Item create (boolean magicFree, Currency currency, int amountPaid)
                throws ServiceException {
                // create the clone row in the database
                ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
                ItemRecord clone = repo.insertClone(
                    listing.item, buyer.memberId, currency, amountPaid);
                // set up the initial memories, if any
                if (memrec != null) {
                    try {
                        memrec.itemType = clone.getType();
                        memrec.itemId = clone.itemId;
                        _memoryRepo.storeMemories(memrec);
                    } catch (Exception e) {
                        log.warning("Unable to save initial item memories", e);
                        // but cope and continue the purchase
                    }
                }
                // note the new purchase for the item, but only if it wasn't magicFree.
                if (!magicFree) {
                    int newCost = repo.nudgeListing(listing, true);
                    _itemLogic.updateDerivedListings(listing, newCost);
                }
                // make any necessary notifications
                _itemLogic.itemPurchased(clone, currency, amountPaid);
                _eventLog.shopPurchase(buyer.memberId, buyer.visitorId);
                return clone.toItem();
            }
        });
    }

    protected static boolean showMature (MemberRecord mrec)
    {
        return (mrec == null) ? false : mrec.isSet(MemberRecord.Flag.SHOW_MATURE);
    }

    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MoneyExchange _exchange;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyEventLogger _eventLog;
}
