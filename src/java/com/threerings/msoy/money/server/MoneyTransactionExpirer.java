//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.msoy.server.persist.BatchInvoker;

import static com.threerings.msoy.Log.log;

/**
 * Manages expiration of {@link MoneyTransactionRecord}s.  Coin records should
 * be removed from the database if they are more than 10 days old (by default).
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyTransactionExpirer
    implements Lifecycle.Component
{
    /**
     * Starts the expirer.  By default, it will use a single-threaded scheduled executor,
     * and check once every hour for coins history records that are at least 10 days old.
     */
    @Inject public MoneyTransactionExpirer (Lifecycle cycle)
    {
        cycle.addComponent(this);

        // note: this Interval doesn't post to the omgr: it doesn't need to.
        _interval = Interval.create(Interval.RUN_DIRECT, new Runnable() {
            public void run () {
                _batchInvoker.postUnit(new Invoker.Unit("MoneyTransactionExpirer") {
                    @Override public boolean invoke () {
                        doPurge();
                        return false;
                    }
                    @Override public long getLongThreshold () {
                        return 10 * 1000;
                    }
                });
            }
        });
    }

    // from interface Lifecycle.Component
    public void init ()
    {
        _interval.schedule(PURGE_INTERVAL, true);
    }

    // from interface Lifecycle.Component
    public void shutdown ()
    {
        _interval.cancel();
    }

    /**
     * Actually do the purging.
     */
    protected void doPurge ()
    {
        int coins, bars, bling;

        if (COIN_MAX_AGE > 0) {
            coins = _repo.deleteOldTransactions(Currency.COINS, COIN_MAX_AGE);
        } else {
            coins = 0;
        }
        if (BAR_MAX_AGE > 0) {
            bars = _repo.deleteOldTransactions(Currency.BARS, BAR_MAX_AGE);
            bling = _repo.deleteOldTransactions(Currency.BLING, BAR_MAX_AGE);
        } else {
            bars = bling = 0;
        }
        if (coins > 0 || bars > 0 || bling > 0) {
            log.info("Removed old money transaction records.",
                "coins", coins, "bars", bars, "bling", bling);
        }
    }

    protected final Interval _interval;

    // dependencies
    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected MoneyRepository _repo;

    protected static final long DAY = 24L * 60 * 60 * 1000L; // the length of 99.4% of days
    protected static final long COIN_MAX_AGE = 10L * DAY; // 10 days
    protected static final long BAR_MAX_AGE = -1L; // never expire

    protected static final long PURGE_INTERVAL = 60 * 60 * 1000; // 1 hour
}
