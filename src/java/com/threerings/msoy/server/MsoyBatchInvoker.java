//
// $Id: $

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ReportingInvoker;
import com.threerings.presents.server.ShutdownManager;

/**
 * Invoker queue for jobs that do not necessarily execute quickly enough for the real-time
 * environment queue, e.g. collection queries and updates.
 * 
 * Do keep in mind that this queue runs on its own thread and so any data structures it shares
 * with @MainInvoker code must be accessed in a thread-safe manner. Also units moved here give
 * up all FIFO guarantees vis-ˆ-vis units on the main invoker -- beware race conditions.
 */
@Singleton
public class MsoyBatchInvoker extends ReportingInvoker
{
    @Inject public MsoyBatchInvoker (PresentsDObjectMgr omgr, ShutdownManager shutmgr,
            ReportManager repmgr)
    {
        super("msoy.BatchInvoker", omgr, repmgr);
    }
}
