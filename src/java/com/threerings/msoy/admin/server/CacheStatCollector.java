//
// $Id$

package com.threerings.msoy.admin.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Collects performance on our EHCache.
 */
public class CacheStatCollector extends NumVecStatCollector
{
    public CacheStatCollector (CacheManager cacheMgr)
    {
        _cacheMgr = cacheMgr;
    }

    @Override // from NumVecStatCollector
    protected void populateStatMap (Map<String, List<Number>> vecs)
    {
        for (String cname : _cacheMgr.getCacheNames()) {
            Cache cache = _cacheMgr.getCache(cname);
            Statistics stats = cache.getStatistics();
            vecs.put(cname.substring(cname.lastIndexOf(".")+1),
                     Lists.<Number>newArrayList(cache.getSize(),
                                                (int)(cache.calculateInMemorySize()/1024),
                                                stats.getCacheHits(),
                                                stats.getInMemoryHits(),
                                                stats.getOnDiskHits(),
                                                stats.getCacheMisses(),
                                                stats.getEvictionCount(),
                                                stats.getAverageGetTime()));
        }
    }

    @Override // from NumVecStatCollector
    protected StatsModel createModel ()
    {
        return StatsModel.newColumnModel(
            "cache", "size", "(kb)", "hits", "inmem", "disk", "miss", "hit%", "evict", "avgget");
    }

    @Override // from NumVecStatCollector
    protected void finalizeRow (StatsModel model, String key, Collection<List<Number>> data)
    {
        int col = 0;
        StatsModel.Cell hits, miss;
        model.addRow(key, StatsModel.newCell(sum(data, col++)), // size
                     StatsModel.newCell(sum(data, col++)), // ksize
                     hits = StatsModel.newCell(sum(data, col++)), // hits
                     StatsModel.newCell(sum(data, col++)), // inmem
                     StatsModel.newCell(sum(data, col++)), // disk
                     miss = StatsModel.newCell(sum(data, col++)), // miss
                     StatsModel.newPercentCell(hits, miss), // hit%
                     StatsModel.newCell(sum(data, col++)), // evict
                     StatsModel.newCell((long)avg(data, col++))); // avgget
    }

    /** Used to obtain stats on our cache. */
    protected CacheManager _cacheMgr;
}
