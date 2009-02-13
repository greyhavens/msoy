//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;

import com.threerings.presents.annotation.EventThread;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import static com.threerings.msoy.Log.log;

/**
 * A registry of watched members whose movements from scene to scene anywhere in the Whirled
 * (i.e. cross server) are relayed to the associated {@link Observer} objects.
 *
 * The sole client of this service is currently AVRGameManager, which needs to keep track of what
 * rooms its various current players are in.
 */
@Singleton @EventThread
public class GameWatcherManager
    implements MsoyPeerManager.MemberObserver
{
    /**
     * Interface for notifying the AVRGameManager of the whereabouts of a member.
     */
    public static interface Observer
    {
        /**
         * Notifies that a member has moved to a new scene and/or logged on.
         */
        void memberMoved (int memberId, int sceneId, String hostname, int port);

        /**
         * Notifies that a member has logged off.
         */
        void memberLoggedOff (int memberId);
    }

    @Inject public GameWatcherManager (MsoyPeerManager peerMan)
    {
        peerMan.memberObs.add(this);
    }

    /**
     * Subscribe to notification of this member's scene-to-scene movements on the world servers.
     */
    public void addWatch (int memberId, Observer observer)
    {
        Observer old = _observers.put(memberId, observer);
        if (old != null) {
            log.warning("Displaced existing watcher", "memberId", "observer", old);
        }
    }

    /**
     * Clear an existing movement watch on the given member.
     */
    public void clearWatch (int memberId)
    {
        if (_observers.remove(memberId) == null) {
            log.warning("Attempt to clear non-existent watch", "memberId", memberId);
        }
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberLoggedOn (String node, MemberName member)
    {
        // nada
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberLoggedOff (String node, MemberName member)
    {
        Observer observer = _observers.get(member.getMemberId());
        if (observer != null) {
            observer.memberLoggedOff(member.getMemberId());
        }
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberEnteredScene (String node, MemberLocation loc)
    {
        Observer observer = _observers.get(loc.memberId);
        if (observer != null) {
            String host = _peerMan.getPeerPublicHostName(node);
            int port = _peerMan.getPeerPort(node);
            observer.memberMoved(loc.memberId, loc.sceneId, host, port);
        }
    }

    /** A map of members to {@link Observer} objects to notify of each member's movements. */
    protected IntMap<Observer> _observers = new HashIntMap<Observer>();

    @Inject protected MsoyPeerManager _peerMan;
}
