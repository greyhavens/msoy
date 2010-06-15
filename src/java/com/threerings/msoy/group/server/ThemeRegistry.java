//
// $Id$

package com.threerings.msoy.group.server;

import static com.threerings.msoy.Log.log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.Maps;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;
import com.samskivert.util.ResultListener.NOOP;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.peer.data.HostedTheme;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject.Lock;

/**
 * Maintains information regarding existing themes and which ones we're responsible
 * for persisting popularity changes to.
 */

@Singleton
public class ThemeRegistry
    implements Lifecycle.Component
{
    public static class ThemeEntry implements Comparable<ThemeEntry>
    {
        /** The groupId of the theme. */
        public int themeId;

        /** What's the current popularity of this theme? */
        public int popularity;

        /** Is this theme currently hosted on any server? */
        public boolean hosted;

        public ThemeEntry (int themeId, int popularity, boolean hosted) {
            this.themeId = themeId;
            this.popularity = popularity;
            this.hosted = hosted;
        }

        public int hashCode () {
            return themeId;
        }

        public boolean equals (Object obj)
        {
            return (this == obj ||
                    (obj != null && getClass() == obj.getClass() &&
                            ((ThemeEntry) obj).themeId == themeId));
        }

        public int compareTo (ThemeEntry other) {
            return ComparisonChain.start()
                .compare(popularity, other.popularity, Ordering.natural().reverse())
                .compare(themeId, other.themeId).result();
        }
    }

    /** Extracts the groupId of an entry. */
    public static final Function<ThemeEntry, Integer> ENTRY_TO_GROUP_ID =
        new Function<ThemeEntry, Integer>() {
        public Integer apply (ThemeEntry record) {
            return record.themeId;
        }
    };


    @Inject public ThemeRegistry (Lifecycle cycle)
    {
        cycle.addComponent(this);
    }

    @Override @BlockingThread
    public void init ()
    {
        // grab whatever's in the database here at startup
        List<ThemeRecord> records = _themeRepo.loadActiveThemes();
        if (records.size() < 5) {
            // for e.g. development environments, just grab whatever is there
            records = _themeRepo.loadThemes(5);
        }
        // initialize our theme mapping with these records
        for (ThemeRecord rec : records) {
            _themes.put(rec.groupId, new ThemeEntry(rec.groupId, rec.popularity, false));
        }
    }

    @Override
    public void shutdown ()
    {
        _invoker.postUnit(new Invoker.Unit("flushDirtyThemes") {
            @Override public boolean invoke () {
                flushDirtyThemes();
                return false;
            }
        });
    }

    /**
     * Construct and return an up-to-date view of the themes on all the servers, sorted by
     * popularity. This is not an expensive operation but it's not trivial either, so very
     * frequent calls should be cached.
     */
    public List<ThemeEntry> getThemes ()
    {
        for (MsoyNodeObject node : _peerMan.getMsoyNodeObjects()) {
            for (HostedTheme nodeTheme : node.hostedThemes) {
                ThemeEntry entry = _themes.get(nodeTheme.themeId);
                if (entry == null || entry.popularity != nodeTheme.popularity || !entry.hosted) {
                    _themes.put(nodeTheme.themeId,
                        new ThemeEntry(nodeTheme.themeId, nodeTheme.popularity, true));
                }
            }
        }

        List<ThemeEntry> result = Lists.newArrayList(_themes.values());
        Collections.sort(result);
        return result;
    }

    /**
     * Host any themes that were loaded from the database upon startup and that need to be
     * hosted (because they're populated or have popularity that needs decaying) but aren't
     * yet. Then go through all themes *hosted on this node* and update their popularity
     * partly by decaying the old value, partly by adding the new population.
     */
    public void heartbeat (final double decay, final PopularPlacesSnapshot snapshot)
    {
        // find out which themes need to be hosted and aren't
        LinkedList<ThemeEntry> toHost = Lists.newLinkedList();
        for (ThemeEntry entry : _themes.values()) {
            if (entry.hosted) {
                continue;
            }
            if (snapshot.getThemePopulation(entry.themeId) > 0 || entry.popularity > 0) {
                toHost.add(entry);
            }
        }
        // set up the next stage, the popularity update
        Runnable doDecay = new Runnable() {
            @Override public void run () {
                doDecayThemes(decay, snapshot);
            }
        };

        // either run the next stage directly, or host stuff asynchronously first
        if (toHost.isEmpty()) {
            doDecay.run();
        } else {
            lazilyHostThemes(toHost, doDecay);
        }
    }

    public void newTheme (ThemeRecord record)
    {
        _themes.put(record.groupId, new ThemeEntry(record.groupId, record.popularity, false));
        maybeHostTheme(record.groupId, new NOOP<Integer>());
    }

    public void maybeHostTheme (final int themeId, final ResultListener<Integer> listener)
    {
        // check to see if the theme is already hosted on a server
        if (_peerMan.getThemeHost(themeId) != null) {
            // if so we're happy already
            listener.requestCompleted(themeId);
            return;
        }

        // if not, we'll host it
        final Lock themeLock = MsoyPeerManager.getThemeLock(themeId);
        _peerMan.acquireLock(themeLock, new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (_peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.debug("Got lock, resolving theme", "themeId", themeId);

                    ThemeEntry entry = _themes.get(themeId);
                    if (entry == null) {
                        log.warning("WTF? Unknown theme", "theme", themeId);
                    } else {
                        ((MsoyNodeObject) _peerMan.getNodeObject()).addToHostedThemes(
                            new HostedTheme(themeId, entry.popularity));
                        _themes.put(themeId, new ThemeEntry(themeId, entry.popularity, true));
                    }
                    releaseLock();

                } else {
                    // we didn't get the lock, so let's see what happened by re-checking
                    Tuple<String, HostedTheme> nodeInfo = _peerMan.getThemeHost(themeId);
                    if (nodeName == null || nodeInfo == null || !nodeName.equals(nodeInfo.left)) {
                        log.warning("Theme resolved on wacked-out node?",
                            "themeId", themeId, "nodeName", nodeName, "nodeInfo", nodeInfo);
                    }
                }
                listener.requestCompleted(themeId);
            }
            protected void releaseLock () {
                _peerMan.releaseLock(themeLock, new ResultListener.NOOP<String>());
            }

            public void requestFailed (Exception e) {
                log.warning("Failed to acquire theme resolution lock", "themeId", themeId, e);
                listener.requestFailed(e);
            }
        });
    }

    protected void lazilyHostThemes (final Queue<ThemeEntry> toHost, final Runnable callback)
    {
        // do we still have something to host?
        ThemeEntry next = toHost.poll();
        if (next == null) {
            callback.run();
            return;
        }
        // yes, so do it!
        maybeHostTheme(next.themeId, new ResultListener<Integer>() {
            @Override public void requestCompleted (Integer result) {
                // yay. next!
                lazilyHostThemes(toHost, callback);
            }
            @Override public void requestFailed (Exception cause) {
                // boo. next!
                lazilyHostThemes(toHost, callback);
            }
        });
    }

    protected void doDecayThemes (double decay, PopularPlacesSnapshot snapshot)
    {
        MsoyNodeObject node = _peerMan.getMsoyNodeObject();
        for (HostedTheme nodeTheme : node.hostedThemes) {
            int newPop = (int)(decay * nodeTheme.popularity) +
            snapshot.getThemePopulation(nodeTheme.themeId);
            if (newPop != nodeTheme.popularity) {
                node.updateHostedThemes(new HostedTheme(nodeTheme.themeId, newPop));
                _themes.put(nodeTheme.themeId, new ThemeEntry(nodeTheme.themeId, newPop, true));
                _dirty.add(nodeTheme.themeId);
            }
        }
    }

    @BlockingThread
    protected void flushDirtyThemes ()
    {
        MsoyNodeObject node = _peerMan.getMsoyNodeObject();
        for (int themeId : _dirty) {
            HostedTheme theme = node.hostedThemes.get(themeId);
            if (theme == null) {
                log.warning("Weird, dirty theme not hosted by us", "theme", themeId);
                continue;
            }
            _themeRepo.updateThemePopularity(themeId, theme.popularity);
        }
        _dirty.clear();
    }

    protected Map<Integer, ThemeEntry> _themes = Maps.newHashMap();
    protected Set<Integer> _dirty = Sets.newHashSet();

    // our dependencies
    @Inject protected GroupRepository _groupRepo;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected ThemeRepository _themeRepo;
}
