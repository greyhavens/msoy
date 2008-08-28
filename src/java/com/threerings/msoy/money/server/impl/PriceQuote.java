//
// $Id$

package com.threerings.msoy.money.server.impl;

import com.threerings.msoy.money.data.all.MoneyType;

/**
 * Contains secured prices when a member views an item. This can be cached and identified by a
 * {@link PriceKey}.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
class PriceQuote
{
    public PriceQuote (MoneyType listedType, int coins, int bars)
    {
        _listedType = listedType;
        _coins = coins;
        _bars = bars;
    }

    public MoneyType getListedType ()
    {
        return _listedType;
    }

    public int getCoins ()
    {
        return _coins;
    }

    public int getBars ()
    {
        return _bars;
    }

    private final MoneyType _listedType;
    private final int _coins;
    private final int _bars;
}
