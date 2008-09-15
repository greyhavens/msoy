//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;

import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.game.server.WorldGameRegistry;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.peer.server.GameNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.MemberItemInfo;

import com.threerings.msoy.item.server.persist.AudioRepository;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;
import com.threerings.msoy.item.server.persist.DocumentRepository;
import com.threerings.msoy.item.server.persist.FavoriteItemRecord;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
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
            throws Exception;
    }

    /**
     * Creates a new item and inserts it into the appropriate repository.
     */
    public Item createItem (MemberRecord memrec, Item item)
        throws ServiceException
    {
        return createItem(memrec, item, null);
    }

    /**
     * Creates a new item and inserts it into the appropriate repository.
     */
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
            ItemRecord prec = prepo.loadItem(parent.itemId);
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

        // now do any post update stuff (but don't let its failure fail our request)
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
        throws ServiceException
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
        // note: we don't want to propagate any exceptions because we don't want to fail the action
        // that triggered the itemUpdated since that has already completed
        try {
            if (nrecord.getType() == Item.AVATAR) {
                // notify the old and new owners of the item
                if (orecord != null && orecord.ownerId != 0) {
                    MemberNodeActions.avatarUpdated(orecord.ownerId, orecord.itemId);
                }
                if ((orecord == null || orecord.ownerId != nrecord.ownerId) &&
                    nrecord.ownerId != 0) {
                    MemberNodeActions.avatarUpdated(nrecord.ownerId, nrecord.itemId);
                }

            } else if (nrecord.getType() == Item.GAME) {
                GameRecord grec = (GameRecord)nrecord;
                if (nrecord.ownerId != 0 && // group changes are only triggered for the original item
                    (orecord == null || ((GameRecord)orecord).groupId != grec.groupId)) {
                    if (orecord != null && ((GameRecord)orecord).groupId != Game.NO_GROUP) {
                        _groupRepo.updateGroup(
                            ((GameRecord)orecord).groupId, GroupRecord.GAME_ID, 0);
                    }
                    if (grec.groupId != Game.NO_GROUP) {
                        _groupRepo.updateGroup(
                            grec.groupId, GroupRecord.GAME_ID, Math.abs(grec.gameId));
                    }
                }

                // notify any server hosting this game that its data is updated
                _peerMan.invokeNodeAction(new GameUpdatedAction(grec.gameId));

            } else if (nrecord instanceof SubItemRecord &&
                       ((SubItem)nrecord.toItem()).getSuiteMasterType() == Item.GAME) {
                int gameId = getGameId((SubItemRecord)nrecord);
                if (gameId != 0) {
                    // notify any server hosting this game that its data is updated
                    _peerMan.invokeNodeAction(new GameUpdatedAction(gameId));
                }
            }

        } catch (Exception e) {
            log.warning("itemUpdated failed", "orecord", orecord, "nrecord", nrecord, e);
        }
    }

    /**
     * Called when an item has been deleted.
     */
    public void itemDeleted (ItemRecord orecord)
    {
        // note: we don't want to propagate any exceptions because we don't want to fail the action
        // that triggered the itemDeleted since that has already completed
        try {
            if (orecord.getType() == Item.AVATAR) {
                MemberNodeActions.avatarDeleted(orecord.ownerId, orecord.itemId);
            }

        } catch (Exception e) {
            log.warning("itemDeleted failed", "orecord", orecord, e);
        }
    }

    /**
     * Called when an item has been purchased. Handles notifying any entities that need to know as
     * well as logging the purchase.
     *
     * @param record the newly created item clone.
     */
    public void itemPurchased (ItemRecord record, int coinsPaid, int barsPaid)
    {
        if (record.getType() == Item.AVATAR) {
            MemberNodeActions.avatarUpdated(record.ownerId, record.itemId);

        } else if (record instanceof SubItemRecord &&
                   ((SubItem)record.toItem()).getSuiteMasterType() == Item.GAME) {
            SubItemRecord srecord = (SubItemRecord)record;
            // see if the owner of this game is playing a game right now (this lookup is cheaper
            // than the subsequent getGameId() lookup)
            if (_gameLogic.getPlayerWorldGameNode(record.ownerId) != null) {
                int gameId = getGameId(srecord);
                if (gameId != 0) {
                    // notify the game that the user has purchased some game content
                    _peerMan.invokeNodeAction(
                        new ContentPurchasedAction(
                            record.ownerId, gameId, srecord.getType(), srecord.ident));
                }
            }
        }

        _eventLog.itemPurchased(
            record.ownerId, record.getType(), record.itemId, coinsPaid, barsPaid);
    }

    /**
     * Resolves the supplied list of favorited items into properly initialized {@link ListingCard}
     * records.
     */
    public List<ListingCard> resolveFavorites (List<FavoriteItemRecord> faves)
        throws ServiceException
    {
        // break the list up by item type
        SetMultimap<Byte, Integer> typeMap = Multimaps.newHashMultimap();
        for (FavoriteItemRecord fave : faves) {
            typeMap.put(fave.itemType, fave.catalogId);
        }

        List<ListingCard> list = Lists.newArrayList();

        // now go through and resolve the records into listing cards by type
        for (Map.Entry<Byte, Collection<Integer>> entry : typeMap.asMap().entrySet()) {
            ItemRepository<ItemRecord> repo = getRepository(entry.getKey());
            list.addAll(Lists.transform(repo.loadCatalog(entry.getValue()), CatalogRecord.TO_CARD));
        }

        // finally resolve all of the member names in our list
        resolveCardNames(list);

        // TODO: restore the original order

        return list;
    }

    /**
     * Resolves the member names in the supplied list of listing cards.
     */
    public void resolveCardNames (List<ListingCard> list)
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

        // create an unmodified version of our record for our later call to itemUpdated()
        final ItemRecord unmod = (ItemRecord)orig.clone();
        unmod.initFromClone(record);

        // do the operation
        try {
            op.doOp(record, orig, repo);
        } catch (Exception e) {
            log.warning("Clone edit failed", "who", memrec.who(), "item", itemIdent, e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // create the proper ItemRecord representing the clone
        orig.initFromClone(record);

        // let everyone know that we've updated this item
        itemUpdated(unmod, orig);

        return orig;
    }

    /**
     * Loads up the item lists for the specified member.
     */
    public List<ItemListInfo> getItemLists (int memberId)
    {
        return Lists.newArrayList(
            Iterables.transform(_listRepo.loadInfos(memberId), ItemListInfoRecord.TO_INFO));
    }

    /**
     * Creates an item list for the specified member.
     */
    public ItemListInfo createItemList (int memberId, byte listType, String name)
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
    {
        _listRepo.deleteList(listId);
    }

    public void addItem (int listId, Item item)
    {
        addItem(listId, item.getIdent());
    }

    public void addItem (int listId, ItemIdent item)
    {
        _listRepo.addItem(listId, item);
    }

    public void removeItem (int listId, ItemIdent item)
    {
        _listRepo.removeItem(listId, item);
    }

    /**
     * Loads up and returns the specified member's rating and favorite status of the supplied item.
     */
    public MemberItemInfo getMemberItemInfo (MemberRecord mrec, Item item)
        throws ServiceException
    {
        MemberItemInfo info = new MemberItemInfo();
        if (mrec != null) {
            ItemRepository<ItemRecord> repo = getRepository(item.getType());
            info.memberRating = repo.getRating(item.itemId, mrec.memberId);
            info.favorite = (item.catalogId != 0) &&
                (_faveRepo.loadFavorite(mrec.memberId, item.getType(), item.catalogId) != null);
        }
        return info;
    }

    /**
     * Notes or clears favorite status for the specified catalog listing for the specified member.
     */
    public void setFavorite (int memberId, byte itemType, CatalogRecord record, boolean favorite)
        throws ServiceException
    {
        ItemRepository<ItemRecord> irepo = getRepository(itemType);
        if (favorite) {
            _faveRepo.noteFavorite(memberId, itemType, record.catalogId);
            irepo.incrementFavoriteCount(record.catalogId, 1);
        } else {
            _faveRepo.clearFavorite(memberId, itemType, record.catalogId);
            irepo.incrementFavoriteCount(record.catalogId, -1);
        }
    }

    public int getItemListSize (int listId)
    {
        return _listRepo.getSize(listId);
    }

    public int getItemListSize (int listId, byte itemType)
    {
        return _listRepo.getSize(listId, itemType);
    }

    public List<Item> loadItemList (int listId)
    {
        // look up the list elements
        return loadItems(_listRepo.loadList(listId));
    }

    public List<Item> loadItemList (ItemListQuery query)
    {
        // look up the list elements
        return loadItems(_listRepo.loadList(query));
    }

    public List<Item> loadItems (ItemIdent[] idents)
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
        for (Tuple<ItemRepository<ItemRecord>, Collection<Integer>> tup : lookupList) {
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

    /**
     * A class that helps manage loading or storing a bunch of items that may be spread in
     * difference repositories.
     */
    protected class LookupList
        implements Iterable<Tuple<ItemRepository<ItemRecord>, Collection<Integer>>>
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
        public Iterator<Tuple<ItemRepository<ItemRecord>, Collection<Integer>>> iterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<ItemRepository<ItemRecord>, Collection<Integer>>>() {
                public boolean hasNext () {
                    return itr.hasNext();
                }
                public Tuple<ItemRepository<ItemRecord>, Collection<Integer>> next () {
                    LookupType lookup = itr.next();
                    return new Tuple<ItemRepository<ItemRecord>, Collection<Integer>>(
                        lookup.repo, lookup.getItemIds());
                }
                public void remove () {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public Iterator<Tuple<Byte, Collection<Integer>>> typeIterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<Byte, Collection<Integer>>>() {
                public boolean hasNext () {
                    return itr.hasNext();
                }
                public Tuple<Byte, Collection<Integer>> next () {
                    LookupType lookup = itr.next();
                    return new Tuple<Byte, Collection<Integer>>(lookup.type, lookup.getItemIds());
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
            public Collection<Integer> getItemIds ()
            {
                return _ids;
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

    /**
     * Looks up the id of the game to which the supplied sub-item record belongs.
     */
    protected int getGameId (SubItemRecord srecord)
    {
        // look up the gameId of the game to which these packs belong
        int gameId = 0;
        if (srecord.suiteId < 0) {
            // listed sub-items have -catalogId as their suite id
            CatalogRecord crec = _gameRepo.loadListing(-srecord.suiteId, true);
            if (crec == null) {
                log.warning("Unable to find catalog record for updated sub-item",
                            "type", srecord.getType(), "suiteId", srecord.suiteId);
            } else {
                gameId = ((GameRecord)crec.item).gameId;
            }

        } else {
            // original sub-items have itemId as their suite id
            GameRecord grec = _gameRepo.loadOriginalItem(srecord.suiteId);
            if (grec == null) {
                log.warning("Unable to find original item for updated sub-item",
                            "type", srecord.getType(), "suiteId", srecord.suiteId);
            } else {
                gameId = grec.gameId;
            }
        }
        return gameId;
    }

    /** Notifies other nodes when a game record is updated. */
    protected static class GameUpdatedAction extends GameNodeAction
    {
        public GameUpdatedAction (int gameId) {
            super(gameId);
        }
        public GameUpdatedAction () {
        }
        @Override protected void execute () {
            _gameReg.gameUpdated(_gameId);
        }
        @Inject protected transient WorldGameRegistry _gameReg;
    }

    /** Notifies other nodes when a user has purchased game content. */
    protected static class ContentPurchasedAction extends GameNodeAction
    {
        public ContentPurchasedAction (int memberId, int gameId, byte itemType, String ident) {
            super(gameId);
            _memberId = memberId;
            _itemType = itemType;
            _ident = ident;
        }
        public ContentPurchasedAction () {
        }
        @Override protected void execute () {
            _gameReg.gameContentPurchased(_memberId, _gameId, _itemType, _ident);
        }
        protected int _memberId;
        protected byte _itemType;
        protected String _ident;
        @Inject protected transient WorldGameRegistry _gameReg;
    }

    /** Maps byte type ids to repository for all digital item types. */
    protected Map<Byte, ItemRepository<ItemRecord>> _repos = Maps.newHashMap();

    @Inject protected ServerMessages _serverMsgs;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected GameLogic _gameLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ItemListRepository _listRepo;
    @Inject protected FavoritesRepository _faveRepo;

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
    @Inject protected GroupRepository _groupRepo;
}
