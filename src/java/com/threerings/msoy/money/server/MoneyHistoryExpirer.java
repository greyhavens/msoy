//
// $Id$

package com.threerings.msoy.money.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Logger;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.persist.MemberAccountHistoryRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.ShutdownManager.Shutdowner;

/**
 * Manages expiration of {@link MemberAccountHistoryRecord}s.  Coin records should
 * be removed from the database if they are more than 10 days old (by default).
 * 
 * This does not start automatically -- call {@link #start()} to start the background
 * thread.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyHistoryExpirer
    implements Shutdowner
{
    /**
     * Starts the expirer.  By default, it will use a single-threaded scheduled executor,
     * and check once every hour for coins history records that are at least 10 days old.
     */
    public MoneyHistoryExpirer (final MoneyRepository repo, final ShutdownManager sm,
        final Invoker invoker)
    {
        this(repo, sm, Executors.newSingleThreadScheduledExecutor(), invoker);
    }
    
    protected MoneyHistoryExpirer (
        final MoneyRepository repo, final ShutdownManager sm,
        final ScheduledExecutorService service, final Invoker invoker)
    {
        _repo = repo;
        _invoker = invoker;
        _maxAge = 10*24*60*60*1000;     // 10 days
        _period = 60*60*1000;           // 1 hour
        _interval = null;
        sm.registerShutdowner(this);
    }
    
    /**
     * Starts the expirer.  Does nothing if already started.
     */
    public void start ()
    {
        if (_interval == null) {
            _interval = new Interval(_invoker) {
                @Override
                public void expired ()
                {
                    final int count = _repo.deleteOldHistoryRecords(Currency.COINS, _maxAge);
                    if (count > 0) {
                        log.info("Removed old member account history records for coins",
                            "count", count);
                    }
                }
            };
            _interval.schedule(_period, true);
        }
    }
    
    /**
     * Stops the expirer.  Does nothing if already stopped.
     */
    public void stop ()
    {
        if (_interval != null) {
            _interval.cancel();
            _interval = null;
        }
    }
    
    /**
     * The maximum age in milliseconds for coins history records until they are expired.
     */
    public long getMaxAge ()
    {
        return _maxAge;
    }

    /**
     * The maximum age in milliseconds for coins history records until they are expired.  The 
     * expirer must be restarted for this to have an effect.
     */
    public void setMaxAge (final long maxAge)
    {
        _maxAge = maxAge;
    }

    /**
     * Number of milliseconds between expiration task calls.
     */
    public long getPeriod ()
    {
        return _period;
    }

    /**
     * Number of milliseconds between expiration task calls.  The expirer must be restarted for 
     * this to have an effect.
     */
    public void setPeriod (final long period)
    {
        _period = period;
    }

    public void shutdown ()
    {
        stop();
    }
    
    protected static final Logger log = Logger.getLogger(MoneyHistoryExpirer.class);
    
    protected final MoneyRepository _repo;
    protected final Invoker _invoker;
    protected long _maxAge;
    protected long _period;
    protected Interval _interval;
}
