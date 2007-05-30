//
// $Id$

package com.threerings.msoy.item.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Provides services related to items.
 */
public interface ItemService extends InvocationService
{
    /**
     * Get the items in the user's inventory.
     * TODO: WTF? Can we ever load the inventory? At best we can display
     *       a page of it.
     */
    public void getInventory (
        Client client, byte type, InvocationListener listener);

    /** 
     * Cause this item to become unused, removing it from the room that its in.
     */
    public void reclaimItem (
        Client client, ItemIdent item, ConfirmListener listener);
}
