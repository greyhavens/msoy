package com.threerings.msoy.peer.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samskivert.depot.EHCacheAdapter;
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
            log.warning("Not coordinat Whirled peers with EHCache peers; no provider initialized.");
            return;
        }
        _instance.initWithPeers(peerMan);
    }

    /** We're both the factory and the provider, just return ourselves. */
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
        @Override public void registerPeer (String rmiUrl) { }
        @Override public void unregisterPeer (String rmiUrl) { }

        @Override @SuppressWarnings("unchecked")
        public List listRemoteCachePeers (Ehcache cache)
            throws CacheException
        {
            if (_peerMan == null) {
                // the ehcache subsystem has fired up but Whirled is still booting; we return null
                // here and ehcache will try again for up to getTimeForClusterToForm milliseconds
                return null;
            }

            final List<CachePeer> result = Lists.newArrayList();
            _peerMan.applyToNodes(new Function<NodeObject, Void>() {
                @Override public Void apply (NodeObject node) {
                    if (node != _peerMan.getNodeObject()) {
                        addCachesForNode(result, node.nodeName);
                    }
                    return null;
                }
            });

            return result;
        }

        @Override
        public void init ()
        {
            // do nothing
        }

        @Override
        public void dispose ()
            throws CacheException
        {
            // do nothing
        }

        @Override
        public long getTimeForClusterToForm ()
        {
            // 30 seconds should be enough for anyone
            return 30000;
        }

        protected void addCachesForNode (List<CachePeer> result, String nodeName)
        {
            String nodeHost = _peerMan.getPeerPublicHostName(nodeName);
            if (nodeHost == null) {
                log.warning("Eek, couldn't find the public host name of peer", "node", nodeName);
                return;
            }
            String rmiBase = "//" + nodeHost + ":" + RMI_PORT + "/";
            log.info("Adding caches for node", "node", nodeName);

            // for the given node, acquire a RMI handle for each known Depot cache
            for (String cacheName : EHCACHES) {
                try {
                    result.add(getCacheByURL(rmiBase + cacheName));
                } catch (Exception e) {
                    log.warning("Could not resolve remote peer", "host", nodeHost,
                        "cache", cacheName, e);
                }
            }
        }

        protected CachePeer getCacheByURL (String url)
            throws MalformedURLException, RemoteException, NotBoundException
        {
            // retrieve the RMI handle for the given peer
            synchronized(_cacheByURL) {
                CachePeer peer = _cacheByURL.get(url);
                if (peer == null) {
                    // do the (blocking) lookup and stow away the result
                    log.info("RMI lookup...", "url", url);
                    peer = (CachePeer) Naming.lookup(url);
                    _cacheByURL.put(url, peer);
                }
                return peer;
            }
        }

        protected Map<String, CachePeer> _cacheByURL = Maps.newHashMap();
        protected MsoyPeerManager _peerMan;
        protected CacheManager _cacheMan;
    }

    protected static Provider _instance;
}
