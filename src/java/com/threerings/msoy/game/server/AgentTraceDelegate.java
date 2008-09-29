//
// $Id$

package com.threerings.msoy.game.server;

import com.google.common.base.Preconditions;

import com.google.inject.Inject;

import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.server.PlaceManagerDelegate;

import com.threerings.presents.annotation.MainInvoker;

import com.threerings.msoy.avrg.server.AVRGameManager;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.GameTraceLogRecord;

import static com.threerings.msoy.Log.log;

/**
 * Handles the recording of game agent trace logs and writing them to the database when the game
 * has ended. If specified, log traces may also be written periodically.
 */
public class AgentTraceDelegate extends PlaceManagerDelegate
{
    /** The string we append to an agent's trace log that's about to exceed the limit. */
    public static String TRACE_CAP = "--- buffer full ---\n";

    /** The maximum length of the user portion of a game log entry. Space is reserved for the
     * cap message. */
    public static int MAX_USER_LENGTH = 
        GameTraceLogRecord.getMaximumLogLength() - TRACE_CAP.length();

    /**
     * Creates a new delegate that stores once on shutdown.
     * @param gameId the id of the game we are storing agent traces for
     */
    public AgentTraceDelegate (int gameId)
    {
        this(gameId, 0, 0);
    }

    /**
     * Creates a new delegate that stores traces periodically. The given intervals control when the
     * trace contents are recorded to the database and when truncation occurs. In no instance will
     * the size limit {@link #MAX_USER_LENGTH} be exceeded.
     * @param gameId the id of the game we are storing agent traces for
     * @param minInterval allow at least this many minutes between storing traces
     * @param maxInterval never allow more than this many minutes betweens storing traces
     */
    public AgentTraceDelegate (int gameId, int minInterval, int maxInterval)
    {
        Preconditions.checkArgument(minInterval >= 0 && maxInterval >= minInterval,
            "Bad intervals, must have 0 <= min <= max");
        
        _gameId = gameId;
        
        // Convert to ms
        _minInterval = minInterval * 60 * 1000;
        _maxInterval = maxInterval * 60 * 1000;
    }

    /**
     * Called by {@link MsoyGameManager} when it receives a trace request.
     */
    public void recordAgentTrace (String trace)
    {
        // Sanity check: bail if the string itself is too large
        if (trace.length() > MAX_USER_LENGTH / 2) {
            log.warning("Agent trace line too long", "length", trace.length());
            return;

        }
        
        // Bail if we have previously truncated
        if (!_tracing) {
            return;
        }

        // Check if this string will fit in the remaining space
        boolean willFit = _traceBuffer.length() + trace.length() + 1 < MAX_USER_LENGTH;
        
        // If not and we are above our minimum interval, ship it off to the DB
        if (!willFit && isPeriodic() && 
            (System.currentTimeMillis() - _lastStoreTime) > _minInterval) {
            storeTrace();
            willFit = true;
        }
        
        // Append the string if it will fit, otherwise append cap and start truncation
        if (willFit) {
            _traceBuffer.append(trace).append("\n");

        } else {
            _traceBuffer.append(TRACE_CAP);
            _tracing = false;
        }
    }

    /**
     * Called by {@link AVRGameManager} when it receives a batch of trace requests.
     */
    public void recordAgentTrace (String[] traces)
    {
        if (_tracing) {
            for (String trace : traces) {
                recordAgentTrace(trace);
            }
        }
    }

    @Override // from PlaceManagerDelegate
    public void didStartup (PlaceObject plobj)
    {
        super.didStartup(plobj);
        
        // Setup the fancy scheduling stuff if requested
        if (isPeriodic()) {
            _storeInterval = new Interval(_omgr) {
                public void expired () {
                    maybeStoreTrace();
                }
            };
            _lastStoreTime = System.currentTimeMillis();
            _storeInterval.schedule(_minInterval, true);
        }
    }

    @Override // from PlaceManagerDelegate
    public void didShutdown ()
    {
        super.didShutdown();
        storeTrace();
        if (isPeriodic()) {
            _storeInterval.cancel();
        }
    }

    /**
     * Checks if it's time to write out a trace to the database, and if so does it.
     */
    protected void maybeStoreTrace ()
    {
        int t = (int)(System.currentTimeMillis() - _lastStoreTime);

        // Just in case we are out of sync
        if (t < _minInterval) {
            _storeInterval.schedule(_minInterval - t);
            return;
        }
        
        // Over the maximum, do the store
        if (t >= _maxInterval) {
            storeTrace();
            return;
        }
        
        // Retry when max. is hit (this may be superceded if the buffer hits the limit before then)
        _storeInterval.schedule(_maxInterval - t);
    }

    /**
     * Stores the current trace to the database and resets the buffer and scheduling stuff.
     */
    protected void storeTrace ()
    {
        // if there were agent traces, flush them to the database
        if (_traceBuffer.length() > 0) {
            final String trace = _traceBuffer.toString();
            _invoker.postUnit(new RepositoryUnit("storeAgentTrace(" + _gameId + ")") {
                public void invokePersist () throws Exception {
                    _gameRepo.storeTraceLog(_gameId, trace);
                }
                public void handleSuccess () {
                    // good
                }
            });
        }
        
        // Prepare to receive more input
        _tracing = true;
        _traceBuffer.setLength(0);
        if (isPeriodic()) {
            _lastStoreTime = System.currentTimeMillis();
            _storeInterval.schedule(_minInterval);
        }
    }

    /**
     * Tests if traces are being written to the database periodically.
     */
    protected boolean isPeriodic ()
    {
        return _minInterval > 0;
    }
    
    /** The id of the game for which we're logging traces. */
    protected int _gameId;
    
    /** Minimum interval to allow to pass before storing a trace. */
    protected int _minInterval;
    
    /** Maximum interval to allow to pass before storing a trace. */
    protected int _maxInterval;

    /** Last time a trace was stored. */
    protected long _lastStoreTime;

    /** Task for storing traces. */
    protected Interval _storeInterval;
    
    /** Accumulates up to {@link #MAX_USER_LENGTH} bytes of trace data from this game's Agent, 
     * if any. */
    protected StringBuilder _traceBuffer = new StringBuilder();

    /** Determines whether we're still tracing or if we've bumped into the cap. */
    protected boolean _tracing = true;

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MsoyGameRepository _gameRepo;
}
