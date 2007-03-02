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

import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.server.persist.TagPopularityRecord;
import com.threerings.presents.data.InvocationCodes;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CatalogService}.
 */
public class CatalogServlet extends MsoyServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public List loadCatalog (int memberId, byte type, byte sortBy, String search, String tag,
                             int offset, int rows)
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
                sortBy, mature, search, tagId, offset, rows)) {
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
        ItemRepository<ItemRecord, ?, ?, ?> repo =
            MsoyServer.itemMan.getRepository(ident.type);

        try {
            final CatalogRecord<ItemRecord> listing = repo.loadListing(ident.itemId);

            if (mrec.flow < listing.flowCost) {
                // only happens if client is buggy, hacked or lagged, or in a blue moon
                throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
            }

            // create the clone row in the database!
            int cloneId = repo.insertClone(
                ident.itemId, mrec.memberId, listing.flowCost, listing.goldCost);

            // note the new purchase for the item
            repo.nudgeListing(ident.itemId, true);

            // then dress the loaded item up as a clone
            listing.item.ownerId = mrec.memberId;
            listing.item.parentId = listing.item.itemId;
            listing.item.itemId = cloneId;

            if (listing.flowCost > 0) {
                int flow = MsoyServer.memberRepo.getFlowRepository().updateFlow(
                    mrec.memberId, listing.flowCost, UserAction.BOUGHT_ITEM + " " + ident.type +
                    " " + ident.itemId, false);
                MemberManager.queueFlowUpdated(mrec.memberId, flow);
            }

            logUserAction(mrec, UserAction.BOUGHT_ITEM, ident.type + " " +
                          ident.itemId + " " + listing.flowCost + " " + listing.goldCost);

            return listing.item.toItem();
            
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Purchase failed [item=" + ident + "].", pe);
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    // from interface CatalogService
    public CatalogListing listItem (WebCreds creds, final ItemIdent ident, final int rarity,
                                    boolean list)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(creds);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo =
            MsoyServer.itemMan.getRepository(ident.type);
        try {
            if (list) {
                final int price;
                switch(rarity) {
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
                if (mrec.flow < price) {
                    // only happens if client is buggy, hacked or lagged, or in a blue moon
                    throw new ServiceException(ItemCodes.INSUFFICIENT_FLOW);
                }

                // load a copy of the original item
                ItemRecord listItem = repo.loadOriginalItem(ident.itemId);
                if (listItem == null) {
                    log.warning("Can't find item to list [item= " + ident + "]");
                    throw new ServiceException(ItemCodes.INTERNAL_ERROR);
                }
                if (listItem.ownerId == 0) {
                    log.warning("Item is already listed [item=" + ident + "]");
                    throw new ServiceException(ItemCodes.INTERNAL_ERROR);
                } else if (listItem.ownerId != mrec.memberId) {
                    log.warning("Member requested to list unowned item [who=" + mrec.accountName +
                                ", ident="+ ident + "].");
                    throw new ServiceException(ItemCodes.ACCESS_DENIED);
                }

                // reset any important bits
                listItem.clearForListing();

                // then insert it as the immutable copy we list
                repo.insertOriginalItem(listItem);

                // and finally create & insert the catalog record
                CatalogRecord record= repo.insertListing(
                    listItem, rarity, System.currentTimeMillis());

                int flow = MsoyServer.memberRepo.getFlowRepository().updateFlow(
                    mrec.memberId, price, UserAction.LISTED_ITEM + " " + ident.type +
                    " " + ident.itemId + " " + rarity, false);
                MemberManager.queueFlowUpdated(mrec.memberId, flow);

                logUserAction(mrec, UserAction.LISTED_ITEM, ident.toString() + " " + rarity);

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

            // note the return for the item
            repo.nudgeListing(ident.itemId, false);

            long mSec = System.currentTimeMillis() - cRec.purchaseTime.getTime();
            int daysLeft = 5 - ((int) (mSec / (24 * 3600 * 1000)));
            if (daysLeft < 1 || cRec.flowPaid == 0) {
                return new int[] { 0, 0 };
            }

            final int flowRefund = (cRec.flowPaid * daysLeft) / 5;
            final int goldRefund = (0 /* TODO */ * daysLeft) / 5;

            String refundDesc = 20*daysLeft + "%";
            int flow = MsoyServer.memberRepo.getFlowRepository().updateFlow(
                mrec.memberId, flowRefund, UserAction.RETURNED_ITEM + " " + ident.type +
                " " + ident.itemId + " " + refundDesc, true);
            MemberManager.queueFlowUpdated(mrec.memberId, flow);

            logUserAction(mrec, UserAction.RETURNED_ITEM, ident.type + " " +
                          ident.itemId + " " + refundDesc + " " + flowRefund + " " + goldRefund);

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
