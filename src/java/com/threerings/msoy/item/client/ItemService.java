//
// $Id$

package com.threerings.msoy.item.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Provides services related to items.
 */
public interface ItemService extends InvocationService<ClientObject>
{
    /**
     * Given an array of ItemIdents, provides an array of item names, in the same order as idents.
     * This can be called by any user for any item.
     */
    void getItemNames (ItemIdent[] item, ResultListener listener);

    /**
     * Load the specified item from the user's inventory for short-term examination.
     * An InvocationException will be thrown if the specified item does not belong to the user.
     */
    void peepItem (ItemIdent item, ResultListener listener);

    /**
     * Cause this item to become unused, removing it from the room that its in.
     */
    public void reclaimItem (
        ItemIdent item, ConfirmListener listener);

    /**
     * Retrieve the catalog id for the specified item.
     * @returns to the listener, an Integer object or null.
     * null - the specified item is owned by the player, we should just show the detail
     * page.
     * 0 - the item is not listed in the catalog
     * any other Integer - the catalog id.
     */
    void getCatalogId (ItemIdent item, ResultListener listener);

    /**
     * Cause this item to be removed from the user's inventory.
     */
    void deleteItem (ItemIdent item, ConfirmListener listener);

    /**
     * Adds a user flag to an item, for subsequent review by support.
     */
    void addFlag (
        ItemIdent item, ItemFlag.Kind kind, String comment,
        ConfirmListener listener);
}
