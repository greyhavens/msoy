//
// $Id$

package com.threerings.msoy.money.server;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.google.inject.Singleton;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Implementation of {@link EscrowCache} that uses a local EHCache to store the escrows.
 * 
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
public class EscrowCache
{
    /**
     * Creates a cache with the given maximum number of escrows, whose entries will expire
     * after some amount of time.
     */
    public EscrowCache ()
    {
        Cache cache = CacheManager.getInstance().getCache(CACHE_NAME);
        if (cache == null) {
            int expireSeconds = SECURED_PRICE_DURATION * 60;
            cache = new Cache(CACHE_NAME, MAX_SECURED_PRICES, false, false,
                expireSeconds, expireSeconds);
            CacheManager.getInstance().addCache(cache);

        } else {
            cache.removeAll();
        }
        _cache = cache;
    }
    
    /**
     * Adds the escrow to the cache. Subsequent calls to {@link #getEscrow(int, ItemIdent)}
     * for this PriceKey will return the escrow data, unless the cache is full and the
     * escrow is dropped to make room, or the escrow expires.
     */
    public void addEscrow (final PriceKey key, final Escrow escrow)
    {
        _cache.put(new Element(key, escrow));
    }

    /**
     * Gets the escrow for a given PriceKey. If the escrow was not previously added,
     * the price quote is expired, or it is removed from the cache to make room, then
     * this will throw an exception. This method does not remove the escrow from the cache.
     *
     * @return The secured escrow.
     * @throws NotSecuredException The member has not previously secured the price, the price is
     * expired, or it is removed from the cache to make room.
     */
    public Escrow getEscrow (final PriceKey key)
        throws NotSecuredException
    {
        final Element e = _cache.get(key);
        if (e == null) {
            return null;
        }
        return (Escrow)e.getValue();
    }

    /**
     * Removes the escrow, so it is no longer reserved. Does nothing if the key is not present.
     */
    public void removeEscrow (final PriceKey key)
    {
        _cache.remove(key);
    }

    private static final int SECURED_PRICE_DURATION = 10;
    private static final int MAX_SECURED_PRICES = 10000;
    private static final String CACHE_NAME = "secured_prices";
    private final Cache _cache;
}
