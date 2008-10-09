//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

import com.threerings.msoy.money.server.persist.MoneyRepository;

/**
 * Handles exchanges between coins and bars as part of a purchase.
 */
// TODO: register as a shutdowner, stop recalculation???
@Singleton
public class MoneyExchange
{
    /** The target numbers of bars in the pool. */
    public static final int BAR_POOL_TARGET = 100000;

    /**
     * Initialize the money exchange.
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
    public void processPurchase (PriceQuote quote, Currency purchaseCurrency)
    {
        if (purchaseCurrency == quote.getListedCurrency()) {
            // the purchase was made at the listed currency, the exchange was not involved.
            return;
        }

        // If they coin-bought, then bars were removed from the pool. Otherwise, bars
        // were added to the pool.
        _moneyRepo.adjustBarPool(quote.getBars() *
            ((purchaseCurrency == Currency.COINS) ? -1 : 1));

        // immediately recalculate
        recalculateRate();
    }

    /**
     * Recalculate the exchange rate.
     */
    protected void recalculateRate ()
    {
        int pool = _moneyRepo.getBarPool();
        // the more bars in the pool: the lower the exchange rate
        // TODO: asymptotic snazziness
        _rate = (BAR_POOL_TARGET * EXPECTED_RATE) / pool;

        // schedule the next recalculation, always a minute from now
        _recalcInterval.schedule(RECALCULATE_INTERVAL);
    }

    /** The interval to recalculate the exchange rate every minute. */
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

    /** Our initial guess at an exchange rate.
     *
     * Value / time, from Puzzle Pirates: $0.25 / hr
     * hourly coin rate = 3000
     * therefore 3000 coins = $.25
     * bars are valued at $.10
     * therefore 1 bar = 1200 coins
     */
    protected static final float EXPECTED_RATE = 1200;
}
