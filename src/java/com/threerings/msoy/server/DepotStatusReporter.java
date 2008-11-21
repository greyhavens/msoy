//
// $Id$

package com.threerings.msoy.server;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.Stats;

import com.threerings.presents.server.ReportManager;

/**
 * Reports a summary of the status of Depot.
 */
public class DepotStatusReporter
    implements ReportManager.Reporter
{
    public DepotStatusReporter (PersistenceContext perCtx)
    {
        _perCtx = perCtx;
    }

    // from interface ReportManager.Reporter
    public void appendReport (StringBuilder buf, long now, long sinceLast, boolean reset)
    {
        buf.append("* samskivert.Depot:\n");

        Stats.Snapshot snap = _perCtx.getStats();
        buf.append("- ops: ").append(snap.totalOps).append("\n");
        final float avgwait = snap.connectionWaitTime/(float)snap.totalOps;
        buf.append("- connection wait: ").append(String.format("%1.1f", avgwait)).append("ms avg\n");
        buf.append("- queries: ").append(snap.cachedQueries).append(" cached, ");
        buf.append(snap.uncachedQueries).append(" uncached\n");
        buf.append("- records: ").append(snap.cachedRecords).append(" cached, ");
        buf.append(snap.uncachedRecords).append(" uncached\n");

        int reads = snap.queryHisto.size();
        buf.append("- reads: ").append(reads).append(", ");
        buf.append(reads/snap.queryTime).append("ms avg\n");

        int writes = snap.modifierHisto.size();
        buf.append("- writes: ").append(writes).append(", ");
        buf.append(writes/snap.modifierTime).append("ms avg\n");
    }

    protected PersistenceContext _perCtx;
}
