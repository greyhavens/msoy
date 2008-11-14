//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Comparators;
import com.google.common.collect.Lists;

import com.samskivert.util.StringUtil;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;

import com.threerings.presents.server.ReportManager;

/**
 * Reports a summary of the status of our caches.
 */
public class CacheStatusReporter
    implements ReportManager.Reporter
{
    public CacheStatusReporter (CacheManager cacheMgr)
    {
        _cacheMgr = cacheMgr;
    }

    // from interface ReportManager.Reporter
    public void appendReport (StringBuilder buf, long now, long sinceLast, boolean reset)
    {
        buf.append("* ehcache.CacheManager:\n");

        //
        List<CacheInfo> infos = Lists.newArrayList();
        for (String cname : _cacheMgr.getCacheNames()) {
            infos.add(new CacheInfo(cname, _cacheMgr.getCache(cname)));
        }
        Collections.sort(infos);

        buf.append(String.format(HFMT, "cache", "size", "(kb)", "hit%", "hits", "inmem", "disk",
                                 "miss", "evict", "avgget"));
        long thits = 0, tmisses = 0, tksize = 0;
        for (CacheInfo info : infos) {
            buf.append(info);
            thits += info.hits;
            tmisses += info.misses;
            tksize += info.ksize;
        }
        buf.append(String.format(SFMT, "total", tksize, hitPercent(thits, tmisses)));
    }

    protected static int hitPercent (long hits, long misses)
    {
        long accesses = hits + misses;
        return (accesses == 0) ? 0 : (int)((100 * hits) / accesses);
    }

    protected static class CacheInfo implements Comparable<CacheInfo> {
        public final long hits;
        public final long misses;
        public final int ksize;

        public CacheInfo (String cname, Cache cache) {
            Statistics stats = cache.getStatistics();
            String sname = cname.substring(cname.lastIndexOf(".")+1);

            this.hits = stats.getCacheHits();
            this.misses = stats.getCacheMisses();
            this.ksize = (int)(cache.calculateInMemorySize()/1024);

            _info = String.format(
                DFMT, StringUtil.truncate(sname, 20), cache.getSize(), this.ksize,
                hitPercent(this.hits, this.misses), this.hits,
                stats.getInMemoryHits(), stats.getOnDiskHits(), this.misses,
                stats.getEvictionCount(), stats.getAverageGetTime());
        }

        // from interface Comparable<CacheInfo>
        public int compareTo (CacheInfo other) {
            return Comparators.compare(other.misses, this.misses);
        }

        public String toString () {
            return _info;
        }

        protected final String _info;
    }

    protected CacheManager _cacheMgr;

    protected static final String HFMT = "- %20s %5s %4s %4s %8s %7s %7s %9s %5s %6s\n";
    protected static final String DFMT = "- %20s %5d %4d %3d%% %8d %7d %7d %9d %5d %1.1f ms\n";
    protected static final String SFMT = "- %20s     %6d %3d%%\n";
}
