//
// $Id: $

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ReportingInvoker;
import com.threerings.presents.server.ShutdownManager;

import static com.threerings.msoy.Log.log;

/**
 * Invoker queue for jobs that do not necessarily execute quickly enough for the real-time
 * environment queue, e.g. collection queries and updates.
 * 
 * Do keep in mind that this queue runs on its own thread and so any data structures it shares
 * with @MainInvoker code must be accessed in a thread-safe manner. Also units moved here give
 * up all FIFO guarantees vis-a-vis units on the main invoker -- beware race conditions.
 * 
 * When the server is shutting down, the batch invoker is one of the first things to go, after
 * which it shuttles further units posted onto it to the main invoker, which contains the more
 * elegant shutdown code -- and which, at that point, is  not as vulnerable to longer-running
 * jobs.
 */
@Singleton
public class MsoyBatchInvoker extends ReportingInvoker
{
    @Inject public MsoyBatchInvoker (PresentsDObjectMgr omgr, ShutdownManager shutmgr,
            ReportManager repmgr)
    {
        super("msoy.BatchInvoker", omgr, repmgr);
    }

    @Override
    public void postUnit (Unit unit)
    {
        // when the batch invoker has been shut down, shuffle units to the main invoker
        if (shutdownRequested()) {
            log.info("Shuttling unit to main invoker", "unit", unit);
            _mainInvoker.postUnit(unit);
        } else {
            super.postUnit(unit);
        }
    }

    @Override
    public void shutdown ()
    {
        log.info("Batch invoker got shutdown request.");
        super.shutdown();
    }

    @Override
    public void didShutdown ()
    {
        log.info("Batch invoker shut down.");
        super.didShutdown();
    }

    @Inject protected @MainInvoker Invoker _mainInvoker;
}
