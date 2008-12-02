//
// $Id$

package com.threerings.msoy.admin.server;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Comparators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import com.samskivert.util.StringUtil;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;

import com.threerings.io.Streamable;
import com.threerings.util.StreamableHashMap;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Collects performance on our EHCache.
 */
public class CacheStatCollector extends StatCollector
{
    public CacheStatCollector (CacheManager cacheMgr)
    {
        _cacheMgr = cacheMgr;
    }

    @Override // from StatCollector
    public Object compileStats ()
    {
        Map<String, Number[]> sums = new StreamableHashMap<String, Number[]>();
        for (String cname : _cacheMgr.getCacheNames()) {
            sums.put(cname.substring(cname.lastIndexOf(".")+1),
                     summarizeCache(_cacheMgr.getCache(cname)));
        }
        return sums;
    }

    @Override // from StatCollector
    public Merger createMerger () {
        return new Merger() {
            protected void mergeStats (String nodeName, Object stats) {
                @SuppressWarnings("unchecked") Map<String, Number[]> sums =
                    (Map<String, Number[]>)stats;
                for (Map.Entry<String, Number[]> entry : sums.entrySet()) {
                    _infos.put(entry.getKey(), entry.getValue());
                }
            }

            protected StatsModel finalizeModel () {
                for (Map.Entry<String, Collection<Number[]>> entry : _infos.asMap().entrySet()) {
                    int col = 0;
                    StatsModel.Cell hits, miss;
                    _model.addRow(entry.getKey(),
                                  StatsModel.newCell(sum(entry.getValue(), col++)), // size
                                  StatsModel.newCell(sum(entry.getValue(), col++)), // ksize
                                  hits = StatsModel.newCell(sum(entry.getValue(), col++)), // hits
                                  StatsModel.newCell(sum(entry.getValue(), col++)), // inmem
                                  StatsModel.newCell(sum(entry.getValue(), col++)), // disk
                                  miss = StatsModel.newCell(sum(entry.getValue(), col++)), // miss
                                  StatsModel.newPercentCell(hits, miss), // hit%
                                  StatsModel.newCell(sum(entry.getValue(), col++)), // evict
                                  StatsModel.newCell((long)avg(entry.getValue(), col++))); // avgget
                }
                return _model;
            }

            protected Multimap<String, Number[]> _infos = Multimaps.newArrayListMultimap();
            protected StatsModel _model = StatsModel.newColumnModel(
                "cache", "size", "(kb)", "hits", "inmem", "disk", "miss", "hit%", "evict", "avgget");
        };
    }

    protected static int hitPercent (long hits, long misses)
    {
        long accesses = hits + misses;
        return (accesses == 0) ? 0 : (int)((100 * hits) / accesses);
    }

    protected Number[] summarizeCache (Cache cache)
    {
        Statistics stats = cache.getStatistics();
        return new Number[] { 
            cache.getSize(),
            (int)(cache.calculateInMemorySize()/1024),
            stats.getCacheHits(),
            stats.getInMemoryHits(),
            stats.getOnDiskHits(),
            stats.getCacheMisses(),
            stats.getEvictionCount(),
            stats.getAverageGetTime(),
        };
    }

    protected long sum (Collection<Number[]> numbers, int index)
    {
        long value = 0;
        for (Number[] numvec : numbers) {
            value += numvec[index].longValue();
        }
        return value;
    }

    protected float avg (Collection<Number[]> numbers, int index)
    {
        float value = 0;
        int count = 0;
        for (Number[] numvec : numbers) {
            value += numvec[index].floatValue();
            count++;
        }
        return value / count;
    }

    /** Used to obtain stats on our cache. */
    protected CacheManager _cacheMgr;

    protected static final String HFMT = "- %20s %5s %4s %4s %8s %7s %7s %9s %5s %6s\n";
    protected static final String DFMT = "- %20s %5d %4d %3d%% %8d %7d %7d %9d %5d %1.1f ms\n";
    protected static final String SFMT = "- %20s     %6d %3d%%\n";
}
