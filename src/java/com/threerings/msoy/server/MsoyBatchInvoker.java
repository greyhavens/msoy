//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.PresentsInvoker;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ReportingInvoker;

/**
 * Invoker queue for jobs that do not necessarily execute quickly enough for the real-time
 * environment queue, e.g. collection queries and updates.
 *
 * <p> Do keep in mind that this queue runs on its own thread and so any data structures it shares
 * with @MainInvoker code must be accessed in a thread-safe manner. Also units moved here give up
 * all FIFO guarantees vis-a-vis units on the main invoker -- beware race conditions.</p>
 *
 * <p>NOTE: it is assumed that the server will take care of adding the batch invoker to the main
 * invoker using {@link PresentsInvoker#addInterdependentInvoker()}. This is required so that
 * the server will not shutdown while there is still a batch unit executing. Doing so could result
 * in lost or corrupt data or other inconsistencies.</p>
 */
@Singleton
public class MsoyBatchInvoker extends ReportingInvoker
{
    @Inject public MsoyBatchInvoker (PresentsDObjectMgr omgr, ReportManager repmgr)
    {
        super("msoy.BatchInvoker", omgr, repmgr);
    }
}
