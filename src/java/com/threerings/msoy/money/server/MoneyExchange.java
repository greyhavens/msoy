//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;

import com.threerings.presents.annotation.AnyThread;
import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

import com.threerings.msoy.money.server.persist.MoneyRepository;

/**
 * Handles exchanges between coins and bars as part of a purchase.
 */
@Singleton
public class MoneyExchange
    implements Lifecycle.Component
{
    @Inject public MoneyExchange (Lifecycle cycle)
    {
        cycle.addComponent(this);
    }

    /**
     * Get the current exchange rate, in terms of how many coins 1 bar is worth.
     */
    @AnyThread
    public float getRate ()
    {
        return _runtime.money.targetExchangeRate;
    }

    /**
     * Secure a price quote based on the current exchange rate.
     */
    @AnyThread
    public PriceQuote secureQuote (Currency listedCurrency, int amount, boolean allowExchange)
    {
        float rate = getRate(); // lock in the rate for the remainder of this method
        switch (listedCurrency) {
        case COINS:
            int bars = allowExchange ? coinsToBars(amount, rate) : -1;
            int change = allowExchange ? coinChange(amount, bars, rate) : 0;
            return new PriceQuote(
                listedCurrency, amount, bars, change, rate, _runtime.money.barCost);

        case BARS:
            int coins = allowExchange ? barsToCoins(amount, rate) : -1;
            return new PriceQuote(listedCurrency, coins, amount, 0, rate, _runtime.money.barCost);

        default:
            throw new RuntimeException("Error: listing not in bars or coins?");
        }
    }

    /**
     * Get the current bar price of the specified coin price.
     */
    @AnyThread
    public int coinsToBars (int coins)
    {
        return coinsToBars(coins, getRate());
    }

    /**
     * Get the current coin price of the specified bar price.
     */
    @AnyThread
    public int barsToCoins (int bars)
    {
        return barsToCoins(bars, getRate());
    }

    /**
     * Get a coin cost such that it converts back to the same number of bars when a quote
     * is made from it. Well, I just need this, and that's what I need it for, but some
     * renaming of this method and possible other cleanup would be great.
     */
    @AnyThread
    public int barsToCoinsFloor (int bars)
    {
        return barsToCoinsFloor(bars, getRate());
    }

    /**
     * Get the coin change for the specified prices.
     */
    @AnyThread
    public int coinChange (int coinPrice, int barPrice)
    {
        return coinChange(coinPrice, barPrice, getRate());
    }

    /**
     * The specified sale has completed, factor it into the exchange rate.
     */
    @BlockingThread
    public void processPurchase (PriceQuote quote, Currency purchaseCurrency, int txId)
    {
        if (purchaseCurrency == quote.getListedCurrency() || quote.getListedAmount() == 0) {
            // the purchase was made at the listed currency, the exchange was not involved.
            return;
        }

        int bars;
        int coins;
        // If they coin-bought, then bars were removed from the pool. Otherwise, bars
        // were added to the pool.
        if (purchaseCurrency == Currency.COINS) {
            bars = -1 * quote.getBars();
            coins = quote.getCoins();

        } else {
            bars = quote.getBars();
            coins = -1 * (quote.getCoins() + quote.getCoinChange());
        }
    }

    // from interface Lifecycle.Component
    public void init ()
    {
    }

    // from interface Lifecycle.Component
    public void shutdown ()
    {
    }

    /**
     * Calculate the rate based on the number of bars in the pool. The more bars: the lower
     * the exchange rate.
     */
    @AnyThread
    protected void calculateRate (int pool)
    {
    }

    @AnyThread
    protected static int coinsToBars (int coins, float rate)
    {
        // if the coin price is 0, the bar price is 0.
        // but otherwise never let the bar price get below 1.
        return (coins == 0) ? 0 : Math.max(1, ((int) Math.ceil(coins / rate)));
    }

    @AnyThread
    protected static int barsToCoins (int bars, float rate)
    {
        return (bars == 0) ? 0 : Math.max(1, ((int) Math.ceil(bars * rate)));
    }

    @AnyThread
    protected static int barsToCoinsFloor (int bars, float rate)
    {
        return (bars == 0) ? 0 : (int)Math.floor(bars * rate);
    }

    @AnyThread
    protected static int coinChange (int coinPrice, int barPrice, float rate)
    {
        return (int) (Math.floor(barPrice * rate) - coinPrice);
    }

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MoneyRepository _moneyRepo;
    @Inject protected RuntimeConfig _runtime;
}
