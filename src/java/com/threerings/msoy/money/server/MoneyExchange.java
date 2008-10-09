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
        int exRate = (int) Math.ceil(_rate);
        switch (listedCurrency) {
        case COINS:
            // NOTE: exchange rate is a floating point number, but we round it up to the
            // nearest integer first and then divide, then round the result up to the nearest
            // int to get the bar amount.
            int bars = (int) Math.ceil(amount / (float)exRate);
            return new PriceQuote(listedCurrency, amount, bars, (bars * exRate) - amount, _rate);

        case BARS:
            // NOTE: Currently I track the exchange rate as a floating point number.
            // To generate the coin quote, we round-up the # of coins in a bar first, then
            // multiply by the number of bars.
            return new PriceQuote(listedCurrency, exRate * amount, amount, 0, _rate);

        default:
            throw new RuntimeException("Error: listing not in bars or coins?");
        }
    }

    /**
     * The specified sale has completed, factor it into the exchange rate.
     */
    public void processPurchase (PriceQuote quote, Currency purchaseCurrency, int txId)
    {
        if (purchaseCurrency == quote.getListedCurrency()) {
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
        int pool = _moneyRepo.getBarPool();
        // the more bars in the pool: the lower the exchange rate
        // TODO: asymptotic snazziness
        _rate = (BAR_POOL_TARGET * RuntimeConfig.server.targetExchangeRate) / pool;

        // If not shutting down, schedule the next recalculation, always a minute from now
        if (_recalcInterval != null) {
            _recalcInterval.schedule(RECALCULATE_INTERVAL);
        }
    }

    /** The interval to recalculate the exchange rate every minute,
     * or null if we're shutting down. */
    protected Interval _recalcInterval = new Interval() {
        public void expired () {
            recalculateRate();
        }
    };

    /** The current exchange rate. */
    protected float _rate;

    /** Our money repository. */
    @Inject protected MoneyRepository _moneyRepo;

    /** How often we re-check the exchange rate, even if no cross-currency purchases have been
     * made during this time. */
    protected static final long RECALCULATE_INTERVAL = 60 * 1000L; // every minute
}
