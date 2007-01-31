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

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.server.persist.TagPopularityRecord;

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
        final ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "purchaseItem[" + mrec.memberId + ", " + ident + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.purchaseItem(mrec.memberId, ident, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface CatalogService
    public CatalogListing listItem (WebCreds creds, ItemIdent ident, boolean list)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(creds);

        // locate the appropriate repository
        ItemRepository<ItemRecord, ?, ?, ?> repo =
            MsoyServer.itemMan.getRepository(ident.type);

        try {
            if (list) {
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
                return repo.insertListing(listItem, System.currentTimeMillis()).toListing();

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
