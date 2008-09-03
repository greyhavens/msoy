//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * Handles money exchanges.
 */
// TODO: this is mostly placeholder, the final service may look different.
//
public class MoneyExchange
{
    /**
     * Secure a price quote based on the current exchange rate.
     */
    public static PriceQuote secureQuote (Currency listedCurrency, int amount)
    {
        int exRate = (int) Math.ceil(_exchangeRate);
        switch (listedCurrency) {
        case COINS:
            int bars = (int) Math.ceil(amount / (float)exRate);
            return new PriceQuote(listedCurrency, amount, bars, (bars * exRate) - amount);

        case BARS:
            // NOTE: Currently I track the exchange rate as a floating point number.
            // To generate the coin quote, we round-up the # of coins in a bar first, then
            // multiply by the number of bars.
            return new PriceQuote(listedCurrency, exRate * amount, amount, 0);

        default:
            throw new RuntimeException("Error: listing not in bars or coins?");
        }
    }

    /**
     * The specified sale has completed, factor it into the exchange rate.
     */
    public static void processPurchase (PriceQuote quote, Currency purchaseCurrency)
    {
        // TEMPorary implementation
        if (purchaseCurrency != quote.getListedCurrency()) {
            // if they coin-purchase something bar-listed, the value of bars goes up
            _exchangeRate *= (purchaseCurrency == Currency.COINS) ? 1.01 : .99;
        }
    }

    /**
     * Get the current exchange rate, in terms of how many coins 1 bar is worth.
     */
    // depending on our implementation, maybe we end up exposing this..
    protected static float getExchangeRate ()
    {
        // TEMPorary implementation
        return _exchangeRate;
    }

    // TEMPorary implmentation: the number of coins in each bar
    protected static float _exchangeRate = 1000;
}
