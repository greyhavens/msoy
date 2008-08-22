//
// $Id$

package com.threerings.msoy.item.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.MemberItemListInfo;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.ListingCard;

import com.threerings.msoy.item.server.persist.AudioRepository;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;
import com.threerings.msoy.item.server.persist.DocumentRepository;
import com.threerings.msoy.item.server.persist.FurnitureRepository;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemListInfoRecord;
import com.threerings.msoy.item.server.persist.ItemListRepository;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.LevelPackRepository;
import com.threerings.msoy.item.server.persist.PetRepository;
import com.threerings.msoy.item.server.persist.PhotoRepository;
import com.threerings.msoy.item.server.persist.PrizeRepository;
import com.threerings.msoy.item.server.persist.PropRepository;
import com.threerings.msoy.item.server.persist.SubItemRecord;
import com.threerings.msoy.item.server.persist.ToyRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;
import com.threerings.msoy.item.server.persist.VideoRepository;

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
     * An exception that may be thrown if an item repository doesn't exist.
     */
    public static class MissingRepositoryException extends Exception
    {
        public MissingRepositoryException (byte type)
        {
            super("No repository registered for " + type + ".");
        }
    }

    /**
     * Initializes repository mappings.
     */
    public void init ()
    {
        // map our various repositories
        registerRepository(Item.AUDIO, _audioRepo);
        registerRepository(Item.AVATAR, _avatarRepo);
        registerRepository(Item.DECOR, _decorRepo);
        registerRepository(Item.DOCUMENT, _documentRepo);
        registerRepository(Item.FURNITURE, _furniRepo);
        registerRepository(Item.TOY, _toyRepo);
        registerRepository(Item.GAME, _gameRepo);
        registerRepository(Item.PET, _petRepo);
        registerRepository(Item.PHOTO, _photoRepo);
        registerRepository(Item.VIDEO, _videoRepo);
        registerRepository(Item.LEVEL_PACK, _lpackRepo);
        registerRepository(Item.ITEM_PACK, _ipackRepo);
        registerRepository(Item.TROPHY_SOURCE, _tsourceRepo);
        registerRepository(Item.PRIZE, _prizeRepo);
        registerRepository(Item.PROP, _propRepo);
    }

    /**
     * Provides a reference to the {@link GameRepository} which is used for nefarious ToyBox
     * purposes.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Provides a reference to the {@link PetRepository} which is used to load pets into rooms.
     */
    public PetRepository getPetRepository ()
    {
        return _petRepo;
    }

    /**
     * Provides a reference to the {@link AvatarRepository} which is used to load pets into rooms.
     */
    public AvatarRepository getAvatarRepository ()
    {
        return _avatarRepo;
    }

    /**
     * Provides a reference to the {@link DecorRepository} which is used to load room decor.
     */
    public DecorRepository getDecorRepository ()
    {
        return _decorRepo;
    }

    /**
     * Provides a reference to the {@link TrophySourceRepository}.
     */
    public TrophySourceRepository getTrophySourceRepository ()
    {
        return _tsourceRepo;
    }

    /**
     * Returns the repository used to manage items of the specified type. Throws a service
     * exception if the supplied type is invalid.
     */
    public ItemRepository<ItemRecord> getRepository (byte type)
        throws ServiceException
    {
        try {
            return getRepositoryFor(type);
        } catch (MissingRepositoryException mre) {
            log.warning("Requested invalid repository type " + type + ".");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Returns an iterator of item types for which we have repositories.
     */
    public Iterable<Byte> getRepositoryTypes ()
    {
        return _repos.keySet();
    }

    /**
     * A small helper interface for editClone.
     */
    public static interface CloneEditOp
    {
        public void doOp (CloneRecord record, ItemRecord orig, ItemRepository<ItemRecord> repo)
            throws PersistenceException;
    }

    /**
     * Creates a new item and inserts it into the appropriate repository.
     */
    public Item createItem (MemberRecord memrec, Item item)
        throws ServiceException, PersistenceException
    {
        return createItem(memrec, item, null);
    }

    /**
     * Creates a new item and inserts it into the appropriate repository.
     */
    public Item createItem (MemberRecord memrec, Item item, ItemIdent parent)
        throws ServiceException, PersistenceException
    {
        // validate the item
        if (!item.isConsistent()) {
            log.warning("Got inconsistent item for upload? [from=" + memrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // create the persistent item record
        ItemRepository<ItemRecord> repo = getRepository(item.getType());
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
            ItemRepository<ItemRecord> prepo = getRepository(parent.type);
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

        // validate any item specific stuff
        validateItem(memrec, null, record);

        // write the item to the database
        repo.insertOriginalItem(record, false);

        // now do any post update stuff
        itemUpdated(null, record);

        return record.toItem();
    }

    /**
     * Ensures that the values specified in this item record are valid.
     *
     * @param memrec the member that is doing the updating or creating.
     * @param orecord the unmodified record in the case of an update, null in the case of a create.
     * @param nrecord the newly created or updated item.
     *
     * @exception ServiceException thrown if illegal or invalid data was detected in the item.
     */
    public void validateItem (MemberRecord memrec, ItemRecord orecord, ItemRecord nrecord)
        throws ServiceException, PersistenceException
    {
        // member must be a manager of any group they assign to a game
        if (nrecord instanceof GameRecord) {
            GameRecord grec = (GameRecord)nrecord;
            if (orecord == null || ((GameRecord)orecord).groupId != grec.groupId) {
                if (grec.groupId != Game.NO_GROUP) {
                    GroupMembershipRecord membership = _groupRepo.getMembership(grec.groupId,
                        memrec.memberId);
                    if (membership == null || membership.rank < GroupMembership.RANK_MANAGER) {
                        throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
                    }
                }
            }
        }
    }

    /**
     * Called after an item is created or updated. Performs any post create/update actions needed.
     *
     * @param orecord the unmodified record in the case of an update, null in the case of a create.
     * @param nrecord the newly created or updated item.
     */
    public void itemUpdated (ItemRecord orecord, final ItemRecord nrecord)
    {
        // let the item manager know that we've created or updated this item
        final boolean justCreated = (orecord == null);
        _omgr.postRunnable(new Runnable() {
            public void run () {
                if (justCreated) {
                    _itemMan.itemCreated(nrecord);
                } else {
                    _itemMan.itemUpdated(nrecord);
                }
            }
        });

        // update our game <-> group binding if necessary
        if (nrecord instanceof GameRecord) {
            GameRecord grec = (GameRecord)nrecord;
            if (orecord == null || ((GameRecord)orecord).groupId != grec.groupId) {
                try {
                    if (orecord != null && ((GameRecord)orecord).groupId != Game.NO_GROUP) {
                        _groupRepo.updateGroup(
                            ((GameRecord)orecord).groupId, GroupRecord.GAME_ID, 0);
                    }
                    if (grec.groupId != Game.NO_GROUP) {
                        _groupRepo.updateGroup(grec.groupId, GroupRecord.GAME_ID,
                                               Math.abs(grec.gameId));
                    }
                } catch (PersistenceException pe) {
                    log.warning("Failed to update group for game", "gameId", grec.itemId,
                                "groupId", grec.groupId);
                }
            }
        }
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
        ItemRepository<ItemRecord> repo = getRepository(itemIdent.type);
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

    /**
     * Loads up the item lists for the specified member.
     */
    public List<ItemListInfo> getItemLists (int memberId)
        throws PersistenceException
    {
        return Lists.newArrayList(
            Iterables.transform(_listRepo.loadInfos(memberId), ItemListInfoRecord.TO_INFO));
    }

    /**
     * Creates an item list for the specified member.
     */
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

    /**
     * Deletes a list and removes all list elements.
     */
    public void deleteList (int listId)
        throws PersistenceException
    {
        _listRepo.deleteList(listId);
    }

    public void addItem (int listId, Item item)
        throws PersistenceException
    {
        addItem(listId, item.getIdent());
    }

    public void addItem (int listId, ItemIdent item)
        throws PersistenceException
    {
        _listRepo.addItem(listId, item);
    }

    public void removeItem (int listId, ItemIdent item)
        throws PersistenceException
    {
        _listRepo.removeItem(listId, item);
    }

    public void addFavorite (int memberId, ItemIdent item)
        throws PersistenceException
    {
        ItemListInfo favoriteList = getFavoriteListInfo(memberId);
        _listRepo.addItem(favoriteList.listId, item);
    }

    public void incrementFavoriteCount (CatalogRecord record, byte itemType)
        throws ServiceException
    {
        incrementFavoriteCount(record, itemType, 1);
    }

    public void decrementFavoriteCount (CatalogRecord record, byte itemType)
        throws ServiceException
    {
        incrementFavoriteCount(record, itemType, -1);
    }

    public int getItemListSize (int listId)
        throws PersistenceException
    {
        return _listRepo.getSize(listId);
    }

    public int getItemListSize (int listId, byte itemType)
        throws PersistenceException
    {
        return _listRepo.getSize(listId, itemType);
    }

    public List<Item> loadItemList (int listId)
        throws PersistenceException
    {
        // look up the list elements
        ItemIdent[] idents = _listRepo.loadList(listId);
        return loadItems(idents);
    }

    public List<Item> loadItemList (ItemListQuery query)
        throws PersistenceException
    {
        // look up the list elements
        ItemIdent[] idents = _listRepo.loadList(query);
        return loadItems(idents);
    }

    public List<Item> loadItems (ItemIdent[] idents)
        throws PersistenceException
    {
        // now we're going to load all of these items
        LookupList lookupList = new LookupList();
        for (ItemIdent ident : idents) {
            try {
                lookupList.addItem(ident);
            } catch (MissingRepositoryException mre) {
                log.warning("Omitting bogus item from list: " + ident);
            }
        }

        // mass-lookup items from their respective repositories
        HashMap<ItemIdent, Item> items = Maps.newHashMap();
        for (Tuple<ItemRepository<ItemRecord>, int[]> tup : lookupList) {
            for (ItemRecord rec : tup.left.loadItems(tup.right)) {
                Item item = rec.toItem();
                items.put(item.getIdent(), item);
            }
        }

        // finally, return all the items in list order
        List<Item> list = Lists.newArrayListWithExpectedSize(idents.length);
        for (ItemIdent ident : idents) {
            list.add(items.get(ident));
        }

        return list;
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
        throws PersistenceException, ServiceException
    {
        byte itemType = item.getType();
        CatalogRecord record = getRepository(itemType).loadListing(item.catalogId, false);
        if (record == null) {
            return false;
        }

        return isFavorite(memberId, new ItemIdent(itemType, record.listedItemId));
    }

    protected void incrementFavoriteCount (CatalogRecord record, byte itemType, int increment)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = getRepository(itemType);
        try {
            repo.incrementFavoriteCount(record, increment);
        } catch (PersistenceException pex) {
            log.warning("Could not increment favorite count.", "catalogId", record.catalogId,
                "increment", increment, pex);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Check to see if the member's favorite list contains the given item.
     */
    protected boolean isFavorite(int memberId, ItemIdent item)
        throws PersistenceException
    {
        ItemListInfo favoriteList = getFavoriteListInfo(memberId);
        return _listRepo.contains(favoriteList.listId, item);
    }

    /**
     * Loads up info on the specified member's favorite items list. If the list does not yet exist,
     * it will be created.
     */
    public MemberItemListInfo getFavoriteListInfo (int memberId)
        throws PersistenceException
    {
        List<ItemListInfo> favoriteLists = Lists.newArrayList(
            Iterables.transform(_listRepo.loadInfos(memberId, ItemListInfo.FAVORITES),
                                ItemListInfoRecord.TO_INFO));

        ItemListInfo favorites;
        if (favoriteLists.isEmpty()) {
            // create the favorites list for this user
            favorites = createItemList(
                memberId, ItemListInfo.FAVORITES,
                _serverMsgs.getBundle("server").get("m.favorites_list_name"));
        } else {
            // there should never be more than one FAVORITES list per member
            if (favoriteLists.size() > 1) {
                log.warning("More than one favorites list found for member.", "memberId", memberId);
            }
            favorites = favoriteLists.get(0);
        }

        // return the member name for display purposes
        MemberRecord member = _memberRepo.loadMember(memberId);
        MemberItemListInfo list = new MemberItemListInfo(favorites);
        list.memberName = member.getName().getNormal();

        return list;
    }

    public List<Item> loadFavoriteList (int memberId)
        throws PersistenceException
    {
        ItemListInfo favoriteList = getFavoriteListInfo(memberId);
        return loadItemList(favoriteList.listId);
    }

    /**
     * A class that helps manage loading or storing a bunch of items that may be spread in
     * difference repositories.
     */
    protected class LookupList
        implements Iterable<Tuple<ItemRepository<ItemRecord>, int[]>>
    {
        /**
         * Add the specified item id to the list.
         */
        public void addItem (ItemIdent ident)
            throws MissingRepositoryException
        {
            addItem(ident.type, ident.itemId);
        }

        /**
         * Add the specified item id to the list.
         */
        public void addItem (byte itemType, int itemId)
            throws MissingRepositoryException
        {
            LookupType lt = _byType.get(itemType);
            if (lt == null) {
                lt = new LookupType(itemType, getRepositoryFor(itemType));
                _byType.put(itemType, lt);
            }
            lt.addItemId(itemId);
        }

        public void removeItem (byte itemType, int itemId)
        {
            LookupType lt = _byType.get(itemType);
            if (lt != null) {
                lt.removeItemId(itemId);
            }
        }

        // from Iterable
        public Iterator<Tuple<ItemRepository<ItemRecord>, int[]>> iterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<ItemRepository<ItemRecord>, int[]>>() {
                public boolean hasNext () {
                    return itr.hasNext();
                }
                public Tuple<ItemRepository<ItemRecord>, int[]> next () {
                    LookupType lookup = itr.next();
                    return new Tuple<ItemRepository<ItemRecord>, int[]>(
                        lookup.repo, lookup.getItemIds());
                }
                public void remove () {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public Iterator<Tuple<Byte, int[]>> typeIterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<Byte, int[]>>() {
                public boolean hasNext () {
                    return itr.hasNext();
                }
                public Tuple<Byte, int[]> next () {
                    LookupType lookup = itr.next();
                    return new Tuple<Byte, int[]>(lookup.type, lookup.getItemIds());
                }
                public void remove () {
                    throw new UnsupportedOperationException();
                }
            };
        }

        protected class LookupType
        {
            /** The item type associated with this list. */
            public byte type;

            /** The repository associated with this list. */
            public ItemRepository<ItemRecord> repo;

            /**
             * Create a new LookupType for the specified repository.
             */
            public LookupType (byte type, ItemRepository<ItemRecord> repo)
            {
                this.type = type;
                this.repo = repo;
            }

            /**
             * Add the specified item to the list.
             */
            public void addItemId (int id)
            {
                _ids.add(id);
            }

            public void removeItemId (int id)
            {
                _ids.remove(id);
            }

            /**
             * Get all the item ids in this list.
             */
            public int[] getItemIds ()
            {
                return _ids.toIntArray();
            }

            protected ArrayIntSet _ids = new ArrayIntSet();
        }

        /** A mapping of item type to LookupType record of repo / ids. */
        protected HashMap<Byte, LookupType> _byType = new HashMap<Byte, LookupType>();
    } /* End: class LookupList. */

    @SuppressWarnings("unchecked")
    protected void registerRepository (byte itemType, ItemRepository repo)
    {
        _repos.put(itemType, repo);
        repo.init(itemType);
    }

    /**
     * Get the specified ItemRepository. This method is called both from the dobj thread and the
     * servlet handler threads but need not be synchronized because the repositories table is
     * created at server startup time and never modified.
     */
    protected ItemRepository<ItemRecord> getRepositoryFor (byte type)
        throws MissingRepositoryException
    {
        ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            throw new MissingRepositoryException(type);
        }
        return repo;
    }

    /** Maps byte type ids to repository for all digital item types. */
    protected Map<Byte, ItemRepository<ItemRecord>> _repos = Maps.newHashMap();

    @Inject protected MemberRepository _memberRepo;
    @Inject protected ItemManager _itemMan;
    @Inject protected ItemListRepository _listRepo;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected ServerMessages _serverMsgs;

    // our myriad item repositories
    @Inject protected AudioRepository _audioRepo;
    @Inject protected AvatarRepository _avatarRepo;
    @Inject protected DecorRepository _decorRepo;
    @Inject protected DocumentRepository _documentRepo;
    @Inject protected FurnitureRepository _furniRepo;
    @Inject protected ToyRepository _toyRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected PetRepository _petRepo;
    @Inject protected PhotoRepository _photoRepo;
    @Inject protected VideoRepository _videoRepo;
    @Inject protected LevelPackRepository _lpackRepo;
    @Inject protected ItemPackRepository _ipackRepo;
    @Inject protected TrophySourceRepository _tsourceRepo;
    @Inject protected PrizeRepository _prizeRepo;
    @Inject protected PropRepository _propRepo;
    @Inject
    protected GroupRepository _groupRepo;
}
