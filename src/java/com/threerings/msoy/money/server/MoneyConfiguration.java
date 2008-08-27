//
// $Id$

package com.threerings.msoy.money.server;

import net.jcip.annotations.Immutable;

/**
 * Configuration of the money service.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Immutable
public class MoneyConfiguration
{
    public MoneyConfiguration (
        final double creatorKickback, final long securePriceDuration,
        final double coinsToBarsWeightValue, final double blingCashoutTransactionFee,
        final int maxSecuredPrices)
    {
        _creatorKickback = creatorKickback;
        _securePriceDuration = securePriceDuration;
        _coinsToBarsWeightValue = coinsToBarsWeightValue;
        _blingCashoutTransactionFee = blingCashoutTransactionFee;
        _maxSecuredPrices = maxSecuredPrices;
    }

    /**
     * The percentage of the purchase price that will be awarded to the creator of an
     * item when it is bought.
     */
    public double getCreatorKickback ()
    {
        return _creatorKickback;
    }

    /**
     * The length of time, in milliseconds, that a secured price will remain available.
     */
    public long getSecurePriceDuration ()
    {
        return _securePriceDuration;
    }

    /**
     * The number of coins equal to one bar, assuming the market value of 1 bar = 1 coin.
     */
    public double getCoinsToBarsWeightValue ()
    {
        return _coinsToBarsWeightValue;
    }

    /**
     * The amount to deduct when cashing out bling into real money.
     */
    public double getBlingCashoutTransactionFee ()
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

    private final double _creatorKickback;
    private final long _securePriceDuration;
    private final int _maxSecuredPrices;
    private final double _coinsToBarsWeightValue;
    private final double _blingCashoutTransactionFee;
}
