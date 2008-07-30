//
// $Id$

package com.threerings.msoy.item.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.ListingCard;

import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemListInfoRecord;
import com.threerings.msoy.item.server.persist.ItemListRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.SubItemRecord;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Contains item related services used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class ItemLogic
{
    /**
     * A small helper interface for editClone.
     */
    public static interface CloneEditOp
    {
        public void doOp (CloneRecord record, ItemRecord orig, ItemRepository<ItemRecord> repo)
            throws PersistenceException;
    }

    public Item createItem (MemberRecord memrec, Item item)
        throws ServiceException
    {
        return createItem(memrec, item, null);
    }

    public Item createItem (MemberRecord memrec, Item item, ItemIdent parent)
        throws ServiceException
    {
        // validate the item
        if (!item.isConsistent()) {
            log.warning("Got inconsistent item for upload? [from=" + memrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // create the persistent item record
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(item.getType());
        final ItemRecord record = repo.newItemRecord(item);

        // configure the item's creator and owner
        record.creatorId = memrec.memberId;
        record.ownerId = memrec.memberId;

        // determine this item's suite id if it is a subitem
        if (item instanceof SubItem) {
            if (parent == null) {
                log.warning("Requested to create sub-item with no parent [who=" + memrec.who() +
                            ", item=" + item + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            ItemRepository<ItemRecord> prepo = _itemMan.getRepository(parent.type);
            ItemRecord prec = null;
            try {
                prec = prepo.loadItem(parent.itemId);
            } catch (PersistenceException pe) {
                log.warning("Failed to load parent in createItem [who=" + memrec.who() +
                        ", item=" + item.getIdent() + ", parent=" + parent + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            if (prec == null) {
                log.warning("Requested to make item with missing parent [who=" + memrec.who() +
                            ", parent=" + parent + ", item=" + item + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            if (prec.ownerId != memrec.memberId) {
                log.warning("Requested to make item with invalid parent [who=" + memrec.who() +
                            ", parent=" + prec + ", item=" + item + "].");
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // if everything is kosher, we can initialize the subitem with info from its parent
            ((SubItemRecord)record).initFromParent(prec);
        }

        // TODO: validate anything else?

        // write the item to the database
        try {
            repo.insertOriginalItem(record, false);
        } catch (PersistenceException pe) {
            log.warning("Failed to create item " + item + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the item manager know that we've created this item
        _omgr.postRunnable(new Runnable() {
            public void run () {
                _itemMan.itemCreated(record);
            }
        });

        return record.toItem();
    }

    /**
     * Resolves the member names in the supplied list of listing cards.
     */
    public void resolveCardNames (List<ListingCard> list)
        throws PersistenceException
    {
        // look up the names and build a map of memberId -> MemberName
        IntMap<MemberName> map = _memberRepo.loadMemberNames(
            list, new Function<ListingCard,Integer>() {
                public Integer apply (ListingCard card) {
                    return card.creator.getMemberId();
                }
            });
        // finally fill in the listings using the map
        for (ListingCard card : list) {
            card.creator = map.get(card.creator.getMemberId());
        }
    }

    /**
     * Helper method for editing clones.
     */
    public ItemRecord editClone (MemberRecord memrec, ItemIdent itemIdent, CloneEditOp op)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(itemIdent.type);
        try {
            // load up the old version of the item
            CloneRecord record = repo.loadCloneRecord(itemIdent.itemId);
            if (record == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            // make sure they own it (or are admin)
            if (record.ownerId != memrec.memberId && !memrec.isAdmin()) {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // load up the original record so we can see what changed
            final ItemRecord orig = repo.loadOriginalItem(record.originalItemId);
            if (orig == null) {
                log.warning("Unable to locate original of remixed clone [who=" + memrec.who() +
                    ", item=" + itemIdent + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            // do the operation
            op.doOp(record, orig, repo);

            // create the proper ItemRecord representing the clone
            orig.initFromClone(record);

            // let the item manager know that we've updated this item
            _omgr.postRunnable(new Runnable() {
                public void run () {
                    _itemMan.itemUpdated(orig);
                }
            });

            return orig;

        } catch (PersistenceException pe) {
            log.warning("Failed to edit clone " + itemIdent + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }


    public List<ItemListInfo> getItemLists (int memberId)
        throws PersistenceException
    {
        if (_omgr.isDispatchThread()) {
            throw new IllegalStateException("Must be called from the invoker");
        }

        // load up the user's lists
        return convertRecords(_listRepo.loadInfos(memberId));
    }

    public ItemListInfo createItemList (int memberId, byte listType, String name)
        throws PersistenceException
    {
        ItemListInfo listInfo = new ItemListInfo();
        listInfo.type = listType;
        listInfo.name = name;
        ItemListInfoRecord record = new ItemListInfoRecord(listInfo, memberId);
        _listRepo.createList(record);
        return record.toItemListInfo();
    }

    public void addItem (int listId, Item item) throws PersistenceException
    {
        addItem(listId, item.getIdent());
    }

    public void addItem (int listId, ItemIdent item) throws PersistenceException
    {
        _listRepo.addItem(listId, item);
    }

    public void removeItem (int listId, ItemIdent item) throws PersistenceException
    {
        _listRepo.removeItem(listId, item);
    }

    public void addFavorite (int memberId, ItemIdent item)
        throws PersistenceException
    {
        ItemListInfo favoriteList = getFavoriteListInfo(memberId);
        _listRepo.addItem(favoriteList.listId, item);
    }

    public void removeFavorite (int memberId, ItemIdent item)
        throws PersistenceException
    {
        ItemListInfo favoriteList = getFavoriteListInfo(memberId);
        _listRepo.removeItem(favoriteList.listId, item);
    }

    /**
     * Check to see if the member's favorite list contains the given item.
     */
    public boolean isFavorite(int memberId, Item item)
        throws PersistenceException
    {
        return isFavorite(memberId, item.getIdent());
    }

    /**
     * Check to see if the member's favorite list contains the given item.
     */
    public boolean isFavorite(int memberId, ItemIdent item)
        throws PersistenceException
    {
        ItemListInfo favoriteList = getFavoriteListInfo(memberId);
        return _listRepo.contains(favoriteList.listId, item);
    }

    protected ItemListInfo getFavoriteListInfo (int memberId)
        throws PersistenceException
    {
        List<ItemListInfoRecord> favoriteRecords = _listRepo.loadInfos(memberId, ItemListInfo.FAVORITES);
        List<ItemListInfo> favoriteLists = convertRecords(favoriteRecords);

        ItemListInfo favorites;

        if (favoriteLists.isEmpty()) {
            // create an favorites list for this user
            favorites = createItemList(memberId, ItemListInfo.FAVORITES, ItemListInfo.FAVORITES_NAME);

        } else {
            // TODO There should never be more than one FAVORITES list per member
            // If there are more than one list, merge them somehow?
            favorites = favoriteLists.get(0);
        }

        return favorites;
    }

    /**
     * Utility for converting a list of records into their counterparts.
     */
    protected static List<ItemListInfo> convertRecords(List<ItemListInfoRecord> records)
    {
        int nn = records.size();
        List<ItemListInfo> list = Lists.newArrayListWithExpectedSize(nn);
        for (int ii = 0; ii < nn; ii++) {
            list.add(records.get(ii).toItemListInfo());
        }
        return list;
    }

    @Inject protected MemberRepository _memberRepo;
    @Inject protected ItemManager _itemMan;
    @Inject protected ItemListRepository _listRepo;
    @Inject protected RootDObjectManager _omgr;
}
