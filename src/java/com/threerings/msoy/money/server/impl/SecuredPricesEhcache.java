//
// $Id$

package com.threerings.msoy.money.server.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.RMIAsynchronousCacheReplicator;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.server.NotSecuredException;

/**
 * Implementation of {@link SecuredPricesCache} that uses ehcache to store the secured prices
 * in memory distributedly.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
class SecuredPricesEhcache
    implements SecuredPricesCache
{
    /**
     * Creates a cache with the given maximum number of secured prices, whose entries will
     * expire after some amount of time.
     * 
     * @param maxElements Maximum number of secured prices that can be stored in memory.
     * @param expireSeconds Number of seconds until a secured price will be purged from the
     *      cache.
     */
    public SecuredPricesEhcache (final int maxElements, final int expireSeconds)
    {
        if (CacheManager.getInstance().getCache(CACHE_NAME) == null) {
            this.cache = new Cache(CACHE_NAME, maxElements, false, false, expireSeconds, expireSeconds);
            
            // If we're running ehcache distributedly, then add an event listener to
            // send out updates.
            if (CacheManager.getInstance().getCacheManagerPeerProvider() != null) {
                this.cache.getCacheEventNotificationService().registerListener(
                    new RMIAsynchronousCacheReplicator(true, true, true, true, 1000));
            }
            CacheManager.getInstance().addCache(cache);
        } else {
            this.cache = CacheManager.getInstance().getCache(CACHE_NAME);
            this.cache.removeAll();
        }
    }
    
    public SecuredPrices getSecuredPrice (final int memberId, final ItemIdent item)
        throws NotSecuredException
    {
        final Element e = cache.get(new PriceKey(memberId, item));
        if (e == null) {
            return null;
        }
        return (SecuredPrices)e.getValue();
    }

    public void securePrice (final int memberId, final ItemIdent item, final SecuredPrices prices)
    {
        cache.put(new Element(new PriceKey(memberId, item), prices));
    }
    
    public void removeSecuredPrice (final int memberId, final ItemIdent item)
    {
        cache.remove(new PriceKey(memberId, item));
    }

    private static final String CACHE_NAME = "secured_prices";
    private final Cache cache;
}
