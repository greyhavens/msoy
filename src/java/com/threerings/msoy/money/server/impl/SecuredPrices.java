//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.io.Serializable;

import com.threerings.msoy.money.data.all.MoneyType;

/**
 * Contains secured prices when a member views an item. This can be cached and identified by a
 * {@link PriceKey}.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
class SecuredPrices
    implements Serializable
{
    public SecuredPrices (
        final MoneyType listedType, final int coins, final int bars,
        final int creatorId, final int affiliateId, final String description)
    {
        _coins = coins;
        _bars = bars;
        _creatorId = creatorId;
        _affiliateId = affiliateId;
        _description = description;
        _listedType = listedType;
    }

    public int getCoins ()
    {
        return _coins;
    }

    public String getDescription ()
    {
        return _description;
    }

    public int getBars ()
    {
        return _bars;
    }

    public int getCreatorId ()
    {
        return _creatorId;
    }

    public int getAffiliateId ()
    {
        return _affiliateId;
    }

    public MoneyType getListedType ()
    {
        return _listedType;
    }

    private final int _coins;
    private final int _bars;
    private final int _creatorId;
    private final int _affiliateId;
    private final String _description;
    private final MoneyType _listedType;
}
