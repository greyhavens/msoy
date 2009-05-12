//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.AnyThread;
import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.server.ShutdownManager;

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
    implements ShutdownManager.Shutdowner
{
    @Inject public MoneyExchange (ShutdownManager shutmgr)
    {
        shutmgr.registerShutdowner(this);
        //runTests();
    }

    /**
     * Initialize the money exchange once the database is ready to roll.
     */
    @BlockingThread
    public void init ()
    {
        // create the recalculating interval
        _recalcInterval = new Interval(_invoker) {
            public void expired () {
                recalculateRate();
            }
        };

        recalculateRate();
        _runtime.money.addListener(_moneyListener);
    }

    /**
     * Get the current exchange rate, in terms of how many coins 1 bar is worth.
     */
    @AnyThread
    public float getRate ()
    {
        return _rate;
    }

    /**
     * Secure a price quote based on the current exchange rate.
     */
    @AnyThread
    public PriceQuote secureQuote (Currency listedCurrency, int amount, boolean allowExchange)
    {
        float rate = _rate; // lock in the rate for the remainder of this method
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
        return coinsToBars(coins, _rate);
    }

    /**
     * Get the current coin price of the specified bar price.
     */
    @AnyThread
    public int barsToCoins (int bars)
    {
        return barsToCoins(bars, _rate);
    }

    /**
     * Get a coin cost such that it converts back to the same number of bars when a quote
     * is made from it. Well, I just need this, and that's what I need it for, but some
     * renaming of this method and possible other cleanup would be great.
     */
    @AnyThread
    public int barsToCoinsFloor (int bars)
    {
        return barsToCoinsFloor(bars, _rate);
    }

    /**
     * Get the coin change for the specified prices.
     */
    @AnyThread
    public int coinChange (int coinPrice, int barPrice)
    {
        return coinChange(coinPrice, barPrice, _rate);
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

        // record everything about the exchange
        _moneyRepo.recordExchange(bars, coins, quote.getExchangeRate(), txId);

        // immediately recalculate
        recalculateRate();
    }

    // from interface ShutdownManager.Shutdowner
    @EventThread
    public void shutdown ()
    {
        _recalcInterval.cancel();
        _recalcInterval = null;
    }

    /**
     * Recalculate the exchange rate.
     */
    @BlockingThread
    protected void recalculateRate ()
    {
        int pool = _moneyRepo.getBarPool(_runtime.money.barPoolSize)[0];
        // the more bars in the pool: the lower the exchange rate
        calculateRate(pool);

        // If not shutting down, schedule the next recalculation, always a minute from now
        if (_recalcInterval != null) {
            _recalcInterval.schedule(RECALCULATE_INTERVAL);
        }
    }

    /**
     * Calculate the rate based on the number of bars in the pool. The more bars: the lower
     * the exchange rate.
     */
    @AnyThread
    protected void calculateRate (int pool)
    {
        int barPoolTarget = _runtime.money.barPoolSize;
        if (pool <= 0) {
            _rate = Float.POSITIVE_INFINITY;

        } else if (pool >= (barPoolTarget * 2)) {
            _rate = 0;

        } else if (pool >= barPoolTarget) {
            float x = 1 - ((pool - barPoolTarget) / ((float) barPoolTarget));
            _rate = (_runtime.money.targetExchangeRate / (1 / x));

        } else {
            float x = pool / ((float) barPoolTarget);
            _rate = (_runtime.money.targetExchangeRate * (1 / x));
        }
    }

    /**
     * Adjust the desired bar pool size. This is called in direct reaction to adjusting
     * the runtime config.
     */
    @BlockingThread
    protected void adjustDesiredBarPool (int delta)
    {
        _moneyRepo.adjustBarPool(delta);
        recalculateRate();
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

//    protected void runTests ()
//    {
//        final int barPoolTarget = _runtime.money.barPoolSize;
////        System.err.println("Rate 0: " + calcRate((int) (0.0 * barPoolTarget)));
////        System.err.println("Rate .125: " + calcRate((int) (0.125 * barPoolTarget)));
////        System.err.println("Rate .25: " + calcRate((int) (.25 * barPoolTarget)));
////        System.err.println("Rate .50: " + calcRate((int) (.5 * barPoolTarget)));
////        System.err.println("Rate .75: " + calcRate((int) (.75 * barPoolTarget)));
////        System.err.println("Rate 1.0: " + calcRate((int) (1.0 * barPoolTarget)));
////        System.err.println("Rate 1.25: " + calcRate((int) (1.25 * barPoolTarget)));
////        System.err.println("Rate 1.5: " + calcRate((int) (1.5 * barPoolTarget)));
////        System.err.println("Rate 1.75: " + calcRate((int) (1.75 * barPoolTarget)));
////        System.err.println("Rate 2.0: " + calcRate((int) (2.0 * barPoolTarget)));
////
////        System.err.println("maxinf casted: " + ((int) (Float.POSITIVE_INFINITY * 2)));
//
//        System.err.println("Draining bar pool...");
//        calculateRate(0);
//        testPrices();
//
//        System.err.println("Overfilling bar pool...");
//        calculateRate(2 * barPoolTarget);
//        testPrices();
//
//        System.err.println("Half-overfilling bar pool...");
//        calculateRate((int) (1.5 * barPoolTarget));
//        testPrices();
//
//        System.err.println("Half-filling bar pool...");
//        calculateRate((int) (.5 * barPoolTarget));
//        testPrices();
//    }
//
//    protected void testPrices ()
//    {
//        PriceQuote p;
//        p = secureQuote(Currency.COINS, 0);
//        System.err.println("coins:0, bars: " + p.getBars());
//        p = secureQuote(Currency.COINS, 1);
//        System.err.println("coins:1, bars: " + p.getBars());
//        p = secureQuote(Currency.COINS, 1000000);
//        System.err.println("coins:1000000, bars: " + p.getBars());
//
//        p = secureQuote(Currency.BARS, 0);
//        System.err.println("bars:0, coins: " + p.getCoins());
//        p = secureQuote(Currency.BARS, 1);
//        System.err.println("bars:1, coins: " + p.getCoins());
//        p = secureQuote(Currency.BARS, 1000000);
//        System.err.println("bars:1000000, coins: " + p.getCoins());
//    }

    /** The interval to recalculate the exchange rate every minute,
     * (because transactions can take place on other peers)
     * or null if we're shutting down. */
    protected Interval _recalcInterval;

    /** Listens for changes to the desired bar pool size and makes adjustments as necessary. */
    protected AttributeChangeListener _moneyListener = new AttributeChangeListener() {
        @EventThread
        public void attributeChanged (AttributeChangedEvent event)
        {
            String name = event.getName();
            if (MoneyConfigObject.BAR_POOL_SIZE.equals(name)) {
                if (-1 == event.getSourceOid()) {
                    // for server-originated changes, do no validation, just recompute our rate
                    _recalcInterval.schedule(0);
                    return;
                }

                // otherwise, make sure the target bar pool is not below 1
                int newValue = event.getIntValue();
                int oldValue = (Integer) event.getOldValue();
                if (newValue < 1) {
                    _runtime.money.setBarPoolSize(Math.max(1, oldValue)); // rollback to old value

                } else {
                    // it's a normal, valid adjustment
                    final int adjustmentSize = newValue - oldValue;
                    _invoker.postRunnable(new Runnable() {
                        public void run () {
                            adjustDesiredBarPool(adjustmentSize);
                        }
                    });
                }

            } else if (MoneyConfigObject.TARGET_EXCHANGE_RATE.equals(name)) {
                _recalcInterval.schedule(0);
            }
        }
    };

    /** The current exchange rate. Can vary from 0 to Float.POSITIVE_INFINITY. */
    protected float _rate;

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MoneyRepository _moneyRepo;
    @Inject protected RuntimeConfig _runtime;

    /** How often we re-check the exchange rate, even if no cross-currency purchases have been
     * made during this time. */
    protected static final long RECALCULATE_INTERVAL = 60 * 1000L; // every minute
}
