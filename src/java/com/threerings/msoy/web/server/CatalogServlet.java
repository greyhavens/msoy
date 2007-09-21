//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.TagPopularityRecord;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CatalogService}.
 */
public class CatalogServlet extends MsoyServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public CatalogResult loadCatalog (int memberId, byte type, byte sortBy, String search,
                                      String tag, int creator, int offset, int rows,
                                      boolean includeCount)
        throws ServiceException
    {
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);

        CatalogResult result = new CatalogResult();
        List<CatalogListing> list = new ArrayList<CatalogListing>();
        try {
            boolean mature = false;
            if (memberId > 0) {
                MemberRecord mRec = MsoyServer.memberRepo.loadMember(memberId);
                if (mRec != null) {
                    mature |= mRec.isSet(MemberRecord.FLAG_SHOW_MATURE);
                }
            }

            TagNameRecord tagRecord = tag != null ? repo.getTagRepository().getTag(tag) : null;
            int tagId = tagRecord != null ? tagRecord.tagId : 0;

            // fetch catalog records and loop over them
            IntSet members = new ArrayIntSet();
            for (CatalogRecord record : repo.loadCatalog(
                sortBy, mature, search, tagId, creator, offset, rows)) {
                // convert them to listings
                list.add(record.toListing());
                // and keep track of which member names we need to look up
                members.add(record.item.creatorId);
            }

            // now look up the names and build a map of memberId -> MemberName
            IntMap<MemberName> map = new HashIntMap<MemberName>();
            int[] idArr = members.toIntArray();
            for (MemberNameRecord record: MsoyServer.memberRepo.loadMemberNames(idArr)) {
                map.put(record.memberId, record.toMemberName());
            }

            // finally fill in the listings using the map
            for (CatalogListing listing : list) {
                listing.creator = map.get(listing.creator.getMemberId());
            }

            // if they want the total number of matches, compute that as well
            if (includeCount) {
                result.listingCount = repo.countListings(mature, search, tagId, creator);
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load catalog [type=" + type + ", sort=" + sortBy +
                    ", search=" + search + ", offset=" + offset + ", rows=" + rows + "].", pe);
        }
        result.listings = list;
        return result;
    }

    // from interface CatalogService
    public Item purchaseItem (WebIdent ident, byte itemType, int catalogId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(itemType);

        try {
            CatalogRecord<ItemRecord> listing = repo.loadListing(catalogId, true);
            if (listing == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // let admins spend flow the flow that they have before they get stuff for free
            int flowCost = mrec.isAdmin() ?
                Math.min(listing.flowCost, mrec.flow) : listing.flowCost;
            if (mrec.flow < flowCost) {
                throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
            }

            // create the clone row in the database
            ItemRecord newClone = repo.insertClone(
                listing.item, mrec.memberId, flowCost, listing.goldCost);

            // note the new purchase for the item
            repo.nudgeListing(catalogId, true);

            // used for logging
            String details = itemType + " " + catalogId + " " + flowCost + " " + listing.goldCost;

            if (flowCost > 0) {
                // take flow from purchaser
                MemberFlowRecord flowRec = MsoyServer.memberRepo.getFlowRepository().spendFlow(
                    mrec.memberId, flowCost, UserAction.BOUGHT_ITEM, details);
                // update member's new flow
                MemberManager.queueFlowUpdated(flowRec);

                // give 30% of it to the creator
                // TODO: hold this in escrow
                int creatorPortion = (3 * listing.flowCost) / 10;
                if (creatorPortion > 0) {
                    flowRec = MsoyServer.memberRepo.getFlowRepository().grantFlow(
                        listing.item.creatorId, creatorPortion, UserAction.RECEIVED_PAYOUT,
                        details + " " + mrec.memberId);
                    MemberManager.queueFlowUpdated(flowRec);
                }
            }

            // update their runtime inventory as appropriate
            final Item nitem = newClone.toItem();
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.itemPurchased(nitem);
                }
            });

            return nitem;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Purchase failed [type=" + itemType +
                    ", catId=" + catalogId + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public CatalogListing listItem (WebIdent ident, ItemIdent item, String descrip, int rarity)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(item.type);
        try {
            // load a copy of the original item
            ItemRecord originalItem = repo.loadOriginalItem(item.itemId);
            if (originalItem == null) {
                log.warning("Can't find item to list [item= " + item + "]");
                throw new ServiceException(ItemCodes.INTERNAL_ERROR);
            }

            // make sure we own this item
            if (originalItem.ownerId != mrec.memberId) {
                log.warning("Member requested to list unowned item [who=" + mrec.accountName +
                            ", item=" + item + "].");
                throw new ServiceException(ItemCodes.ACCESS_DENIED);
            }

            // load up the old list item if we're updating a listing
            ItemRecord oldListItem = null;
            if (originalItem.catalogId != 0) {
                CatalogRecord record = repo.loadListing(originalItem.catalogId, false);
                if (record != null) {
                    oldListItem = repo.loadItem(record.listedItemId);
                }
            }
            boolean newListing = (oldListItem == null);

            // if this is a new listing, compute the listing cost and check that they have it
            int price = 0;
            if (newListing) {
                price = getCheckListingPrice(mrec, rarity);
            }

            // we will modify the original item (it's a clone, no need to worry) to create the new
            // catalog listing prototype item
            int originalItemId = originalItem.itemId;
            ItemRecord listItem = originalItem;
            listItem.prepareForListing(oldListItem);

            // use the updated description
            listItem.description = descrip;

            // acquire a current timestamp
            long now = System.currentTimeMillis();
            CatalogRecord record;

            // create our new immutable catalog prototype item
            repo.insertOriginalItem(listItem, true);

            // copy tags from the old listing (or the original) item to the new listing item
            int oldTagId = newListing ? originalItemId : oldListItem.itemId;
            repo.getTagRepository().copyTags(oldTagId, listItem.itemId, mrec.memberId, now);

            // we'll either create a new listing or update the old one
            if (newListing) {
                // create & insert the catalog record
                record = repo.insertListing(listItem, originalItemId, rarity, now);

                // record the listing action and charge the flow
                String details = repo.getItemType() + " " + originalItemId + " " + rarity;
                if (price > 0) {
                    MemberFlowRecord flowRec = MsoyServer.memberRepo.getFlowRepository().spendFlow(
                        mrec.memberId, price, UserAction.LISTED_ITEM, details);
                    MemberManager.queueFlowUpdated(flowRec);
                } else {
                    logUserAction(mrec, UserAction.LISTED_ITEM, details);
                }

            } else {
                // reassign ratings from the old prototype
                repo.reassignRatings(oldListItem.itemId, listItem.itemId);

                // update the catalog listing
                record = repo.updateListing(listItem, now);

                // record the listing action
                String details = repo.getItemType() + " " + originalItemId;
                logUserAction(mrec, UserAction.UPDATED_LISTING, details);
            }

            return record.toListing();

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "List item failed [item=" + item + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public void removeListing (WebIdent ident, byte itemType, int catalogId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(itemType);
        try {
            CatalogRecord listing = repo.loadListing(catalogId, true);
            if (listing == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // make sure we're the creator of the listed item
            if (listing.item.creatorId != mrec.memberId && !mrec.isSupport()) {
                log.warning("Member requested to delist unowned item [who=" + mrec.accountName +
                            ", type=" + itemType + ", catId=" + catalogId + "].");
                throw new ServiceException(ItemCodes.ACCESS_DENIED);
            }

            repo.removeListing(listing);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Remove listing failed [type=" + itemType +
                    ", catId=" + catalogId + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public int[] returnItem (WebIdent ident, final ItemIdent iident)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(ident);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo =
            MsoyServer.itemMan.getRepository(iident.type);

        try {
            CloneRecord<ItemRecord> cRec = repo.loadCloneRecord(iident.itemId);
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

            String details = iident.type + " " + iident.itemId + " " + (20*daysLeft) + "% " +
                flowRefund + " " + goldRefund;
            MemberFlowRecord flowRec = MsoyServer.memberRepo.getFlowRepository().refundFlow(
                mrec.memberId, flowRefund, UserAction.RETURNED_ITEM, details);
            MemberManager.queueFlowUpdated(flowRec);

            // now we have to take 30% of the refund away from the creator
            // TODO: when escrow works, this will blessedly go away
            int creatorPortion = (3 * flowRefund) / 10;
            flowRec = MsoyServer.memberRepo.getFlowRepository().spendFlow(
                item.creatorId, creatorPortion, UserAction.RECEIVED_PAYOUT,
                details + " " + mrec.memberId);
            MemberManager.queueFlowUpdated(flowRec);

            return new int[] { flowRefund, goldRefund };

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Purchase failed [item=" + iident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public Map<String, Integer> getPopularTags (byte type, int rows)
        throws ServiceException
    {
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        Map<String, Integer> result = new HashMap<String, Integer>();
        try {
            for (TagPopularityRecord record : repo.getTagRepository().getPopularTags(rows)) {
                result.put(record.tag, record.count);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Load popular tags failed [type=" + type + "].", pe);
        }
        return result;
    }

    /**
     * Helper function for {@link #listItem}.
     */
    protected int getCheckListingPrice (MemberRecord mrec, int rarity)
        throws ServiceException
    {
        // TEMP: admins don't have to pay to list; when this gets factored into
        // FinancialAction then admins/support can not pay for anything
        if (mrec.isAdmin()) {
            return 0;
        }

        int price;
//         switch (rarity) {
//         case CatalogListing.RARITY_PLENTIFUL:
//             price = 100; break;
//         case CatalogListing.RARITY_COMMON:
//             price = 200; break;
//         case CatalogListing.RARITY_NORMAL:
//             price = 300; break;
//         case CatalogListing.RARITY_UNCOMMON:
//             price = 400; break;
//         case CatalogListing.RARITY_RARE:
//             price = 500; break;
//         default:
//             log.warning("Unknown rarity [rarity=" + rarity + "]");
//             throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
//         }
        // FURTHER TEMP: item listing fee is currently disabled for everyone
        price = 0;

        if (mrec.flow < price) {
            throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
        }
        return price;
    }
}
