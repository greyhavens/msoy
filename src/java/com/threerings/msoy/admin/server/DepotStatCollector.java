//
// $Id$

package com.threerings.msoy.admin.server;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.Stats;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Collects stats on overall Depot performance.
 */
public class DepotStatCollector extends StatCollector
{
    public DepotStatCollector (PersistenceContext perCtx)
    {
        _perCtx = perCtx;
    }

    @Override // from StatCollector
    public Object compileStats ()
    {
        Stats.Snapshot stats = _perCtx.getStats();
        return new long[] {
            stats.totalOps,
            stats.connectionWaitTime,
            stats.cachedQueries,
            stats.uncachedQueries,
            stats.explicitQueries,
            stats.cachedRecords,
            stats.uncachedRecords,
            stats.queryHisto.size(),
            stats.queryTime,
            stats.modifierHisto.size(),
            stats.modifierTime,
        };
    }

    @Override // from StatCollector
    public Merger createMerger () {
        return new Merger() {
            protected void mergeStats (String nodeName, Object stats) {
                long[] values = (long[])stats;
                int idx = 0;
                StatsModel.Cell cell1, cell2;
                _model.addColumn(
                    nodeName, cell1 = StatsModel.newCell(values[idx++]), // stats.totalOps
                    cell2 = StatsModel.newCell(values[idx++]), // stats.connectionWaitTime
                    StatsModel.newDivCell(cell2, cell1),
                    StatsModel.newCell(values[idx++]), // stats.cachedQueries
                    StatsModel.newCell(values[idx++]), // stats.uncachedQueries
                    StatsModel.newCell(values[idx++]), // stats.explicitQueries
                    // TODO q% (for each column)
                    StatsModel.newCell(values[idx++]), // stats.cachedRecords
                    StatsModel.newCell(values[idx++]), // stats.uncachedRecords
                    // TODO r%
                    cell1 = StatsModel.newCell(values[idx++]), // stats.queryHisto.size()
                    cell2 = StatsModel.newCell(values[idx++]), // stats.queryTime
                    StatsModel.newDivCell(cell2, cell1),
                    cell1 = StatsModel.newCell(values[idx++]), // stats.modifierHisto.size()
                    cell2 = StatsModel.newCell(values[idx++]), // stats.modifierTime
                    StatsModel.newDivCell(cell2, cell1));
            }

            protected StatsModel finalizeModel () {
                StatsModel.Cell cell1, cell2;
                _model.addColumn(
                    "total", cell1 = StatsModel.newRowSumCell(), // stats.totalOps
                    cell2 = StatsModel.newRowSumCell(), // stats.connectionWaitTime
                    StatsModel.newDivCell(cell2, cell1),
                    StatsModel.newRowSumCell(), // stats.cachedQueries
                    StatsModel.newRowSumCell(), // stats.uncachedQueries
                    StatsModel.newRowSumCell(), // stats.explicitQueries
                    // TODO q% (for each column)
                    StatsModel.newRowSumCell(), // stats.cachedRecords
                    StatsModel.newRowSumCell(), // stats.uncachedRecords
                    // TODO r%
                    cell1 = StatsModel.newRowSumCell(), // stats.queryHisto.size()
                    cell2 = StatsModel.newRowSumCell(), // stats.queryTime
                    StatsModel.newDivCell(cell2, cell1),
                    cell1 = StatsModel.newRowSumCell(), // stats.modifierHisto.size()
                    cell2 = StatsModel.newRowSumCell(), // stats.modifierTime
                    StatsModel.newDivCell(cell2, cell1));
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
