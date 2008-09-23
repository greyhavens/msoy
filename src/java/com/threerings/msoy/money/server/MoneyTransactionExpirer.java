//
// $Id$

package com.threerings.msoy.money.server;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Logger;

import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.ShutdownManager.Shutdowner;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;

/**
 * Manages expiration of {@link MoneyTransactionRecord}s.  Coin records should
 * be removed from the database if they are more than 10 days old (by default).
 * 
 * This does not start automatically -- call {@link #start()} to start the background
 * thread.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyTransactionExpirer
    implements Shutdowner
{
    /**
     * Starts the expirer.  By default, it will use a single-threaded scheduled executor,
     * and check once every hour for coins history records that are at least 10 days old.
     */
    public MoneyTransactionExpirer (MoneyRepository repo, Invoker invoker, ShutdownManager sm)
    {
        _repo = repo;

        sm.registerShutdowner(this);

        _interval = new Interval(invoker) {
            @Override public void expired () {
                doPurge();
            }
        };
        _interval.schedule(PURGE_INTERVAL, true);
    }
    
    // from Shutdowner
    public void shutdown ()
    {
        _interval.cancel();
    }

    /**
     * Actually do the purging.
     */
    protected void doPurge ()
    {
        int coins = _repo.deleteOldTransactions(Currency.COINS, COIN_MAX_AGE);
        int bars = _repo.deleteOldTransactions(Currency.BARS, BAR_MAX_AGE);
        int bling = _repo.deleteOldTransactions(Currency.BLING, BAR_MAX_AGE);
        if (coins > 0 || bars > 0 || bling > 0) {
            log.info("Removed old money transacion records.",
                 "coins", coins, "bars", bars, "bling", bling);
        }
    }
    
    protected static final Logger log = Logger.getLogger(MoneyTransactionExpirer.class);

    protected MoneyRepository _repo;

    protected Interval _interval;

    protected static final long COIN_MAX_AGE = 10L * 24 * 60 * 60 * 1000L; // 10 days
    protected static final long BAR_MAX_AGE = 3L * COIN_MAX_AGE; // 30 days

    protected static final long PURGE_INTERVAL = 60 * 60 * 1000; // 1 hour
}
