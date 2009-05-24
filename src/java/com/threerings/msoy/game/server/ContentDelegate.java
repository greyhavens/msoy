//
// $Id$

package com.threerings.msoy.game.server;

import java.util.NoSuchElementException;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.parlor.server.PlayManagerDelegate;

import com.whirled.game.client.ContentService;
import com.whirled.game.data.GameContentOwnership;
import com.whirled.game.data.GameData;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.ServiceUnit;

import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.server.CatalogLogic;
import com.threerings.msoy.item.server.persist.ItemPackRecord;
import com.threerings.msoy.item.server.persist.ItemPackRepository;

import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.PlayerObject;

import static com.threerings.msoy.Log.log;

/**
 * Handles the server-side of game content services.
 */
public class ContentDelegate extends PlayManagerDelegate
{
    public ContentDelegate (GameContent content)
    {
        _content = content;
    }

    /**
     * Returns the game content on which we're operating.
     */
    public GameContent getContent ()
    {
        return _content;
    }

    /**
     * Handles {@link ContentService#purchaseItemPack}.
     */
    public void purchaseItemPack (final PlayerObject plobj, final String ident,
                                  InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // locate the item pack in question
        final ItemPack buypack;
        try {
            buypack = Iterables.find(_content.ipacks, new Predicate<ItemPack>() {
                public boolean apply (ItemPack pack) {
                    return pack.ident.equals(ident);
                }
            });
        } catch (NoSuchElementException nsee) {
            throw new InvocationException("e.missing_item_pack");
        }
        if (buypack.catalogId == 0) {
            log.info("Requested to buy unlisted item pack.", "game", where(), "ident", ident);
            throw new InvocationException(MsoyGameCodes.E_INTERNAL_ERROR);
        }

        // go off to the invoker thread and execute the purchase
        _invoker.postUnit(new ServiceUnit("purchaseItemPack", listener, "who", plobj.who()) {
            public void invokePersistent () throws Exception {
                MemberRecord memrec = _memberRepo.loadMember(plobj.getMemberId());
                if (memrec == null) {
                    throw new InvocationException(MsoyGameCodes.E_INTERNAL_ERROR);
                }
                _catalogLogic.purchaseItem(memrec, buypack.getType(), buypack.catalogId);
            }
        });
    }

    /**
     * Handles {@link ContentService#consumeItemPack}.
     */
    public void consumeItemPack (final PlayerObject plobj, final String ident,
                                 InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // make sure they have at least one copy of this item pack
        GameContentOwnership gco = plobj.gameContent.get(
            new GameContentOwnership(_content.gameId, GameData.ITEM_DATA, ident));
        if (gco == null || gco.count < 1) {
            listener.requestFailed("e.missing_item_pack"); // checked on client, shouldn't happen
        }

        // reduce their count in the runtime by one
        if (--gco.count == 0) {
            plobj.removeFromGameContent(gco);
        } else {
            plobj.updateGameContent(gco);
        }

        // go off to the database and delete one item pack record
        _invoker.postUnit(new PersistingUnit("consumeItemPack", listener, "who", plobj.who()) {
            public void invokePersistent () throws Exception {
                int deleteId = 0;
                for (ItemPackRecord ipack :
                         _ipackRepo.loadGameClones(_content.gameId, plobj.getMemberId())) {
                    // pick the first item pack with a matching ident to delete; they're all
                    // exactly the same
                    if (ipack.ident.equals(ident)) {
                        deleteId = ipack.itemId;
                        break;
                    }
                }
                if (deleteId == 0) { // no free lunch
                    throw new InvocationException("e.missing_item_pack");
                }
                // "consume" the item
                _ipackRepo.deleteItem(deleteId);
            }
        });
    }

    protected GameContent _content;

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected CatalogLogic _catalogLogic;
    @Inject protected ItemPackRepository _ipackRepo;
    @Inject protected MemberRepository _memberRepo;
}
