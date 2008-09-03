//
// $Id$

package com.threerings.msoy.money.server;

import net.jcip.annotations.Immutable;

/**
 * Configuration of the money service.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@Immutable
public class MoneyConfiguration
{
    /**
     * @param securePriceDurtion in minutes.
     */
    public MoneyConfiguration (
        float creatorPercentage, float affiliatePercentage,
        float blingCashoutTransactionFee,
        int securePriceDuration, int maxSecuredPrices)
    {
        _creatorPercentage = creatorPercentage;
        _affiliatePercentage = affiliatePercentage;
        _blingCashoutTransactionFee = blingCashoutTransactionFee;
        _securePriceDuration = securePriceDuration;
        _maxSecuredPrices = maxSecuredPrices;
    }

    /**
     * The percentage of the purchase price that will be awarded to the creator of an
     * item when it is bought.
     */
    public float getCreatorPercentage ()
    {
        return _creatorPercentage;
    }

    /**
     * The percentage of the purchase price that will be awarded to the affiliate of the
     * user who bought the item.
     */
    public float getAffiliatePercentage ()
    {
        return _affiliatePercentage;
    }

    /**
     * The length of time, in minutes, that a secured price will remain available.
     */
    public int getSecurePriceDuration ()
    {
        return _securePriceDuration;
    }

    /**
     * The amount to deduct when cashing out bling into real money.
     */
    public float getBlingCashoutTransactionFee ()
    {
        return _blingCashoutTransactionFee;
    }

    /**
     * The maximum number of prices that can be secured in this node.
     */
    public int getMaxSecuredPrices ()
    {
        return _maxSecuredPrices;
    }

    private final float _creatorPercentage;
    private final float _affiliatePercentage;
    private final float _blingCashoutTransactionFee;
    private final int _securePriceDuration;
    private final int _maxSecuredPrices;
}
