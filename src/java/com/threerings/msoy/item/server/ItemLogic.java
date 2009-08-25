//
// $Id$

package com.threerings.msoy.item.server;

import java.sql.Timestamp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DuplicateKeyException;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.underwire.web.data.Event;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.underwire.server.SupportLogic;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.game.server.GameNodeActions;
import com.threerings.msoy.game.server.PlayerNodeActions;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.money.data.all.Currency;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.IdentGameItem;
import com.threerings.msoy.item.data.all.Launcher;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.MemberItemInfo;

import com.threerings.msoy.item.server.persist.AudioRepository;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;
import com.threerings.msoy.item.server.persist.DocumentRepository;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.item.server.persist.FurnitureRepository;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.IdentGameItemRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRepository;
import com.threerings.msoy.item.server.persist.ItemListInfoRecord;
import com.threerings.msoy.item.server.persist.ItemListRepository;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.LauncherRepository;
import com.threerings.msoy.item.server.persist.LevelPackRepository;
import com.threerings.msoy.item.server.persist.PetRepository;
import com.threerings.msoy.item.server.persist.PhotoRepository;
import com.threerings.msoy.item.server.persist.PrizeRepository;
import com.threerings.msoy.item.server.persist.PropRepository;
import com.threerings.msoy.item.server.persist.ToyRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;
import com.threerings.msoy.item.server.persist.VideoRepository;
import com.threerings.msoy.item.server.persist.FavoritesRepository.FavoritedItemResultRecord;

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
    public static class MissingRepositoryException extends RuntimeException
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
        registerRepository(Item.GAME, _gameRepo);
        registerRepository(Item.ITEM_PACK, _ipackRepo);
        registerRepository(Item.LAUNCHER, _launcherRepo);
        registerRepository(Item.LEVEL_PACK, _lpackRepo);
        registerRepository(Item.PET, _petRepo);
        registerRepository(Item.PHOTO, _photoRepo);
        registerRepository(Item.PRIZE, _prizeRepo);
        registerRepository(Item.PROP, _propRepo);
        registerRepository(Item.TOY, _toyRepo);
        registerRepository(Item.TROPHY_SOURCE, _tsourceRepo);
        registerRepository(Item.VIDEO, _videoRepo);
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
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
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
     * Returns an iterator over our different item repositories.
     */
    public Iterable<ItemRepository<ItemRecord>> getRepositories ()
    {
        return _repos.values();
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
     *
     * @return the newly inserted {@link ItemRecord}.
     */
    public ItemRecord createItem (int creatorId, Item item)
        throws ServiceException
    {
        // validate the item
        if (!item.isConsistent()) {
            log.warning("Got inconsistent item for upload?", "from", creatorId, "item", item);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // create the persistent item record
        ItemRepository<ItemRecord> repo = getRepository(item.getType());
        final ItemRecord record = repo.newItemRecord(item);

        // configure the item's creator and owner
        record.creatorId = creatorId;
        record.ownerId = creatorId;

        // if this is a subitem, validate its suite id
        if (item instanceof IdentGameItem) {
            int gameId = ((IdentGameItem)item).gameId;
            GameInfoRecord grec = _mgameRepo.loadGame(gameId);
            if (grec == null || GameInfo.toDevId(grec.gameId) != gameId) {
                log.warning("Requested to create game item with invalid parent", "who", creatorId,
                            "item", item);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            if (grec.creatorId != creatorId) {
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
        }
        // Hack in the AVRG-ness of Launchers, the GWT editor code fails completely here.
        if (item instanceof Launcher) {
            updateAVRGness((Launcher)item);
        }

        // write the item to the database
        repo.insertOriginalItem(record, false);

        // now do any post update stuff (but don't let its failure fail our request)
        itemUpdated(null, record);

        return record;
    }

    /**
     * Update the AVRGness of the Launcher, which is probably incorrect because
     * GWT item editor is incapable of knowing whether it's an AVRG without passing
     * some fucking parameter in the url to indicate that it is. Fucking retarded.
     */
    public void updateAVRGness (Launcher item)
    {
        GameInfoRecord grec = _mgameRepo.loadGame(item.gameId);
        item.isAVRG = grec.isAVRG;
    }

    /**
     * Deletes the specified item. Checks access and a number of other criteria before allowing the
     * item to be deleted.
     */
    public void deleteItem (MemberRecord deleter, ItemIdent iident)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = getRepository(iident.type);

        final ItemRecord item = repo.loadItem(iident.itemId);
        if (item == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        if (item.ownerId != deleter.memberId) {
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }
        if (item.used.forAnything()) {
            throw new ServiceException(ItemCodes.E_ITEM_IN_USE);
        }
        if (item.isListedOriginal()) {
            throw new ServiceException(ItemCodes.E_ITEM_LISTED);
        }
        repo.deleteItem(iident.itemId);

        itemDeleted(item);
    }

    /**
     * Ensures that the specified user or a support user is taking the requested action.
     */
    public void requireIsUser (MemberRecord mrec, int targetId, String action, ItemRecord item)
        throws ServiceException
    {
        if (mrec != null && (mrec.memberId == targetId || mrec.isSupport())) {
            return;
        }
        String who = (mrec == null) ? "null" : mrec.who();
        String iid = (item == null) ? "null" : ""+item.itemId;
        log.warning("Access denied for catalog action", "who", who, "wanted", targetId,
            "action", action, "item", iid);
        throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
    }

    /**
     * Ensures that a user requesting an action is in a brand, or is support.
     */
    public void requireIsInBrand (MemberRecord mrec, int brandId, String action, ItemRecord item)
        throws ServiceException
    {
        if (mrec != null && _groupRepo.getBrandShare(brandId, mrec.memberId) > 0) {
            return;
        }
        String who = (mrec == null) ? "null" : mrec.who();
        String iid = (item == null) ? "null" : ""+item.itemId;
        log.warning("Access denied for catalog action", "who", who, "brand", brandId,
            "action", action, "item", iid);
        throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
    }

    /**
     * Loads and returns the specified listings. Throws {@link ItemCodes#E_NO_SUCH_ITEM} if the
     * listing could not be found.
     */
    public CatalogRecord requireListing (byte itemType, int catalogId, boolean loadListedItem)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = getRepository(itemType);
        CatalogRecord listing = repo.loadListing(catalogId, loadListedItem);
        if (listing == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        return listing;
    }

    /**
     * Removes the specified catalog listing.
     * @return the number of listings removed. This can be more than 1 if the listing is a basis
     */
    public int removeListing (MemberRecord remover, byte itemType, int catalogId)
        throws ServiceException
    {
        // load up the listing to be removed
        CatalogRecord listing = requireListing(itemType, catalogId, true);

        // make sure we're the creator of the listed item
        if (remover.memberId != listing.item.creatorId && !remover.isSupport()) {
            log.warning("Disallowing listing removal for non-owner", "who", remover.who(),
                        "listing", listing);
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }

        // get rid of derived listings
        // TODO: attribution phase II - update derived prices if there is a price change
        int removed = removeDerivedListings(remover, listing, true);

        // remove the listing record and possibly the catalog master item
        if (getRepository(itemType).removeListing(listing)) {
            itemDeleted(listing.item);
        }

        return ++removed;
    }

    /**
     * Removes all listings that are derived from this listing. Also adds a support note if the
     * remover is the item creator.
     * @param delisted true if this is due to the basis being delisted; only used for notification
     * @return the number of listings removed
     */
    public int removeDerivedListings (MemberRecord remover, CatalogRecord record, boolean delisted)
        throws ServiceException
    {
        if (record.derivationCount == 0) {
            return 0;
        }

        // TODO: attribution phase II - recursively delist derivatives

        // record a note unless the remover is support
        StringBuilder note = remover.isSupport() ? null :
            new StringBuilder("Delisted: " + delisted + "\n");
        appendToNote(note, "Basis listing", record);
        appendToNote(note, "Basis Item", record.item);

        Multimap<Integer, ItemRecord> items = HashMultimap.create();
        ItemRepository<?> repo = getRepository(record.item.getType());
        int removed = 0;
        try {
            for (CatalogRecord derived : repo.loadCatalog(
                repo.loadDerivativeIds(record.catalogId, 0))) {
                appendToNote(note, "Derived Listing", derived);
                appendToNote(note, "Derived Item", derived.item);

                // remove the listing record and possibly the catalog master item
                if (repo.removeListing(derived)) {
                    itemDeleted(derived.item);
                }

                // stash the derived item for later notification
                items.put(derived.item.creatorId, derived.item);

                ++removed;
            }

        } finally {
            // add a support note on the remover - it's not necessarily wrong, just good to know
            if (note != null) {
                String truncMsg = "\n\nTruncated %d characters";
                int maxLen = Event.CHAT_HISTORY_LENGTH - String.format(truncMsg, 9999999).length();
                if (note.length() > maxLen) {
                    truncMsg = String.format(truncMsg, note.length() - maxLen);
                    note.setLength(maxLen);
                    note.append(truncMsg);
                }
                _supportLogic.addNote(remover.getName(), remover.memberId,
                    "Delisted " + record.derivationCount + " derived items",
                    note.toString(), record.item.toItem().getPrimaryMedia().getMediaPath());
            }

            // notify the derived item owners that their stuff was delisted, just send them the
            // item names for now
            // TODO: i18n
            // TODO: when we are in margaritaville, use a payload with real links
            for (Map.Entry<Integer, Collection<ItemRecord>> entry : items.asMap().entrySet()) {
                // don't bother notifying yourself - it can only happen if the remover is support
                // but I don't want to confuse the mail system
                if (entry.getKey() == remover.memberId) {
                    continue;
                }

                // build the message body
                // TODO: i18n
                StringBuilder body = new StringBuilder();
                body.append("This is an automated message.\n\n");
                body.append("The following listings were removed due to the ");
                body.append(delisted ? "delisting" : "repricing");
                body.append(" of \"").append(record.item.name).append("\":\n\n");
                for (ItemRecord rec : entry.getValue()) {
                    body.append(rec.name).append("\n");
                }
                try {
                    MemberRecord recip = _memberRepo.loadMember(entry.getKey());
                    _mailLogic.startBulkConversation(
                        remover, recip, "Delisted Items", body.toString(), null);

                } catch (RuntimeException re) {
                    log.warning("Failed to send mail message for delisted derived items",
                        "basisId", record.catalogId, "count", entry.getValue().size(),
                        "recipient", entry.getKey());
                }
            }
        }

        return removed;
    }

    /**
     * Updates the cost of all listings that use the given listing as a basis. The item field is
     * expected to be resolved for this method. Each derived item will have its price augmented by
     * the difference between the given new cost and the old cost (that specified by the given
     * record). This may be positive or negative.
     */
    public void updateDerivedListings (CatalogRecord basis, int newCost)
    {
        if (basis.derivationCount == 0 || basis.cost == newCost) {
            return;
        }

        // TODO: attribution phase II - recursively update derived listings

        byte type = basis.item.getType();
        try {
            // give all derived listings a new price
            int updated =
                getRepository(type).updateDerivedCosts(basis.catalogId, newCost - basis.cost);

            if (updated != basis.derivationCount) {
                log.warning("Mismatch in number of updated derived records and derivationCount",
                    "type", type, "catId", basis.catalogId, "updated", updated,
                    "derivationCount", basis.derivationCount);
            }

            log.info("Updated cost of derived listings", "type", type, "basisId", basis.catalogId,
                "count", updated, "oldCost", basis.cost, "newCost", newCost);

        } catch (Exception e) {
            log.info("Failed to update cost of derived listings", "type", type,
                "basisId", basis.catalogId, "oldCost", basis.cost, "newCost", newCost);
        }
    }

    /**
     * Very minimal logic to grant a subscriber the free item of the month.
     */
    public Item grantItem (MemberRecord grantee, CatalogRecord listing)
        throws ServiceException
    {
        // I hate copying this apart from normal buying, but it's very special, you see.
        ItemRepository<ItemRecord> repo = getRepository(listing.item.getType());
        ItemRecord clone = repo.insertClone(listing.item, grantee.memberId, Currency.BARS, 0);
        itemPurchased(clone, Currency.BARS, 0);
        return clone.toItem();
    }

    /**
     * Transfer all the items used in the specified scene to the new owner.
     */
    public void transferRoomItems (int oldOwnerId, int newOwnerId, int sceneId)
    {
        for (byte itemType : Item.ROOM_TYPES) {
            getRepositoryFor(itemType).transferRoomItems(sceneId, oldOwnerId, newOwnerId);
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

            } else if (nrecord instanceof IdentGameItemRecord) {
                // notify any server hosting this game that its content has been updated
                _gameActions.gameUpdated(((IdentGameItemRecord)nrecord).gameId);
            }

        } catch (Exception e) {
            log.warning("itemUpdated failed", "orecord", orecord, "nrecord", nrecord, e);
        }
    }

    /**
     * Called after an item is deleted. Performs any post delete actions needed.
     *
     * @param record the item record for the just deleted item.
     */
    public void itemDeleted (ItemRecord record)
    {
        // note: we don't want to propagate any exceptions from here on out because we don't want
        // to fail the item deletion since the item is already gone
        try {
            if (record.getType() == Item.AVATAR) {
                MemberNodeActions.avatarDeleted(record.ownerId, record.itemId);
            }

        } catch (Exception e) {
            log.warning("itemDeleted failed", "record", record, e);
        }
    }

    /**
     * Called when an item has been purchased. Handles notifying any entities that need to know as
     * well as logging the purchase.
     *
     * @param record the newly created item clone.
     */
    public void itemPurchased (ItemRecord record, Currency currency, int amountPaid)
    {
        if (record.getType() == Item.AVATAR) {
            MemberNodeActions.avatarUpdated(record.ownerId, record.itemId);

        } else if (record instanceof IdentGameItemRecord) {
            IdentGameItemRecord srecord = (IdentGameItemRecord)record;
            // see if the owner of this game is playing a game right now
            if (_peerMan.locateClient(GameAuthName.makeKey(record.ownerId)) != null) {
                // notify the game that the user has purchased some game content
                _playerActions.gameContentPurchased(
                    record.ownerId, srecord.gameId, srecord.getType(), srecord.ident);
            }
        }

        _eventLog.itemPurchased(
            record.ownerId, record.getType(), record.itemId, currency, amountPaid);
    }

    /**
     * Resolves the supplied list of favorited items into properly initialized {@link ListingCard}
     * records.
     */
    public List<ListingCard> resolveFavorites (List<FavoritedItemResultRecord> faves)
        throws ServiceException
    {
        // break the list up by item type
        SetMultimap<Byte, Integer> typeMap = HashMultimap.create();
        for (FavoritedItemResultRecord fave : faves) {
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
        IntMap<MemberName> memberMap = _memberRepo.loadMemberNames(
            list, new Function<ListingCard,Integer>() {
                public Integer apply (ListingCard card) {
                    return card.creator.getMemberId();
                }
            });
        // finally fill in the listings using the map
        for (ListingCard card : list) {
            card.creator = memberMap.get(card.creator.getMemberId());
        }

        // look up the names and build a map of memberId -> MemberName
        IntMap<GroupName> brandMap = _groupRepo.loadGroupNames(
            list, new Function<ListingCard,Integer>() {
                public Integer apply (ListingCard card) {
                    return (card.brand != null) ? card.brand.getGroupId() : null;
                }
            });
        // finally fill in the listings using the map
        for (ListingCard card : list) {
            if (card.brand != null) {
                card.brand = brandMap.get(card.brand.getGroupId());
            }
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

        // make sure they own it (or are support+)
        if (record.ownerId != memrec.memberId && !memrec.isSupport()) {
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
     * Add a flag for the specified item, or return an error reason.
     */
    public void addFlag (int memberId, ItemIdent ident, ItemFlag.Kind kind, String comment)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = getRepository(ident.type);

        // TODO: If things get really tight, this could use updatePartial() later.
        ItemRecord item = repo.loadItem(ident.itemId);
        if (item == null) {
            log.warning("Missing item for addFlag()", "flag", kind, "");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemFlagRecord frec = new ItemFlagRecord();
        frec.comment = comment;
        frec.kind = kind;
        frec.memberId = memberId;
        frec.itemType = ident.type;
        // if we're being asked to flag a clone, instead make it refer to the original
        frec.itemId = (item.sourceId == 0) ? item.itemId : item.sourceId;
        frec.timestamp = new Timestamp(System.currentTimeMillis());
        try {
            _itemFlagRepo.addFlag(frec);

        } catch (DuplicateKeyException dke) {
            throw new ServiceException(ItemCodes.E_ITEM_ALREADY_FLAGGED);
        }
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

    /**
     * Loads the specified item from the database, returns null if it could not be loaded.
     */
    public Item loadItem (ItemIdent ident)
    {
        ItemRepository<ItemRecord> repo = getRepositoryFor(ident.type);
        ItemRecord rec = repo.loadItem(ident.itemId);
        return rec != null ? rec.toItem() : null;
    }

    /**
     * Loads all items in the supplied array of idents.
     */
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
        List<Item> list = Lists.newArrayListWithCapacity(idents.length);
        for (ItemIdent ident : idents) {
            list.add(items.get(ident));
        }

        return list;
    }

    /**
     * Loads information about each listing that is derived from the given listing.
     * @param max maximum number of items to return, or 0 to return all listings
     */
    public CatalogListing.DerivedItem[] loadDerivedItems (byte itemType, int catalogId, int max)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = getRepository(itemType);
        List<CatalogListing.DerivedItem> items = Lists.newArrayListWithExpectedSize(max);
        for (int derivativeId : repo.loadDerivativeIds(catalogId, max)) {
            CatalogRecord drec = repo.loadListing(derivativeId, true);
            if (drec == null) {
                continue;
            }
            CatalogListing.DerivedItem derived = new CatalogListing.DerivedItem();
            derived.catalogId = drec.catalogId;
            derived.name = drec.item.name;
            items.add(derived);
        }
        return items.toArray(new CatalogListing.DerivedItem[items.size()]);
    }

    /**
     * Checks if a listing of the given derived item can be based on the existing listing with the
     * given id. If currency is not null, then the currency and cost are checked against the
     * minimum pricing requirements of a derived item as well.
     */
    public boolean isSuitableBasis (ItemRecord derived, int basisId, Currency currency, int cost)
        throws ServiceException
    {
        final ItemRepository<ItemRecord> repo = getRepository(derived.getType());
        List<CatalogRecord> basisRecs = repo.loadCatalog(Collections.singleton(basisId));
        if (basisRecs.size() == 0) {
            return false;
        }
        CatalogRecord basisRec = basisRecs.get(0);
        return isSuitableBasis(derived.getType(), derived.creatorId, basisRec, currency, cost);
    }

    /**
     * Checks if a listing of the given type by the given creator id can be based on the given
     * existing listing. If currency is not null, then the currency and cost are checked against
     * the minimum pricing requirements of a derived item as well.
     */
    public boolean isSuitableBasis (byte type, int creatorId, CatalogRecord basisRec,
                                    Currency currency, int cost)
    {
        if (basisRec == null || basisRec.item == null || basisRec.basisId != 0 ||
            basisRec.item.creatorId == creatorId) {
            return false;
        }

        if (type != basisRec.item.getType() || !Item.supportsDerivation(type)) {
            return false;
        }

        if (currency != null) {
            int minCost = CatalogListing.getMinimumDerivedCost(currency, basisRec.cost);
            if (basisRec.currency != currency || cost < minCost) {
                return false;
            }
        }

        return true;
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

    protected static void appendToNote (StringBuilder note, String objectName, Object object)
    {
        if (note == null) {
            return;
        }
        String sep = "\n  ";
        note.append(objectName).append(":").append(sep);
        StringUtil.fieldsToString(note, object, sep);
        note.append("\n");
    }

    /** Maps byte type ids to repository for all digital item types. */
    protected Map<Byte, ItemRepository<ItemRecord>> _repos = Maps.newHashMap();

    @Inject protected FavoritesRepository _faveRepo;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameNodeActions _gameActions;
    @Inject protected PlayerNodeActions _playerActions;
    @Inject protected ItemFlagRepository _itemFlagRepo;
    @Inject protected ItemListRepository _listRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected SupportLogic _supportLogic;

    // our myriad item repositories
    @Inject protected AudioRepository _audioRepo;
    @Inject protected AvatarRepository _avatarRepo;
    @Inject protected DecorRepository _decorRepo;
    @Inject protected DocumentRepository _documentRepo;
    @Inject protected FurnitureRepository _furniRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ItemPackRepository _ipackRepo;
    @Inject protected LauncherRepository _launcherRepo;
    @Inject protected LevelPackRepository _lpackRepo;
    @Inject protected PetRepository _petRepo;
    @Inject protected PhotoRepository _photoRepo;
    @Inject protected PrizeRepository _prizeRepo;
    @Inject protected PropRepository _propRepo;
    @Inject protected ToyRepository _toyRepo;
    @Inject protected TrophySourceRepository _tsourceRepo;
    @Inject protected VideoRepository _videoRepo;
}
