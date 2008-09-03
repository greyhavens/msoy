//
// $Id$

package com.threerings.msoy.money.server.impl;

import com.google.inject.ImplementedBy;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.server.NotSecuredException;

/**
 * A cache of escrow prices.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@ImplementedBy(EscrowCacheMap.class)
public interface EscrowCache
{
    /**
     * Adds the escrow to the cache. Subsequent calls to {@link #getEscrow(int, ItemIdent)}
     * for this PriceKey will return the escrow data, unless the cache is full and the
     * escrow is dropped to make room, or the escrow expires.
     */
    void addEscrow (PriceKey key, Escrow escrow);

    /**
     * Gets the escrow for a given PriceKey. If the escrow was not previously added,
     * the price quote is expired, or it is removed from the cache to make room, then
     * this will throw an exception. This method does not remove the escrow from the cache.
     *
     * @return The secured escrow.
     * @throws NotSecuredException The member has not previously secured the price, the price is
     * expired, or it is removed from the cache to make room.
     */
    Escrow getEscrow (PriceKey key)
        throws NotSecuredException;

    /**
     * Removes the escrow, so it is no longer reserved. Does nothing if the key is not present.
     */
    void removeEscrow (PriceKey key);
}
