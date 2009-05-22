//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.parlor.server.PlayManagerDelegate;

import com.whirled.game.client.ContentService;
import com.whirled.game.data.GameContentOwnership;
import com.whirled.game.data.GameData;

import com.threerings.msoy.item.server.persist.ItemPackRecord;
import com.threerings.msoy.item.server.persist.ItemPackRepository;

import com.threerings.msoy.game.data.PlayerObject;

/**
 * Handles the server-side of game content services.
 */
public class ContentDelegate extends PlayManagerDelegate
{
    public ContentDelegate (GameContent content)
    {
        _content = content;
    }

    public GameContent getContent ()
    {
        return _content;
    }

    /**
     * Handles {@link ContentService#purchaseItemPack}.
     */
    public void purchaseItemPack (PlayerObject plobj, final String ident,
                                  InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // TODO: check isApproved()
        // TODO: the actual purchase
    }

    /**
     * Handles {@link ContentService#consumeItemPack}.
     */
    public void consumeItemPack (final PlayerObject plobj, final String ident,
                                 InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // TODO: check isApproved()

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
    @Inject protected ItemPackRepository _ipackRepo;
}
