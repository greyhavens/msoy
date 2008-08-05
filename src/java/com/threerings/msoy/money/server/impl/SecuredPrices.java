//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.io.Serializable;

/**
 * Contains secured prices when a member views an item.  This can be cached and identified by
 * a {@link PriceKey}.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class SecuredPrices implements Serializable
{
    public SecuredPrices (final int coins, final int bars, final int creatorId, final int affiliateId, 
        final String description)
    {
        this.coins = coins;
        this.bars = bars;
        this.creatorId = creatorId;
        this.affiliateId = affiliateId;
        this.description = description;
    }

    public int getCoins ()
    {
        return coins;
    }
    
    public String getDescription ()
    {
        return description;
    }

    public int getBars ()
    {
        return bars;
    }

    public int getCreatorId ()
    {
        return creatorId;
    }

    public int getAffiliateId ()
    {
        return affiliateId;
    }
    
    private final int coins;
    private final int bars;
    private final int creatorId;
    private final int affiliateId;
    private final String description;
}
