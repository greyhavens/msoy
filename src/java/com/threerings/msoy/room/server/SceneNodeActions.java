//
// $Id: PlayerNodeActions.java 16931 2009-05-27 21:01:42Z mdb $

package com.threerings.msoy.room.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

/**
 * Provides a simple interface for dispatching node actions for scenes.
 */
@Singleton
public class SceneNodeActions
{
    public void flushTheme (int sceneId)
    {
        _peerMan.invokeNodeAction(new FlushThemeAction(sceneId));
    }

    protected static abstract class SceneNodeAction extends MsoyPeerManager.NodeAction
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

    /** Handles updating a player's game. */
    protected static class FlushThemeAction extends SceneNodeAction
    {
        public FlushThemeAction (int sceneId) {
            super(sceneId);
        }

        public FlushThemeAction () {
        }

        protected void execute (RoomManager mgr) {
            // DO SOMETHING
        }
    }

    @Inject protected MsoyPeerManager _peerMan;
}
