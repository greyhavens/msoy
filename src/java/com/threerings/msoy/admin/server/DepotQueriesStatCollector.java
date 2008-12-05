//
// $Id$

package com.threerings.msoy.admin.server;

import com.samskivert.depot.PersistenceContext;
// import com.samskivert.depot.Stats;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Collects stats on Depot query performance.
 */
public class DepotQueriesStatCollector extends StatCollector
{
    public DepotQueriesStatCollector (PersistenceContext perCtx)
    {
        _perCtx = perCtx;
    }

    @Override // from StatCollector
    public Object compileStats ()
    {
//         Stats.Snapshot stats = _perCtx.getStats();
        return null; // TODO
    }

    @Override // from StatCollector
    public Merger createMerger () {
        return new Merger() {
            protected void mergeStats (String nodeName, Object stats) {
                // TODO
            }

            protected StatsModel finalizeModel () {
                // TODO
                return _model;
            }

            protected StatsModel _model = StatsModel.newRowModel(
                "stat", "ops", "con. wait", "avg wait",
                "q. cached", "q. uncached", "q. explicit", // "q. %",
                "r. cached", "r. uncached", // "r. %",
                "reads", "read time", "avg read", "writes", "write time", "avg write");
        };
    }

    protected PersistenceContext _perCtx;
}
