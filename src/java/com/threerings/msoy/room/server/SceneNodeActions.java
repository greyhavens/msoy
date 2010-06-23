//
// $Id: PlayerNodeActions.java 16931 2009-05-27 21:01:42Z mdb $

package com.threerings.msoy.room.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

/**
 * Provides a simple interface for dispatching node actions for scenes.
 */
@Singleton
public class SceneNodeActions
{
    public void evictAndShutdown (int sceneId)
    {
        _peerMan.invokeNodeAction(new EvictAndShutdownAction(sceneId));
    }

    public void flushUpdates (int sceneId, InvocationService.ConfirmListener listener)
    {
        _peerMan.invokeNodeRequest(new FlushFurniUpdatesRequest(sceneId), listener);
    }

    protected static abstract class SceneNodeAction extends PeerManager.NodeAction
    {
        public SceneNodeAction (int sceneId) {
            _sceneId = sceneId;
        }

        public SceneNodeAction () {
        }

        @Override // from PeerManager.NodeAction
        public boolean isApplicable (NodeObject nodeobj) {
            return ((MsoyNodeObject)nodeobj).hostedScenes.containsKey(_sceneId);
        }

        @Override // from PeerManager.NodeAction
        protected void execute () {
            RoomManager mgr = (RoomManager)_sceneReg.getSceneManager(_sceneId);
            if (mgr != null) {
                execute(mgr);
            } // if not, oh well, they went away
        }

        protected abstract void execute (RoomManager mgr);

        protected int _sceneId;

        /** Used to look up member objects. */
        @Inject protected transient SceneRegistry _sceneReg;
    }

    protected static abstract class SceneNodeRequest extends PeerManager.NodeRequest
    {
        public SceneNodeRequest (int sceneId) {
            _sceneId = sceneId;
        }

        public SceneNodeRequest () {
        }

        @Override // from PeerManager.NodeAction
        protected void execute (InvocationService.ResultListener listener) {
            execute(_sceneId, listener);
        }

        protected abstract void execute (int sceneId, InvocationService.ResultListener listener);

        protected int _sceneId;

        /** Used to look up member objects. */
        @Inject protected transient SceneRegistry _sceneReg;
    }

    /** Throws everyone out of the given room and shuts it down, forcing a reload on next entry. */
    protected static class EvictAndShutdownAction extends SceneNodeAction {
        public EvictAndShutdownAction (int sceneId) {
            super(sceneId);
        }

        public EvictAndShutdownAction () {
        }

        protected void execute (RoomManager mgr) {
            mgr.evictPlayersAndShutdown();
        }
    }

    /** Throws everyone out of the given room and shuts it down, forcing a reload on next entry. */
    protected static class FlushFurniUpdatesRequest extends SceneNodeRequest {
        public FlushFurniUpdatesRequest (int sceneId) {
            super(sceneId);
        }

        public FlushFurniUpdatesRequest () {
        }

        protected void execute (int sceneId, final InvocationService.ResultListener listener) {
            _sceneLogic.flushUpdates(sceneId, listener);
        }

        @Inject protected transient SceneLogic _sceneLogic;

    }
    @Inject protected MsoyPeerManager _peerMan;
}
