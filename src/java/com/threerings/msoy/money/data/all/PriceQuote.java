//
// $Id$

package com.threerings.msoy.money.data.all;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Contains secured prices when a member views an item. This can be cached and identified by a
 * {@link PriceKey}.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
public class PriceQuote extends SimpleStreamableObject
{
    public PriceQuote (Currency listedCurrency, int coins, int bars, int coinChangeForBars)
    {
        _listedCurrency = listedCurrency;
        _coins = coins;
        _bars = bars;
        _coinChange = coinChangeForBars;
    }

    /**
     * Does the "buy" amount from the client work?
     */
    public boolean isSatisfied (Currency buyCurrency, int buyAmount)
    {
        return (buyAmount >= getAmount(buyCurrency));
    }

    public Currency getListedCurrency ()
    {
        return _listedCurrency;
    }

    /**
     * Get the listed amount, be it coins or bars.
     */
    public int getListedAmount ()
    {
        return getAmount(_listedCurrency);
    }

    public int getAmount (Currency currency)
    {
        return (currency == Currency.BARS) ? _bars : _coins;
    }

    public int getCoins ()
    {
        return _coins;
    }

    public int getBars ()
    {
        return _bars;
    }

    /**
     * Retrieve the amount of change, in coins, when bar-buying a coin-listed item.
     * If the listedType is BARS, this will always be 0.
     * Currently, Three Rings eats this change, nom nom nom, but we could conceiveably
     * pay it out to the user.
     */
    public int getCoinChange ()
    {
        return _coinChange;
    }

    /* For debugging */
    public String toString ()
    {
        return "PriceQuote[" + _listedCurrency + "-listed]: " +
            _coins + "coins, " + _bars + "bars.";
    }

    protected final Currency _listedCurrency;
    protected final int _coins;
    protected final int _bars;
    protected final int _coinChange;
}
