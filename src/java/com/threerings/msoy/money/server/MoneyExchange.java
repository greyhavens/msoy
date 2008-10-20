//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;

import com.threerings.presents.server.ShutdownManager;

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
    /** The target numbers of bars in the pool. */
    public static final int BAR_POOL_TARGET = 100000;

    @Inject public MoneyExchange (ShutdownManager shutmgr)
    {
        shutmgr.registerShutdowner(this);
        //runTests();
    }

    /**
     * Initialize the money exchange once the database is ready to roll.
     */
    public void init ()
    {
        recalculateRate();
    }

    /**
     * Get the current exchange rate, in terms of how many coins 1 bar is worth.
     */
    public float getRate ()
    {
        return _rate;
    }

    /**
     * Secure a price quote based on the current exchange rate.
     */
    public PriceQuote secureQuote (Currency listedCurrency, int amount)
    {
        switch (listedCurrency) {
        case COINS:
            // if the coin price is 0, the bar price is 0.
            // but otherwise never let the bar price get below 1.
            int bars = (amount == 0) ? 0 : Math.max(1, ((int) Math.ceil(amount / _rate)));
            return new PriceQuote(listedCurrency, amount, bars,
                (int) (Math.floor(bars * _rate) - amount), _rate);

        case BARS:
            int coins = (amount == 0) ? 0 : Math.max(1, ((int) Math.ceil(amount * _rate)));
            return new PriceQuote(listedCurrency, coins, amount,
                0, _rate);

        default:
            throw new RuntimeException("Error: listing not in bars or coins?");
        }
    }

    /**
     * The specified sale has completed, factor it into the exchange rate.
     */
    public void processPurchase (PriceQuote quote, Currency purchaseCurrency, int txId)
    {
        if (purchaseCurrency == quote.getListedCurrency() || quote.getListedAmount() == 0) {
            // the purchase was made at the listed currency, the exchange was not involved.
            return;
        }

        boolean barsCreated = (purchaseCurrency == Currency.COINS);
        // If they coin-bought, then bars were removed from the pool. Otherwise, bars
        // were added to the pool.
        int bars = quote.getBars() * (barsCreated ? -1 : 1);
        int coins = quote.getCoins() * (barsCreated ? 1 : -1);

        // record everything about the exchange
        _moneyRepo.recordExchange(bars, coins, quote.getExchangeRate(), txId);

        // immediately recalculate
        recalculateRate();
    }

    // from interface ShutdownManager.Shutdowner
    public void shutdown ()
    {
        _recalcInterval.cancel();
        _recalcInterval = null;
    }

    /**
     * Recalculate the exchange rate.
     */
    protected void recalculateRate ()
    {
        int pool = _moneyRepo.getBarPool()[0];
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
    protected void calculateRate (int pool)
    {
        if (pool <= 0) {
            _rate = Float.POSITIVE_INFINITY;

        } else if (pool >= (BAR_POOL_TARGET * 2)) {
            _rate = 0;

        } else if (pool >= BAR_POOL_TARGET) {
            float x = 1 - ((pool - BAR_POOL_TARGET) / ((float) BAR_POOL_TARGET));
            _rate = (RuntimeConfig.money.targetExchangeRate / (1 / x));

        } else {
            float x = pool / ((float) BAR_POOL_TARGET);
            _rate = (RuntimeConfig.money.targetExchangeRate * (1 / x));
        }
    }

    /** The interval to recalculate the exchange rate every minute,
     * or null if we're shutting down. */
    protected Interval _recalcInterval = new Interval() {
        public void expired () {
            recalculateRate();
        }
    };

    protected void runTests ()
    {
//        System.err.println("Rate 0: " + calcRate((int) (0.0 * BAR_POOL_TARGET)));
//        System.err.println("Rate .125: " + calcRate((int) (0.125 * BAR_POOL_TARGET)));
//        System.err.println("Rate .25: " + calcRate((int) (.25 * BAR_POOL_TARGET)));
//        System.err.println("Rate .50: " + calcRate((int) (.5 * BAR_POOL_TARGET)));
//        System.err.println("Rate .75: " + calcRate((int) (.75 * BAR_POOL_TARGET)));
//        System.err.println("Rate 1.0: " + calcRate((int) (1.0 * BAR_POOL_TARGET)));
//        System.err.println("Rate 1.25: " + calcRate((int) (1.25 * BAR_POOL_TARGET)));
//        System.err.println("Rate 1.5: " + calcRate((int) (1.5 * BAR_POOL_TARGET)));
//        System.err.println("Rate 1.75: " + calcRate((int) (1.75 * BAR_POOL_TARGET)));
//        System.err.println("Rate 2.0: " + calcRate((int) (2.0 * BAR_POOL_TARGET)));
//
//        System.err.println("maxinf casted: " + ((int) (Float.POSITIVE_INFINITY * 2)));

        System.err.println("Draining bar pool...");
        calculateRate(0);
        testPrices();

        System.err.println("Overfilling bar pool...");
        calculateRate(2 * BAR_POOL_TARGET);
        testPrices();

        System.err.println("Half-overfilling bar pool...");
        calculateRate((int) (1.5 * BAR_POOL_TARGET));
        testPrices();

        System.err.println("Half-filling bar pool...");
        calculateRate((int) (.5 * BAR_POOL_TARGET));
        testPrices();
    }

    protected void testPrices ()
    {
        PriceQuote p;
        p = secureQuote(Currency.COINS, 0);
        System.err.println("coins:0, bars: " + p.getBars());
        p = secureQuote(Currency.COINS, 1);
        System.err.println("coins:1, bars: " + p.getBars());
        p = secureQuote(Currency.COINS, 1000000);
        System.err.println("coins:1000000, bars: " + p.getBars());

        p = secureQuote(Currency.BARS, 0);
        System.err.println("bars:0, coins: " + p.getCoins());
        p = secureQuote(Currency.BARS, 1);
        System.err.println("bars:1, coins: " + p.getCoins());
        p = secureQuote(Currency.BARS, 1000000);
        System.err.println("bars:1000000, coins: " + p.getCoins());
    }

    /** The current exchange rate. Can vary from 0 to Float.POSITIVE_INFINITY. */
    protected float _rate;

    /** Our money repository. */
    @Inject protected MoneyRepository _moneyRepo;

    /** How often we re-check the exchange rate, even if no cross-currency purchases have been
     * made during this time. */
    protected static final long RECALCULATE_INTERVAL = 60 * 1000L; // every minute
}
