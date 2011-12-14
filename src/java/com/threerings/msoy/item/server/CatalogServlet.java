//
// $Id$

package com.threerings.msoy.item.server;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.threerings.util.MessageBundle;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.group.gwt.BrandDetail;
import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.ThemeLogic;
import com.threerings.msoy.group.server.persist.BrandShareRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.GameItem;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.ItemPrices;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.FavoritesRepository.FavoritedItemResultRecord;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.server.BuyResult;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagPopularityRecord;
import com.threerings.msoy.server.persist.UserActionRepository;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CatalogService}.
 */
public class CatalogServlet extends MsoyServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public CatalogResult loadJumble (int themeId, int offset, int rows)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        // if the caller does not explicitly request a theme, use the player's current one (if any)
        if (themeId == 0 && mrec != null) {
            themeId = mrec.themeGroupId;
        }
        List<ListingCard> items;
        GroupName theme;

        if (themeId != 0) {
            items = _itemLogic.getThemedJumble(themeId);
            theme = _groupRepo.loadGroupName(themeId);
        } else {
            items = _itemLogic.getJumbleSnapshot();
            theme = null;
        }

        _itemLogic.resolveCardNames(items);

        items = Lists.newArrayList(items.subList(
            Math.min(items.size(), offset), Math.min(items.size(), offset + rows)));
        return new CatalogResult(items, theme);
    }

    // from interface CatalogService
    public CatalogResult loadCatalog (CatalogQuery query, int offset, int rows)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        CatalogResult result = new CatalogResult();
        result.listings = Lists.newArrayList();

        // if the type in question is not salable, return an empty result
        if (!isSalable(query.itemType)) {
            return result;
        }

        // if the query does not explicitly request a theme, use the player's current one (if any)
        if (query.themeGroupId == 0) {
            query.themeGroupId = (mrec != null) ? mrec.themeGroupId : 0;
        }
        if (query.themeGroupId != 0) {
            result.theme = _groupRepo.loadGroupName(query.themeGroupId);
        }

        // pass the complexity buck off to the catalog logic
        List<CatalogRecord> data = _catalogLogic.loadCatalog(
            mrec, new CatalogLogic.Query(query, 0), offset, rows);

        // convert the listings to runtime records and resolve their names
        result.listings.addAll(Lists.transform(data, CatalogRecord.TO_CARD));
        _itemLogic.resolveCardNames(result.listings);

        // log this for posterity
        final int memberId = (mrec != null) ? mrec.memberId : MsoyEventLogger.UNKNOWN_MEMBER_ID;
        final String tracker = (mrec != null) ? mrec.visitorId : getVisitorTracker();
        _eventLog.shopPageBrowsed(memberId, tracker);

        return result;
    }

    // from interface CatalogService
    public PurchaseResult<Item> purchaseItem (
        MsoyItemType itemType, int catalogId, Currency currency, int authedCost, String memories)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        BuyResult<Item> result = _catalogLogic.purchaseItem(
            mrec, itemType, catalogId, currency, authedCost, memories);
        PurchaseResult<Item> presult = result.toPurchaseResult();

        // update stats, not letting any booch in here screw with the purchase..
        try {
            boolean magicFree = result.wasMagicFreeBuy();
            MoneyTransaction memberTx = result.getMemberTransaction();
            List<MoneyTransaction> creatorTxs = result.getCreatorTransactions();
            if (!magicFree && creatorTxs != null) {
                for (MoneyTransaction creatorTx : creatorTxs) {
                    int creatorId = creatorTx.memberId;
                    if (mrec.memberId != creatorId && creatorTx.amount > 0) {
                        if (creatorTx.currency == Currency.COINS) {
                            _statLogic.incrementStat(
                                creatorId, StatType.COINS_EARNED_SELLING, creatorTx.amount);
                        }
                        // else: I guess if they earned BLING, that's it's own reward

                        // Some items have a stat that may need updating
                        if (itemType == MsoyItemType.AVATAR) {
                            _statLogic.ensureIntStatMinimum(
                                creatorId, StatType.AVATARS_CREATED, StatType.ITEM_SOLD);
                        } else if (itemType == MsoyItemType.FURNITURE) {
                            _statLogic.ensureIntStatMinimum(
                                creatorId, StatType.FURNITURE_CREATED, StatType.ITEM_SOLD);
                        } else if (itemType == MsoyItemType.DECOR) {
                            _statLogic.ensureIntStatMinimum(
                                creatorId, StatType.BACKDROPS_CREATED, StatType.ITEM_SOLD);
                        }
                    }
                }
            }

            // update their stat set, if they aren't buying something from themselves.
            if (!magicFree && (mrec.memberId != presult.ware.creatorId) &&
                (memberTx.currency == Currency.COINS)) {
                _statLogic.incrementStat(mrec.memberId, StatType.COINS_SPENT, -memberTx.amount);
            }

        } catch (Exception e) {
            log.warning("Error logging stats during item purchase", e);
        }

        // if a charity was selected, set charity info
        if (result.getCharityTransaction() != null) {
            presult.charityPercentage = _runtime.money.charityPercentage;
            presult.charity = _memberRepo.loadMemberName(result.getCharityTransaction().memberId);
        }
        return presult;
    }

    // from interface CatalogService
    public int listItem (ItemIdent item, byte rating, int pricing, int salesTarget,
                         Currency currency, int cost, int basisId, int brandId)
        throws ServiceException
    {
        final MemberRecord mrec = requireRegisteredUser();

        // validate the listing cost
        if (!currency.isValidCost(cost)) {
            log.warning("Requested to list an item for invalid price",
                        "item", item, "currency", currency, "cost", cost);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // make sure they didn't hack their client and violate the pricing minimums
        int minPrice = ItemPrices.getMinimumPrice(currency, item.type, rating);
        if (cost < minPrice) {
            log.warning("Requested to price an item too low", "who", mrec.who(), "item", item,
                        "rating", rating, "price", cost, "minPrice", minPrice);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // charities cannot list items in bars
        if (currency == Currency.BARS) {
            CharityRecord charityRec = _memberRepo.getCharityRecord(mrec.memberId);
            if (charityRec != null) {
                throw new ServiceException(ItemCodes.E_CHARITIES_CANNOT_LIST_FOR_BARS);
            }
        }

        // load a copy of the original item
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.type);
        ItemRecord originalItem = repo.loadOriginalItem(item.itemId);
        if (originalItem == null) {
            log.warning("Can't find item to list", "item", item);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // validate the basis
        if (basisId != 0) {
            if (!_itemLogic.isSuitableBasis(originalItem, basisId, currency, cost)) {
                throw new ServiceException(ItemCodes.E_BASIS_ERROR);
            }
        }

        // validate the brand
        if (brandId != 0) {
            if (_groupRepo.getBrandShare(brandId, mrec.memberId) == 0) {
                throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
            }
        }

        // sanitize the sales target
        salesTarget = Math.max(salesTarget, CatalogListing.MIN_SALES_TARGET);

        // make sure we own AND created this item
        _itemLogic.requireIsUser(mrec, originalItem.ownerId, "listItem", originalItem);

        // make sure this item is not already listed
        if (originalItem.catalogId != 0) {
            log.warning("Requested to list already listed item", "who", mrec.who(), "item", item);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // we will modify the original item (it's a clone, no need to worry) to create the new
        // catalog listing master item
        final int originalItemId = originalItem.itemId;
        final ItemRecord master = originalItem;
        master.prepareForListing(null);

        // process the payment of the listing fee and create the listing if it succeeds
        final long now = System.currentTimeMillis();
        final Currency fcurrency = currency;
        final int fpricing = pricing, fsalesTarget = salesTarget, fcost = cost;
        final int fbasisId = basisId;
        final int fbrandId = brandId;
        // the coin minimum price is the listing fee
        int listFee = ItemPrices.getMinimumPrice(Currency.COINS, item.type, rating);

        int catalogId = _moneyLogic.listItem(
            mrec, listFee, master.name, new MoneyLogic.BuyOperation<Integer>() {
            @Override
            public Integer create (boolean magicFree, Currency currency, int amountPaid) {
                // create our new immutable catalog master item
                repo.insertOriginalItem(master, true);
                // create and insert the catalog record, return its new id
                return repo.insertListing(master, originalItemId, fpricing, fsalesTarget,
                                          fcurrency, fcost, now, fbasisId, fbrandId);
            }
        }).toPurchaseResult().ware;

        // copy tags from the original item to the new listing item
        repo.getTagRepository().copyTags(originalItemId, master.itemId, mrec.memberId, now);

        // apply the first rating
        repo.getRatingRepository().rate(master.itemId, mrec.memberId, rating);

        // note in the user action system that they listed an item
        _userActionRepo.logUserAction(UserAction.listedItem(mrec.memberId));

        // publish to the member's feed if it's not hidden
        if (pricing != CatalogListing.PRICING_HIDDEN) {
            _feedLogic.publishMemberMessage(mrec.memberId, FeedMessageType.FRIEND_LISTED_ITEM,
                master.name, repo.getItemType().toByte(), catalogId, master.getThumbMediaDesc());
        }

        // some items are related to a stat that may need updating.  Use originalItem.creatorId
        // so that agents and admins don't get credit for listing someone elses stuff.
        if (item.type == MsoyItemType.AVATAR) {
            _statLogic.ensureIntStatMinimum(
                originalItem.creatorId, StatType.AVATARS_CREATED, StatType.ITEM_LISTED);
        } else if (item.type == MsoyItemType.FURNITURE) {
            _statLogic.ensureIntStatMinimum(
                originalItem.creatorId, StatType.FURNITURE_CREATED, StatType.ITEM_LISTED);
        } else if (item.type == MsoyItemType.DECOR) {
            _statLogic.ensureIntStatMinimum(
                originalItem.creatorId, StatType.BACKDROPS_CREATED, StatType.ITEM_LISTED);
        }

        // note that the listed item was created
        _itemLogic.itemUpdated(null, master);

        // note in the event log that an item was listed
        _eventLog.itemListedInCatalog(master.creatorId, mrec.visitorId, master.getType(),
            master.itemId, currency, cost, pricing, salesTarget);

        return catalogId;
    }

    // from interface CatalogServlet
    public CatalogListing loadListing (MsoyItemType itemType, int catalogId, boolean forDisplay)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        int memberId = (mrec != null) ? mrec.memberId : 0;

        // load up the old catalog record
        CatalogRecord record = _itemLogic.requireListing(itemType, catalogId, true);

        // if we're not the creator of the listing (who has to download it to update it) do
        // some access control checks
        if (mrec == null || (record.item.creatorId != memberId && !mrec.isSupport())) {
            // if the type in question is not salable, reject the request
            if (!isSalable(itemType)) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // if this listing is not meant for general sale, no lookey
            if (record.pricing == CatalogListing.PRICING_HIDDEN) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }
        }

        ItemRepository<ItemRecord> itemRepo = _itemLogic.getRepository(itemType);

        // load up the basis item if requested
        CatalogListing.BasisItem basis = null;
        if (forDisplay && record.basisId > 0) {
            CatalogRecord basisRec =
                itemRepo.loadListing(record.basisId, true);
            if (basisRec != null) {
                basis = new CatalogListing.BasisItem();
                basis.catalogId = basisRec.catalogId;
                basis.name = basisRec.item.name;
                basis.creator = _memberRepo.loadMemberName(basisRec.item.creatorId);
                if (basisRec.brandId != 0) {
                    basis.brand = _groupRepo.loadGroupName(basisRec.brandId);
                }
                basis.hidden = (basisRec.pricing == CatalogListing.PRICING_HIDDEN);
            }
        }

        // if this is a branded item, we load up a bunch of new things
        BrandDetail brand = null;
        if (record.brandId != 0) {
            brand = _groupLogic.loadBrandDetail(record.brandId);
            if (brand == null) {
                log.warning("Eek, listing's brand doesn't exist", "itemType", itemType,
                    "catalogId", catalogId, "brandId", record.brandId);
            }

        }

        // load up to 5 derived items if requested
        CatalogListing.DerivedItem[] derivatives = null;
        if (forDisplay && record.derivationCount > 0) {
            derivatives = _itemLogic.loadDerivedItems(itemType, catalogId, 5);
        }

        // secure the current price of the item for this member
        PriceQuote quote = _moneyLogic.securePrice(memberId,
            new CatalogIdent(itemType, catalogId), record.currency, record.cost);

        if (mrec != null) {
            // if this item is for sale for coins and this member is in a game, we may also want to
            // flush their pending coin earnings to ensure that they can buy it if affording it
            // requires a combination of their real coin balance plus their pending earnings
            if (record.currency == Currency.COINS) {
                _gameLogic.maybeFlushCoinEarnings(memberId, record.cost);
            }
        }

        // finally convert the listing to a runtime record
        CatalogListing listing = record.toListing();
        listing.detail.creator = _memberRepo.loadMemberCard(record.item.creatorId, false);
        listing.detail.memberItemInfo = _itemLogic.getMemberItemInfo(mrec, record.item.toItem());
        if (forDisplay) {
            listing.detail.themes = _itemLogic.loadItemStamps(
                memberId, itemType, record.listedItemId);
        } else {
            listing.detail.themes = Lists.newArrayList();
        }

        listing.quote = quote;
        listing.basis = basis;
        listing.brand = brand;
        listing.derivatives = derivatives;

        _eventLog.shopDetailsViewed(
            (mrec != null) ? mrec.memberId : MsoyEventLogger.UNKNOWN_MEMBER_ID,
            (mrec != null) ? mrec.visitorId : getVisitorTracker());

        return listing;
    }

    public CatalogListing.DerivedItem[] loadAllDerivedItems (MsoyItemType itemType, int catalogId)
        throws ServiceException
    {
        return _itemLogic.loadDerivedItems(itemType, catalogId, 0);
    }

    // from interface CatalogService
    public void updateListing (ItemIdent item)
        throws ServiceException
    {
        MemberRecord mrec = requireRegisteredUser();

        // load a copy of the original item
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.type);
        ItemRecord originalItem = repo.loadOriginalItem(item.itemId);
        if (originalItem == null) {
            log.warning("Can't find item for listing update", "item", item);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // make sure we own this item
        _itemLogic.requireIsUser(mrec, originalItem.ownerId, "updateListing", originalItem);

        // load up the old catalog record
        CatalogRecord record = repo.loadListing(originalItem.catalogId, false);
        if (record == null) {
            log.warning("Missing listing for update", "who", mrec.who(), "item", item,
                "catId", originalItem.catalogId);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // load up the old list item
        ItemRecord oldListItem = repo.loadItem(record.listedItemId);

        // we will modify the original item (it's a clone, no need to worry) to create the new
        // catalog master item
        ItemRecord master = originalItem;
        master.prepareForListing(oldListItem);

        // update our catalog master item
        repo.updateOriginalItem(master);

        // note that the listed item was updated
        _itemLogic.itemUpdated(oldListItem, master);
    }

    // from interface CatalogService
    public void updatePricing (MsoyItemType itemType, int catalogId, int pricing, int salesTarget,
                               Currency currency, int cost, int basisId, int brandId)
        throws ServiceException
    {
        MemberRecord mrec = requireRegisteredUser();

        // validate the listing cost
        if (!currency.isValidCost(cost)) {
            log.warning("Requested to update listing with an invalid price", "type", itemType,
                        "catId", catalogId, "currency", currency.toString(), "cost", cost);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // load up the listing we're updating
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
        CatalogRecord record = repo.loadListing(catalogId, true);
        if (record == null) {
            log.warning("Missing listing for update", "who", mrec.who(), "type", itemType,
                "catId", catalogId);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // make sure they didn't hack their client and violate the pricing minimums
        if (cost < ItemPrices.getMinimumPrice(currency, itemType, (byte)record.item.getRating())) {
            log.warning("Requested to price an item too low", "who", mrec.who(), "type", itemType,
                        "catId", catalogId, "rating", record.item.getRating(), "cost", cost);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        // check basis item validity (in case of client hackery)
        if (basisId != 0) {
            if (!_itemLogic.isSuitableBasis(record.item, basisId, currency, cost)) {
                throw new ServiceException(ItemCodes.E_BASIS_ERROR);
            }
            if (_themeLogic.isUsedInTemplate(itemType, catalogId)) {
                throw new ServiceException(MessageBundle.tcompose(
                    RoomCodes.E_TEMPLATE_LISTING_DERIVED, record.item.name));
            }
        }

        // load a copy of the original item
        ItemRecord originalItem = repo.loadOriginalItem(record.originalItemId);
        if (originalItem == null) {
            log.warning("Can't find original for pricing update", "who", mrec.who(),
                "type", itemType, "catId", catalogId, "itemId", record.originalItemId);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }

        if (record.brandId == 0) {
            // if the record is not listed by a brand, just make sure we're the original owner
            _itemLogic.requireIsUser(mrec, originalItem.ownerId, "updatePricing", originalItem);

        } else {
            // else verify our ownership in the brand
            _itemLogic.requireIsInBrand(mrec, record.brandId, "updatePricing", originalItem);
        }

        // if we're switching the item over to another brand, make sure we belong to that one too
        if (brandId != 0 && brandId != record.brandId) {
            _itemLogic.requireIsInBrand(mrec, brandId, "updatePricing", originalItem);
        }

        // sanitize the sales target
        salesTarget = Math.max(salesTarget, CatalogListing.MIN_SALES_TARGET);

        // now we can update the record
        repo.updatePricing(catalogId, pricing, salesTarget, currency, cost, brandId,
                           System.currentTimeMillis());

        // update the basis and derivation counts
        repo.updateBasis(record, basisId);

        // delist derivatives if the currency is changing
        if (record.currency != currency) {
            _itemLogic.removeDerivedListings(mrec, record, false);

        } else if (record.cost != cost) {
            // othwerise update their prices
            _itemLogic.updateDerivedListings(record, cost);
        }
    }

    // from interface CatalogService
    public void removeListing (MsoyItemType itemType, int catalogId)
        throws ServiceException
    {
        _itemLogic.removeListing(requireRegisteredUser(), itemType, catalogId);
    }

    // from interface CatalogService
    public Map<String, Integer> getPopularTags (MsoyItemType type, int rows)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        Map<String, Integer> result = Maps.newHashMap();
        for (TagPopularityRecord record : repo.getTagRepository().getPopularTags(rows)) {
            result.put(record.tag, record.count);
        }
        return result;
    }

    // from interface CatalogService
    public FavoritesResult loadFavorites (int memberId, MsoyItemType itemType)
        throws ServiceException
    {
        FavoritesResult result = new FavoritesResult();
        // look up the party in question, if they don't exist, return null
        result.noter = _memberRepo.loadMemberName(memberId);
        if (result.noter == null) {
            return null;
        }
        result.favorites = _itemLogic.resolveFavorites(
            _faveRepo.loadFavorites(memberId, itemType));
        return result;
    }

    // from interface CatalogService
    public List<ListingCard> loadPotentialBasisItems (MsoyItemType itemType)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        List<ListingCard> cards = Lists.newArrayList();
        for (CatalogRecord crec : _itemLogic.getRepository(itemType).loadCatalog(
            Lists.transform(_faveRepo.loadFavorites(mrec.memberId, itemType),
                new Function<FavoritedItemResultRecord, Integer>() {
                    public Integer apply (FavoritedItemResultRecord frec) {
                        return frec.catalogId;
                    }
                }))) {

            if (_itemLogic.isSuitableBasis(itemType, mrec.memberId, crec, null, 0)) {
                cards.add(crec.toListingCard());
            }
        }
        return cards;
    }

    public List<BrandDetail> loadManagedBrands ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        List<BrandDetail> brands = Lists.newArrayList();
        for (BrandShareRecord rec : _groupRepo.getBrands(mrec.memberId)) {
            brands.add(_groupLogic.loadBrandDetail(rec.groupId));
        }
        return brands;
    }

    // from interface CatalogService
    public SuiteResult loadSuite (MsoyItemType itemType, int suiteId)
        throws ServiceException
    {
        // NOTE: this method is expensive as fuck, but we cache the results on the client and
        // viewing the game shop is an extremely important step on the path to paying us money, so
        // we take the pain and hurt the database to make the game shop maximally convenient; if
        // this turns out to be a big drain, we need to look into flagging a game as having or not
        // having each of the various subtypes (easier) and tagged types (much harder)

        // right now we only support game suites (where suiteId == gameId), oh the hackery
        GameInfoRecord grec = _mgameRepo.loadGame(suiteId);
        if (grec == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // configure the suite metadata
        SuiteResult info = new SuiteResult();
        info.name = grec.name;
        info.suiteId = suiteId;
        info.creatorId = grec.creatorId;
        info.suiteTag = grec.shopTag;
        info.listings = Lists.newArrayList();

        // load up all subitems of the master
        MemberRecord mrec = getAuthedUser();
        for (GameItem gitem : grec.getSuiteTypes()) {
            if (!gitem.isSalable()) {
                continue;
            }
            CatalogLogic.Query query = new CatalogLogic.Query(
                gitem.getType(), CatalogQuery.SORT_BY_LIST_DATE);
            query.gameId = info.suiteId;
            List<CatalogRecord> slist = _catalogLogic.loadCatalog(mrec, query, 0, Short.MAX_VALUE);
            info.listings.addAll(Lists.transform(slist, CatalogRecord.TO_CARD));
        }

        if (info.suiteTag != null) {
            // all tag repositories share the same name to id mapping
            int tagId = _itemLogic.getRepository(MsoyItemType.PET).getTagRepository().getTagId(
                info.suiteTag);
            if (tagId != 0) {
                for (MsoyItemType tagType : SUITE_TAG_TYPES) {
                    CatalogLogic.Query tquery = new CatalogLogic.Query(
                        tagType, CatalogQuery.SORT_BY_LIST_DATE);
                    tquery.tagId = tagId;
                    tquery.creatorId = info.creatorId;
                    List<CatalogRecord> tlist = _catalogLogic.loadCatalog(
                        mrec, tquery, 0, Short.MAX_VALUE);
                    info.listings.addAll(Lists.transform(tlist, CatalogRecord.TO_CARD));
                }
            }
        }

        // resolve the creator names for these listings
        _itemLogic.resolveCardNames(info.listings);

        return info;
    }

    /**
     * Returns true if the specified item type is salable, false if not.
     */
    protected boolean isSalable (MsoyItemType itemType)
        throws ServiceException
    {
        try {
            return itemType.getClassForType().newInstance().isSalable();
        } catch (Exception e) {
            log.warning("Failed to check salability", "type", itemType, e);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // our dependencies
    @Inject protected CatalogLogic _catalogLogic;
    @Inject protected FavoritesRepository _faveRepo;
    @Inject protected FeedLogic _feedLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected StatLogic _statLogic;
    @Inject protected ThemeLogic _themeLogic;
    @Inject protected ThemeRepository _themeRepo;
    @Inject protected UserActionRepository _userActionRepo;

    /** Used by {@link #loadSuite}. */
    protected static final MsoyItemType[] SUITE_TAG_TYPES = new MsoyItemType[] {
        MsoyItemType.AVATAR, MsoyItemType.FURNITURE, MsoyItemType.DECOR, MsoyItemType.TOY,
        MsoyItemType.PET, MsoyItemType.PHOTO, MsoyItemType.AUDIO, MsoyItemType.VIDEO
    };
}
