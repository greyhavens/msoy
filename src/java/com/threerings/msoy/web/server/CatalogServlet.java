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
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CatalogService}.
 */
public class CatalogServlet extends MsoyServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public List loadCatalog (int memberId, byte type, byte sortBy, String search, String tag,
                             int creator, int offset, int rows)
        throws ServiceException
    {
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);

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
                map.put(record.memberId, new MemberName(record.name, record.memberId));
            }

            // finally fill in the listings using the map
            for (CatalogListing listing : list) {
                listing.creator = map.get(listing.creator.getMemberId());
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load catalog [type=" + type + ", sort=" + sortBy +
                    ", search=" + search + ", offset=" + offset + ", rows=" + rows + "].", pe);
        }
        return list;
    }

    // from interface CatalogService
    public Item purchaseItem (WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(creds);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(ident.type);

        try {
            final CatalogRecord<ItemRecord> listing = repo.loadListing(ident.itemId);
            final int flowCost = mrec.isAdmin() ? 0 : listing.flowCost;

            if (mrec.flow < flowCost) {
                throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
            }

            // create the clone row in the database
            int cloneId = repo.insertClone(listing.item, mrec.memberId, flowCost, listing.goldCost);

            // note the new purchase for the item
            repo.nudgeListing(ident.itemId, true);

            // then dress the loaded item up as a clone
            listing.item.ownerId = mrec.memberId;
            listing.item.parentId = listing.item.itemId;
            listing.item.itemId = cloneId;

            // used for logging
            String details = ident.type + " " + ident.itemId + " " +
                flowCost + " " + listing.goldCost;

            if (flowCost > 0) {
                // take flow from purchaser
                int flow = MsoyServer.memberRepo.getFlowRepository().spendFlow(
                    mrec.memberId, flowCost, UserAction.BOUGHT_ITEM, details);
                // update member's new flow
                MemberManager.queueFlowUpdated(mrec.memberId, flow);

                // give 30% of it to the creator
                // TODO: hold this in escrow
                int creatorPortion = (3 * listing.flowCost) / 10;
                if (creatorPortion > 0) {
                    flow = MsoyServer.memberRepo.getFlowRepository().grantFlow(
                        listing.item.creatorId, creatorPortion, UserAction.RECEIVED_PAYOUT,
                        details + " " + mrec.memberId);
                    // update creator's new flow, but do not accumulate
                    MemberManager.queueFlowUpdated(listing.item.creatorId, flow);
                }
            }

            final Item item = listing.item.toItem();

            // update their runtime inventory as appropriate
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.itemMan.itemPurchased(item);
                }
            });

            return item;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Purchase failed [item=" + ident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public CatalogListing listItem (WebCreds creds, final ItemIdent ident, String descrip,
                                    final int rarity, boolean list)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(creds);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(ident.type);
        try {
            if (list) {
                final int price;
                // TEMP: admins don't have to pay to list; when this gets factored into
                // FinancialAction then admins/support can not pay for anything
                // FURTHER TEMP: item listing is currently disabled for everyone
                price = 0;
                /*if (mrec.isAdmin()) {
                    price = 0;
                } else {
                    switch (rarity) {
                    case CatalogListing.RARITY_PLENTIFUL:
                        price = 100; break;
                    case CatalogListing.RARITY_COMMON:
                        price = 200; break;
                    case CatalogListing.RARITY_NORMAL:
                        price = 300; break;
                    case CatalogListing.RARITY_UNCOMMON:
                        price = 400; break;
                    case CatalogListing.RARITY_RARE:
                        price = 500; break;
                    default:
                        log.warning("Unknown rarity [item=" + ident + ", rarity=" + rarity + "]");
                        throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
                    }
                }
                if (mrec.flow < price) {
                    throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
                }*/

                // load a copy of the original item
                ItemRecord listItem = repo.loadOriginalItem(ident.itemId);
                if (listItem == null) {
                    log.warning("Can't find item to list [item= " + ident + "]");
                    throw new ServiceException(ItemCodes.INTERNAL_ERROR);
                }
                if (listItem.ownerId != mrec.memberId) {
                    log.warning("Member requested to list unowned item [who=" + mrec.accountName +
                                ", ident="+ ident + "].");
                    throw new ServiceException(ItemCodes.ACCESS_DENIED);
                }
                // reset any important bits
                listItem.clearForListing();

                // use the updated description
                listItem.description = descrip;

                // acquire a current timestamp
                long now = System.currentTimeMillis();

                // then insert the record as the immutable copy we list (filling in its itemId)
                repo.insertOriginalItem(listItem);

                // then copy tags from original to immutable
                repo.getTagRepository().copyTags(ident.itemId, listItem.itemId, mrec.memberId, now);

                // and finally create & insert the catalog record
                CatalogRecord record= repo.insertListing(listItem, rarity, now);

                String details = ident.type + " " + ident.itemId + " " + rarity;
                if (price > 0) {
                    int flow = MsoyServer.memberRepo.getFlowRepository().spendFlow(
                        mrec.memberId, price, UserAction.LISTED_ITEM, details);
                    MemberManager.queueFlowUpdated(mrec.memberId, flow);
                } else {
                    logUserAction(mrec, UserAction.LISTED_ITEM, details);
                }

                return record.toListing();

            } else {
                // TODO: validate ownership
                return repo.removeListing(ident.itemId) ? new CatalogListing() : null;
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "List failed [item=" + ident + ", list=" + list + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public int[] returnItem (WebCreds creds, final ItemIdent ident)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(creds);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo =
            MsoyServer.itemMan.getRepository(ident.type);

        try {
            CloneRecord<ItemRecord> cRec = repo.loadCloneRecord(ident.itemId);
            if (cRec == null) {
                log.warning("Failed to find clone record [item=" + ident + "]");
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }

            // TODO: WE ONLY NEED THIS UNTIL WE HAVE ESCROW WORKING
            ItemRecord item = repo.loadOriginalItem(cRec.originalItemId);
            if (item == null) {
                log.warning("Failed to find clone record [item=" + ident + "]");
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }

            // note the return for the item
            repo.nudgeListing(ident.itemId, false);

            long mSec = System.currentTimeMillis() - cRec.purchaseTime.getTime();
            int daysLeft = 5 - ((int) (mSec / (24 * 3600 * 1000)));
            if (daysLeft < 1 || cRec.flowPaid == 0) {
                return new int[] { 0, 0 };
            }

            int flowRefund = (cRec.flowPaid * daysLeft) / 5;
            int goldRefund = (0 /* TODO */ * daysLeft) / 5;

            String details = ident.type + " " + ident.itemId + " " + (20*daysLeft) + "% " +
                flowRefund + " " + goldRefund;
            int flow = MsoyServer.memberRepo.getFlowRepository().grantFlow(
                mrec.memberId, flowRefund, UserAction.RETURNED_ITEM, details);
            // do not count refunded flow for accumulated flow purposes
            MemberManager.queueFlowUpdated(mrec.memberId, flow);

            // now we have to take 30% of the refund away from the creator
            // TODO: when escrow works, this will blessedly go away
            int creatorPortion = (3 * flowRefund) / 10;
            flow = MsoyServer.memberRepo.getFlowRepository().spendFlow(
                item.creatorId, creatorPortion, UserAction.RECEIVED_PAYOUT,
                details + " " + mrec.memberId);
            MemberManager.queueFlowUpdated(item.creatorId, flow);

            return new int[] { flowRefund, goldRefund };

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Purchase failed [item=" + ident + "].", pe);
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
}
