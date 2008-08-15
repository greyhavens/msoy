//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.threerings.msoy.money.server.MoneyType;
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
@Singleton
public class MoneyHistoryExpirer implements Shutdowner
{
    /**
     * Starts the expirer.  By default, it will use a single-threaded scheduled executor,
     * and check once every hour for coins history records that are at least 10 days old.
     */
    @Inject
    public MoneyHistoryExpirer (final MoneyRepository repo, final ShutdownManager sm)
    {
        this(repo, sm, Executors.newSingleThreadScheduledExecutor());
    }
    
    public MoneyHistoryExpirer (final MoneyRepository repo, final ShutdownManager sm, final ScheduledExecutorService service)
    {
        this._repo = repo;
        this._service = service;
        this._maxAge = 10*24*60*60*1000;     // 10 days
        this._period = 60*60*1000;           // 1 hour
        this._future = null;
        sm.registerShutdowner(this);
    }
    
    /**
     * Starts the expirer.  Does nothing if already started.
     */
    public void start ()
    {
        if (_future == null) {
            _future = _service.scheduleAtFixedRate(new Runnable() {
                public void run () {
                    final int count = _repo.deleteOldHistoryRecords(MoneyType.COINS, _maxAge);
                    if (count > 0) {
                        logger.info("Removed " + count + " old member account history records for coins.");
                    }
                }
            }, 0, _period, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Stops the expirer.  Does nothing if already stopped.
     */
    public void stop ()
    {
        if (_future != null) {
            _future.cancel(false);
            _future = null;
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
     * The maximum age in milliseconds for coins history records until they are expired.  The expirer must be
     * restarted for this to have an effect.
     */
    public void setMaxAge (final long maxAge)
    {
        this._maxAge = maxAge;
    }

    /**
     * Number of milliseconds between expiration task calls.
     */
    public long getPeriod ()
    {
        return _period;
    }

    /**
     * Number of milliseconds between expiration task calls.  The expirer must be restarted for this to have
     * an effect.
     */
    public void setPeriod (final long period)
    {
        this._period = period;
    }

    public void shutdown ()
    {
        _service.shutdown();
    }
    
    private static final Logger logger = Logger.getLogger(MoneyHistoryExpirer.class);
    
    private final MoneyRepository _repo;
    private final ScheduledExecutorService _service;
    private long _maxAge;
    private long _period;
    private ScheduledFuture<?> _future;
}
