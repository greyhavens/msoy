//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.samskivert.jdbc.RepositoryListenerUnit;

import com.threerings.util.MessageBundle;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.ConfirmAdapter;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.server.BodyManager;
import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.util.ServiceUnit;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Prize;

import com.threerings.msoy.item.data.ItemCodes;

// we'll avoid import verbosity in this rare case
import com.threerings.msoy.item.server.persist.*;

import com.threerings.msoy.item.server.ItemLogic.MissingRepositoryException;

import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.server.MsoySceneRegistry;

import com.threerings.msoy.money.data.all.Currency;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
@Singleton @EventThread
public class ItemManager
    implements ItemProvider
{
    @Inject public ItemManager (InvocationManager invmgr)
    {
        // register our invocation service
        invmgr.registerDispatcher(new ItemDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    /**
     * Initializes the item manager.
     */
    public void init ()
    {
        _itemLogic.init();
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord> getRepository (ItemIdent ident, ResultListener<?> rl)
    {
        return getRepository(ident.type, rl);
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord> getRepository (byte type, ResultListener<?> rl)
    {
        try {
            return _itemLogic.getRepositoryFor(type);
        } catch (MissingRepositoryException mre) {
            rl.requestFailed(mre);
            return null;
        }
    }

    /**
     * Get the specified item.
     */
    public void getItem (final ItemIdent ident, ResultListener<Item> lner)
    {
        final ItemRepository<ItemRecord> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }
        _invoker.postUnit(new RepositoryListenerUnit<Item>("getItem", lner) {
            public Item invokePersistResult () throws Exception {
                ItemRecord rec = repo.loadItem(ident.itemId);
                if (rec == null) {
                    throw new InvocationException(ItemCodes.E_NO_SUCH_ITEM);
                }
                return rec.toItem();
            }
        });
    }

    /**
     * Awards the specified prize to the specified member.
     */
    public void awardPrize (final int memberId, final int gameId, final String gameName,
                            final Prize prize, ResultListener<Item> listener)
    {
        final ItemRepository<ItemRecord> repo = getRepository(prize.targetType, listener);
        _invoker.postUnit(new RepositoryListenerUnit<Item>("awardPrize", listener) {
            public Item invokePersistResult () throws Exception {
                CatalogRecord listing = repo.loadListing(prize.targetCatalogId, true);
                if (listing == null) {
                    throw new InvocationException(MsoyCodes.ITEM_MSGS, ItemCodes.E_NO_SUCH_ITEM);
                }
                if (listing.item.creatorId != prize.creatorId) {
                    throw new InvocationException(MsoyCodes.GAME_MSGS,
                        MessageBundle.tcompose(
                            MsoyGameCodes.E_PRIZE_CREATOR_MISMATCH, prize.ident));
                }
                log.info("Awarding prize " + listing + " to " + memberId + ".");
                Item item = repo.insertClone(listing.item, memberId, Currency.COINS, 0).toItem();
                _eventLog.prizeEarned(memberId, gameId, prize.ident, prize.targetType);
                return item;
            }

            public void handleSuccess () {
                super.handleSuccess();

// TODO: post to their feed
//                 // send them a mail message as well
//                 String subject = _serverMsgs.getBundle("server").get(
//                     "m.got_prize_subject", _result.name);
//                 String body = _serverMsgs.getBundle("server").get("m.got_prize_body");
//                 _mailMan.deliverMessage(
//                     // TODO: sender should be special system id
//                     memberId, memberId, subject, body, new GameAwardPayload(
//                         gameId, gameName, GameAwardPayload.PRIZE,
//                         _result.name, _result.getThumbnailMedia()),
//                     true, new ResultListener.NOOP<Void>());
            }
        });
    }

    /**
     * Helper function: updates usage of avatar items.  This method assumes that the specified
     * items are both valid and owned by the user in question. The supplied listener will be
     * notified of success with null.
     *
     * @see #updateItemUsage(byte, byte, int, int, int, int, ResultListener)
     */
    public void updateItemUsage (int memberId, Avatar oldAvatar, Avatar newAvatar,
                                 ResultListener<Object> lner)
    {
        updateItemUsage(Item.AVATAR, Item.USED_AS_AVATAR, memberId, memberId,
                        (oldAvatar != null) ? oldAvatar.itemId : 0,
                        (newAvatar != null) ? newAvatar.itemId : 0, lner);
    }

    /**
     * Update usage of any items. Old item will be marked as unused, and new item will be
     * marked with the itemUseType id.
     *
     * This method assumes that the specified items are both valid and owned by the user in
     * question. The supplied listener will be notified of success with null.
     */
    public void updateItemUsage (
        final byte itemType, final byte itemUseType, final int memberId, final int locationId,
        final int oldItemId, final int newItemId, ResultListener<Object> lner)
    {
        if (oldItemId == newItemId) {
            lner.requestCompleted(null); // mr. no-op
            return;
        }

        final ItemRepository<ItemRecord> repo = getRepository(itemType, lner);
        if (repo == null) {
            return; // getRepository already informed the listener about this problem
        }

        _invoker.postUnit(new RepositoryListenerUnit<Object>("updateItemUsage", lner) {
            public Object invokePersistResult () throws Exception {
                if (oldItemId != 0) {
                    repo.markItemUsage(Collections.singleton(oldItemId), Item.UNUSED, 0);
                }
                if (newItemId != 0) {
                    repo.markItemUsage(Collections.singleton(newItemId), itemUseType, locationId);
                }
                return null;
            }
        });
    }

    /**
     * Called when an avatar item is updated.
     */
    public void avatarUpdatedOnPeer (final MemberObject memObj, final int avatarId)
    {
        getItem(new ItemIdent(Item.AVATAR, avatarId), new ResultListener<Item>() {
            public void requestCompleted (Item avatar) {
                avatarUpdatedOnPeer(memObj, (Avatar) avatar);
            }

            public void requestFailed (Exception cause) {
                log.warning("Failed to resolve updated avatar.", "id", avatarId, cause);
            }
        });
    }

    /**
     * Called when an avatar item is updated.
     */
    public void avatarUpdatedOnPeer (MemberObject memObj, Avatar avatar)
    {
        memObj.startTransaction();
        try {
            boolean remove = (avatar.ownerId != memObj.getMemberId());

            // if they're wearing it, update that.
            if (avatar.equals(memObj.avatar)) {
                memObj.setAvatar(remove ? null : avatar);
                _bodyMan.updateOccupantInfo(memObj, new MemberInfo.AvatarUpdater(memObj));
            }

            // probably we'll update it in their cache, too.
            if (memObj.avatarCache.contains(avatar)) {
                if (remove) {
                    memObj.removeFromAvatarCache(avatar.getKey());
                } else {
                    memObj.updateAvatarCache(avatar);
                }

            } else if (remove) {
                // nothing, they don't have it in their cache and we want to remove it anyway

            } else if (memObj.avatarCache.size() < MemberObject.AVATAR_CACHE_SIZE) {
                memObj.addToAvatarCache(avatar);

            } else {
                Avatar oldest = avatar;
                for (Avatar av : memObj.avatarCache) {
                    if (oldest.lastTouched > av.lastTouched) {
                        oldest = av;
                    }
                }
                if (oldest != avatar) {
                    memObj.removeFromAvatarCache(oldest.getKey());
                    memObj.addToAvatarCache(avatar);
                }
            }
        } finally {
            memObj.commitTransaction();
        }
    }

    /**
     * Called to effect the deletion of an avatar on a member's userobject.
     */
    public void avatarDeletedOnPeer (MemberObject memObj, int avatarId)
    {
        memObj.startTransaction();
        try {
            if ((memObj.avatar != null) && (memObj.avatar.itemId == avatarId)) {
                // the user is wearing this item: delete
                memObj.setAvatar(null);
                _bodyMan.updateOccupantInfo(memObj, new MemberInfo.AvatarUpdater(memObj));
            }
            ItemIdent ident = new ItemIdent(Item.AVATAR, avatarId);
            if (memObj.avatarCache.containsKey(ident)) {
                memObj.removeFromAvatarCache(ident);
            }
        } finally {
            memObj.commitTransaction();
        }
    }

    /**
     * Load at most maxCount recently-touched items from the specified user's inventory.
     */
    public void loadRecentlyTouched (
        final int memberId, byte type, final int maxCount, ResultListener<List<Item>> lner)
    {
        // locate the appropriate repo
        final ItemRepository<ItemRecord> repo = getRepository(type, lner);
        if (repo == null) {
            return;
        }

        // load ye items
        _invoker.postUnit(
            new RepositoryListenerUnit<List<Item>>("loadRecentlyTouched", lner) {
            public List<Item> invokePersistResult () throws Exception {
                List<ItemRecord> list = repo.loadRecentlyTouched(memberId, maxCount);
                List<Item> returnList = Lists.newArrayListWithExpectedSize(list.size());
                for (int ii = 0, nn = list.size(); ii < nn; ii++) {
                    returnList.add(list.get(ii).toItem());
                }
                return returnList;
            }
        });
    }

    // from ItemProvider
    public void getCatalogId (
        ClientObject caller, final ItemIdent ident, final InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        getItem(ident, new ResultAdapter<Item>(rl) {
            public void requestCompleted (Item item) {
                if ((item.ownerId == user.getMemberId()) || // if the user owns it,
                        // OR, it's not in the catalog but the user is support
                        ((item.catalogId == 0) && user.tokens.isSupport())) {
                    rl.requestProcessed(null); // send them straight through to the detail page

                } else {
                    // otherwise, redirect them to the catalog
                    rl.requestProcessed(Integer.valueOf(item.catalogId));
                }
                // do NOT call super
            }
        });
    }

    // from ItemProvider
    public void getItemNames (ClientObject caller, final ItemIdent[] idents,
                              InvocationService.ResultListener rl)
        throws InvocationException
    {
        // pull item names from repos
        _invoker.postUnit(
            new RepositoryListenerUnit<String[]>("getItemNames", new ResultAdapter<String[]>(rl)) {
            public String[] invokePersistResult () throws Exception {
                String[] itemNames = new String[idents.length];
                for (int ii = 0; ii < idents.length; ii++) {
                    ItemIdent ident = idents[ii];
                    ItemRecord rec = _itemLogic.getRepository(ident.type).loadItem(ident.itemId);
                    if (rec != null) {
                        itemNames[ii] = rec.name;
                    }
                }
                return itemNames;
            }
        });
    }

    // from ItemProvider
    public void peepItem (ClientObject caller, ItemIdent ident, InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (user.isGuest()) {
            throw new InvocationException(ItemCodes.E_ACCESS_DENIED);
        }

        getItem(ident, new ResultAdapter<Item>(rl) {
            public void requestCompleted (Item item) {
                if (item.ownerId == user.getMemberId()) {
                    super.requestCompleted(item);

                } else {
                    _listener.requestFailed(ItemCodes.E_ACCESS_DENIED);
                }
            }
        });
    }

    // from ItemProvider
    public void addFlag (
        ClientObject caller, final ItemIdent ident, final ItemFlag.Kind kind, final String comment,
        InvocationService.ConfirmListener cl)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        _invoker.postUnit(new ServiceUnit("addFlag", cl) {
            @Override public void invokePersistent ()
                throws Exception
            {
                _itemLogic.addFlag(user.getMemberId(), ident, kind, comment);
            }
        });
    }

    // from ItemProvider
    public void deleteItem (ClientObject caller, final ItemIdent ident,
                            final InvocationService.ConfirmListener cl)
        throws InvocationException
    {
        // Disabled until this feature can be further considered
        // There are security concerns with allowing any user to destroy another user's item
        /*
        final MemberObject user = (MemberObject) caller;

        getItem(ident, new ResultListener<Item>() {
            public void requestCompleted (final Item result) {
                final byte type = result.getType();
                if (type == Item.DECOR || type == Item.PET || result.used == Item.USED_AS_FURNITURE) {
                // TODO: this is wrong! Respect nodes!
                    _sceneReg.resolveScene(result.location, new SceneRegistry.ResolutionListener() {
                        public void sceneWasResolved (SceneManager scene) {
                            RoomManager room = (RoomManager)scene;
                            if ( ! room.ensureEntityControl(user, ident, "selfDestruct")) {
                                cl.requestFailed(ItemCodes.E_ACCESS_DENIED);
                                return;
                            }

                            room.reclaimItem(ident, user.getMemberId());

                            ResultListener<Void> rl = new ResultListener.NOOP<Void>();
                            _invoker.postUnit(new RepositoryListenerUnit<Void>("deleteItem", rl) {
                                public Void invokePersistResult () throws Exception {
                                    ItemRepository<ItemRecord> repo = getRepository(ident.type);
                                    repo.deleteItem(ident.itemId);
                                    cl.requestProcessed();
                                    return null;
                                }
                            });
                        }

                        public void sceneFailedToResolve (int sceneId, Exception reason) {
                            log.warning("Scene failed to resolve. [id=" + sceneId + "]", reason);
                            cl.requestFailed(InvocationCodes.INTERNAL_ERROR);
                        }
                    });
                } else {
                    log.warning("Tried to reclaim invalid item type [type=" + ident.type +
                        ", id=" + ident.itemId + "]");
                    cl.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            }

            public void requestFailed (Exception cause) {
                log.warning("Failed to resolve item for deletion. [type=" + ident.type +
                    ", id=" + ident.itemId + "].");
                cl.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
        */
    }

    // from ItemProvider
    public void reclaimItem (ClientObject caller, final ItemIdent item,
                             final InvocationService.ConfirmListener lner)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (user.isGuest()) {
            throw new InvocationException(ItemCodes.E_ACCESS_DENIED);
        }

        if (item.type == Item.AVATAR) {
            log.warning("Tried to reclaim invalid item type", "who", user.who(), "item", item);
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        getItem(item, new ResultListener<Item>() {
            public void requestCompleted (Item result) {
                if (result.ownerId != user.getMemberId()) {
                    lner.requestFailed(ItemCodes.E_ACCESS_DENIED);
                    return;
                }
                if ((result.used == Item.USED_AS_FURNITURE) || (result.getType() == Item.DECOR) ||
                        (result.getType() == Item.AUDIO)) {
                    ((MsoySceneRegistry)_sceneReg).reclaimItem(
                        result.location, user.getMemberId(), item, new ConfirmAdapter(lner));

                } else {
                    // TODO: avatar reclamation will be possible
                    log.warning("Item to be reclaimed is neither decor nor furni",
                        "type", result.getType(), "id", result.itemId);
                    lner.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            }
            public void requestFailed (Exception cause) {
                log.warning("Unable to retrieve item.", cause);
                lner.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // our dependencies
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected BodyManager _bodyMan;
    @Inject protected SceneRegistry _sceneReg;
    @Inject protected ItemLogic _itemLogic;
}
