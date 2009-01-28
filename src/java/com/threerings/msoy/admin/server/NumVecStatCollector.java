//
// $Id$

package com.threerings.msoy.admin.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import com.threerings.util.StreamableHashMap;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * A helpful class for reporting on stats that are basically a vector of numbers.
 */
public abstract class NumVecStatCollector extends StatCollector
{
    @Override // from StatCollector
    public Object compileStats ()
    {
        Map<String, List<Number>> sums = new StreamableHashMap<String, List<Number>>();
        populateStatMap(sums);
        return sums;
    }

    @Override // from StatCollector
    public Merger createMerger () {
        return new Merger() {
            protected void mergeStats (String nodeName, Object stats) {
                @SuppressWarnings("unchecked") Map<String, List<Number>> sums =
                    (Map<String, List<Number>>)stats;
                for (Map.Entry<String, List<Number>> entry : sums.entrySet()) {
                    _infos.put(entry.getKey(), entry.getValue());
                }
            }

            protected StatsModel finalizeModel () {
                for (Map.Entry<String, Collection<List<Number>>> entry :
                        _infos.asMap().entrySet()) {
                    finalizeRow(_model, entry.getKey(), entry.getValue());
                }
                return _model;
            }

            protected Multimap<String, List<Number>> _infos = Multimaps.newArrayListMultimap();
            protected StatsModel _model = createModel();
        };
    }

    protected abstract void populateStatMap (Map<String, List<Number>> stats);

    protected abstract StatsModel createModel ();

    protected abstract void finalizeRow (
        StatsModel model, String key, Collection<List<Number>> data);

    protected static long sum (Collection<List<Number>> numbers, int index)
    {
        long value = 0;
        for (List<Number> numvec : numbers) {
            value += numvec.get(index).longValue();
        }
        return value;
    }

    protected static double avg (Collection<List<Number>> numbers, int index)
    {
        double value = 0;
        int count = 0;
        for (List<Number> numvec : numbers) {
            value += numvec.get(index).doubleValue();
            count++;
        }
        return value / count;
    }
}
