package com.threerings.msoy.peer.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.samskivert.depot.EHCacheAdapter;
import com.samskivert.util.Tuple;
import com.threerings.presents.peer.data.NodeObject;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.distribution.CachePeer;

import static com.threerings.msoy.Log.log;

public class EHCachePeerCoordinator extends CacheManagerPeerProviderFactory
{
    /** Must correspond to what's provided to the PeerManagerCacheListener in ehcache.xml. */
    public static final int RMI_PORT = 40001;

    /** Caches we boldly assume exist on each peer. */
    public static final String[] EHCACHES = {
        EHCacheAdapter.EHCACHE_RECORD_CACHE,
        EHCacheAdapter.EHCACHE_SHORT_KEYSET_CACHE,
        EHCacheAdapter.EHCACHE_LONG_KEYSET_CACHE,
        EHCacheAdapter.EHCACHE_RESULT_CACHE
    };

    public static void initWithPeers (MsoyPeerManager peerMan)
    {
        if (_instance == null) {
            log.warning("No provider initialized -- not coordinating Whirled and EHCache peers.");
            return;
        }
        _instance.initWithPeers(peerMan);
    }

    /** Return our provider, creating it if needed. */
    @Override
    public CacheManagerPeerProvider createCachePeerProvider (
        CacheManager cacheManager, Properties properties)
    {
        if (_instance == null) {
            _instance = new Provider(cacheManager);
        }
        return _instance;
    }

    protected static class Provider implements CacheManagerPeerProvider
    {
        public Provider (CacheManager cacheManager)
        {
            _cacheMan = cacheManager;
        }

        public void initWithPeers (MsoyPeerManager peerMan)
        {
            _peerMan = peerMan;
        }

        // these are internal to EHCache and I have NO clue why they're in the API
        public void registerPeer (String rmiUrl) { }
        public void unregisterPeer (String rmiUrl) { }

        public List<?> listRemoteCachePeers (Ehcache cache)
            throws CacheException
        {
            if (_peerMan == null) {
                // the ehcache subsystem has fired up but Whirled is still booting;
                // we return null here and ehcache will try again
                return null;
            }

            // list the current whirled peers
            final List<CachePeer> result = Lists.newArrayList();
            final Set<String> nodes = Sets.newHashSet();
            _peerMan.applyToNodes(new Function<NodeObject, Void>() {
                public Void apply (NodeObject node) {
                    if (node != _peerMan.getNodeObject()) {
                        addCachesForNode(result, node.nodeName);
                        nodes.add(node.nodeName);
                    }
                    return null;
                }
            });

            // if any previously known peer is no longer with us, clear out the cache
            Set<Tuple<String, String>> toRemove = Sets.newHashSet();
            for (Tuple<String, String> key : _peerCache.keySet()) {
                if (!nodes.contains(key.left)) {
                    toRemove.add(key);
                }
            }
            for (Tuple<String, String> key : toRemove) {
                log.info("Removing EHCache peer: " + key);
                _peerCache.remove(key);
            }

            return result;
        }

        public void init ()
        {
            // do nothing
        }

        public void dispose ()
            throws CacheException
        {
            // do nothing
        }

        public long getTimeForClusterToForm ()
        {
            // this is only used when bootstrapping, which we don't do, but whatever
            return 10000;
        }

        protected void addCachesForNode (List<CachePeer> result, String nodeName)
        {
            String nodeHost = _peerMan.getPeerInternalHostName(nodeName);
            if (nodeHost == null) {
                log.warning("Eek, couldn't find the public host name of peer", "node", nodeName);
                return;
            }
            String rmiBase = "//" + nodeHost + ":" + RMI_PORT + "/";

            // for the given node, acquire a RMI handle for each known Depot cache
            for (String cacheName : EHCACHES) {
                try {
                    result.add(getCache(nodeName, rmiBase + cacheName));
                } catch (Exception e) {
                    log.warning("Could not resolve remote peer", "host", nodeHost,
                        "cache", cacheName, e);
                }
            }
        }

        protected CachePeer getCache (String nodeName, String url)
            throws MalformedURLException, RemoteException, NotBoundException
        {
            Tuple<String, String> key = Tuple.newTuple(nodeName, url);
            // retrieve the RMI handle for the given peer
            synchronized(_peerCache) {
                CachePeer peer = _peerCache.get(key);
                if (peer == null) {
                    // do the (blocking) lookup and stow away the result
                    log.info("RMI lookup of remote cache", "url", url);
                    peer = (CachePeer) Naming.lookup(url);
                    _peerCache.put(key, peer);
                }
                return peer;
            }
        }

        protected Map<Tuple<String, String>, CachePeer> _peerCache = Maps.newHashMap();
        protected MsoyPeerManager _peerMan;
        protected CacheManager _cacheMan;
    }

    protected static Provider _instance;
}
