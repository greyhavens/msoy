//
// $Id$

package com.threerings.msoy.money.server.impl;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.server.NotSecuredException;

/**
 * A cache of secured prices.  The cache should be distributed across all other nodes.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public interface SecuredPricesCache
{
    /**
     * Adds the secured price to the cache.  Subsequent calls to {@link #getSecuredPrice(int, ItemIdent)}
     * for this member/item will return the secured price, unless the cache is full and the secured price
     * is dropped to make room, or the price expires.
     * 
     * @param memberId ID of the member securing the price.
     * @param item Item whose price is being secured.
     * @param prices The secured prices.
     */
    void securePrice (int memberId, ItemIdent item, SecuredPrices prices);
    
    /**
     * Gets the secured price for a given member and item.  If the member has not previously secured
     * the price, the price is expired, or it is removed from the cache to make room, then this will
     * throw an exception.
     * 
     * @param memberId ID of the member who secured the price.
     * @param item Item whose prices were secured.
     * @return The secured prices.
     * @throws NotSecuredException The member has not previously secured the price, the price is 
     *      expired, or it is removed from the cache to make room.
     */
    SecuredPrices getSecuredPrice (int memberId, ItemIdent item)
        throws NotSecuredException;
    
    /**
     * Removes the secured price, so it is no longer secured.  Does nothing if the member has not
     * previously secured the item.
     * 
     * @param memberId ID of the member who secured the price.
     * @param item Item whose prices were secured.
     */
    void removeSecuredPrice (int memberId, ItemIdent item);
}
