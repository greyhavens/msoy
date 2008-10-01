//
// $Id$

package com.threerings.msoy.money.server;

import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.google.common.base.Preconditions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.msoy.item.data.all.CatalogIdent;

import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * A cache of secured prices that uses a local EHCache to store the escrows.
 * 
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
public class PriceQuoteCache
{
    /**
     * Creates a cache with the given maximum number of escrows, whose entries will expire after
     * some amount of time.
     */
    @Inject public PriceQuoteCache (CacheManager cachemgr)
    {
        Cache cache = cachemgr.getCache(CACHE_NAME);
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
     * Adds the quote to the cache. Subsequent calls to {@link #getQuote} for this PriceKey will
     * return the quote data, unless the cache is full and the quote is dropped to make room, or
     * the quote expires.
     */
    public void addQuote (int memberId, CatalogIdent item, PriceQuote quote)
    {
        _cache.put(new Element(new PriceKey(memberId, item), quote));
    }

    /**
     * Gets the escrow for a given PriceKey. If the quote was not previously added,
     * the price quote is expired, or it is removed from the cache to make room, then
     * this will return null. This method does not remove the quote from the cache.
     *
     * @return The secured quote, or null.
     */
    public PriceQuote getQuote (int memberId, CatalogIdent item)
        throws NotSecuredException
    {
        final Element e = _cache.getQuiet(new PriceKey(memberId, item));
        if (e == null) {
            return null;
        }
        return (PriceQuote)e.getValue();
    }

    /**
     * Removes the quote, so it is no longer reserved. Does nothing if the key is not present.
     */
    public void removeQuote (int memberId, CatalogIdent item)
    {
        _cache.remove(new PriceKey(memberId, item));
    }

    protected final Cache _cache;

    protected static final int SECURED_PRICE_DURATION = 10;
    protected static final int MAX_SECURED_PRICES = 10000;
    protected static final String CACHE_NAME = "secured_prices";
}

class PriceKey
    implements Serializable
{
    public PriceKey (int memberId, CatalogIdent item)
    {
        Preconditions.checkNotNull(item);
        _memberId = memberId;
        _ident = item;
    }

    @Override
    public int hashCode ()
    {
        int hash = _memberId;
        hash = 31 * hash + _ident.hashCode();
        return hash;
    }

    @Override
    public boolean equals (final Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof PriceKey)) {
            return false;
        }
        PriceKey that = (PriceKey)obj; 
        return (_memberId == that._memberId) && _ident.equals(that._ident);
    }

    protected int _memberId;
    protected CatalogIdent _ident;
}
