//
// $Id$

package com.threerings.msoy.admin.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import com.threerings.msoy.web.server.RPCProfiler;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Collects stats on GWT servlet RPC performance.
 */
public class RPCStatCollector extends NumVecStatCollector
{
    public RPCStatCollector (RPCProfiler profiler)
    {
        _profiler = profiler;
    }

    @Override // from NumVecStatCollector
    protected void populateStatMap (Map<String, List<Number>> vecs)
    {
        for (Map.Entry<String, RPCProfiler.Result> entry : _profiler.getResults().entrySet()) {
            RPCProfiler.Result result = entry.getValue();
            vecs.put(entry.getKey(), Lists.<Number>newArrayList(
                result.numSamples, result.averageTime, result.standardDeviation));
        }
    }

    @Override // from NumVecStatCollector
    protected StatsModel createModel ()
    {
        return StatsModel.newColumnModel("method", "calls", "average(ms)", "deviation(ms)");
    }

    @Override // from NumVecStatCollector
    protected void finalizeRow (StatsModel model, String key, Collection<List<Number>> data)
    {
        int col = 0;
        model.addRow(key,
                     StatsModel.newCell(sum(data, col++)), // calls
                     StatsModel.newCell((long)avg(data, col++)), // avg
                     StatsModel.newCell((long)avg(data, col++))); // dev
    }

    protected RPCProfiler _profiler;
}
