//
// $Id$

package com.threerings.msoy.admin.server;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;

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

            protected StatsModel _model = StatsModel.newColumnModel(
                "cache", "size", "(kb)", "hit%", "hits", "inmem", "disk",
                "miss", "evict", "avgget");
        };
    }

    /** Used to obtain stats on our cache. */
    protected CacheManager _cacheMgr;
}
