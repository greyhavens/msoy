//
// $Id$

package com.threerings.msoy.money.server.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.google.inject.Singleton;
import com.threerings.msoy.money.server.NotSecuredException;

/**
 * Implementation of {@link EscrowCache} that uses a local EHCache to store the escrows.
 * 
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
public class EscrowCacheMap
    implements EscrowCache
{
    /**
     * Creates a cache with the given maximum number of escrows, whose entries will expire
     * after some amount of time.
     * 
     * @param maxElements Maximum number of secured prices that can be stored in memory.
     * @param expireSeconds Number of seconds until a secured price will be purged from the cache.
     */
    public EscrowCacheMap (final int maxElements, final int expireSeconds)
    {
        Cache cache = CacheManager.getInstance().getCache(CACHE_NAME);
        if (cache == null) {
            cache = new Cache(CACHE_NAME, maxElements, false, false, expireSeconds, expireSeconds);
            CacheManager.getInstance().addCache(cache);

        } else {
            cache.removeAll();
        }
        _cache = cache;
    }

    // from EscrowCache
    public Escrow getEscrow (final PriceKey key)
        throws NotSecuredException
    {
        final Element e = _cache.get(key);
        if (e == null) {
            return null;
        }
        return (Escrow)e.getValue();
    }

    // from EscrowCache
    public void addEscrow (final PriceKey key, final Escrow escrow)
    {
        _cache.put(new Element(key, escrow));
    }

    // from EscrowCache
    public void removeEscrow (final PriceKey key)
    {
        _cache.remove(key);
    }

    private static final String CACHE_NAME = "secured_prices";
    private final Cache _cache;
}
