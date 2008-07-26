//
// $Id$

package com.threerings.msoy.world.server;

import com.samskivert.util.ResultListener;

import com.threerings.presents.server.InvocationException;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.LocationManager;

import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.server.SceneManager;
import com.threerings.whirled.server.SceneMoveHandler;

import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;

/**
 * Handles entering a room.
 */
public class MsoySceneMoveHandler extends SceneMoveHandler
{
    public MsoySceneMoveHandler (LocationManager locman, MsoyPeerManager peerMan, BodyObject body,
                                 int sceneVer, MsoyLocation dest,
                                 SceneService.SceneMoveListener listener)
    {
        super(locman, body, sceneVer, listener);
        _dest = dest;
    }

    @Override // from AbstractSceneMoveHandler
    public void sceneFailedToResolve (int sceneId, Exception reason)
    {
        super.sceneFailedToResolve(sceneId, reason);
        // release our scene resolution lock because we booched it
        _peerMan.releaseLock(
            MsoyPeerManager.getSceneLock(sceneId), new ResultListener.NOOP<String>());
    }

    @Override // from AbstractSceneMoveHandler
    protected void effectSceneMove (SceneManager scmgr)
        throws InvocationException
    {
        // create a fake "from" portal that contains our destination location
        MsoyPortal from = new MsoyPortal();
        from.targetPortalId = (short)-1;
        from.dest = _dest;

        // let the destination room manager know that we're coming in "from" that portal
        RoomManager destmgr = (RoomManager)scmgr;
        destmgr.mapEnteringBody(_body, from);

        try {
            super.effectSceneMove(destmgr);
        } catch (InvocationException ie) {
            // if anything goes haywire, clear out our entering status
            destmgr.clearEnteringBody(_body);
            throw ie;
        }
    }

    protected MsoyPeerManager _peerMan;
    protected MsoyLocation _dest;
}
