//
// $Id$

package com.threerings.msoy.money.server;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.server.ShutdownManager;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * A cache of secured prices that uses a local EHCache to store the escrows.
 * 
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
public class PriceQuoteCache
    implements ShutdownManager.Shutdowner
{
    /**
     * Creates a cache with the given maximum number of escrows, whose entries will expire
     * after some amount of time.
     */
    @Inject public PriceQuoteCache (ShutdownManager shutmgr)
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

        shutmgr.registerShutdowner(this);
    }
    
    /**
     * Adds the quote to the cache. Subsequent calls to {@link #getEscrow(int, ItemIdent)}
     * for this PriceKey will return the quote data, unless the cache is full and the
     * quote is dropped to make room, or the quote expires.
     */
    public void addQuote (final PriceKey key, final PriceQuote quote)
    {
        _cache.put(new Element(key, quote));
    }

    /**
     * Gets the escrow for a given PriceKey. If the quote was not previously added,
     * the price quote is expired, or it is removed from the cache to make room, then
     * this will return null. This method does not remove the quote from the cache.
     *
     * @return The secured quote, or null.
     */
    public PriceQuote getQuote (final PriceKey key)
        throws NotSecuredException
    {
        final Element e = _cache.get(key);
        if (e == null) {
            return null;
        }
        return (PriceQuote)e.getValue();
    }

    /**
     * Removes the quote, so it is no longer reserved. Does nothing if the key is not present.
     */
    public void removeQuote (final PriceKey key)
    {
        _cache.remove(key);
    }

    // from ShutdownManager.Shutdowner
    public void shutdown ()
    {
        CacheManager.getInstance().shutdown();
    }

    protected static final int SECURED_PRICE_DURATION = 10;
    protected static final int MAX_SECURED_PRICES = 10000;
    protected static final String CACHE_NAME = "secured_prices";
    protected final Cache _cache;
}
