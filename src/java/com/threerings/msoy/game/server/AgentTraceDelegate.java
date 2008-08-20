//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.Invoker;

import com.threerings.crowd.server.PlaceManagerDelegate;

import com.threerings.presents.annotation.MainInvoker;

import com.threerings.msoy.item.server.persist.GameRepository;

/**
 * Handles the recording of game agent trace logs and writing them to the database when the game
 * has ended.
 */
public class AgentTraceDelegate extends PlaceManagerDelegate
{
    /** The string we append to an agent's trace log that's about to exceed 64K. */
    public static String TRACE_CAP = "--- buffer full ---";

    public AgentTraceDelegate (int gameId)
    {
        _gameId = gameId;
    }

    /**
     * Called by {@link MsoyGameManager} when it receives a trace request.
     */
    public void recordAgentTrace (String trace)
    {
        if (_tracing) {
            // the first line we see that would exceed 64K turns off tracing
            if (_traceBuffer.length() + trace.length() + 1 + TRACE_CAP.length() + 1 < 65535) {
                _traceBuffer.append(trace).append("\n");
            } else {
                _traceBuffer.append(TRACE_CAP).append("\n");
                _tracing = false;
            }
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

    @Override
    public void didShutdown ()
    {
        super.didShutdown();

        // if there were agent traces, flush them to the database
        if (_traceBuffer.length() > 0) {
            _invoker.postUnit(new RepositoryUnit("storeAgentTrace(" + _gameId + ")") {
                public void invokePersist () throws Exception {
                    _gameRepo.storeTraceLog(_gameId, _traceBuffer.toString());
                }
                public void handleSuccess () {
                    // good
                }
            });
        }
    }

    /** The id of the game for which we're logging traces. */
    protected int _gameId;

    /** Accumulates up to 64K bytes of trace data from this game's Agent, if any. */
    protected StringBuilder _traceBuffer = new StringBuilder();

    /** Determines whether we're still tracing or if we've bumped into the cap. */
    protected boolean _tracing = true;

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected GameRepository _gameRepo;
}
