//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.client.PendingData;
import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.client.persist.SceneRepository;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;

/**
 * Handles custom scene traversal and extra bits for Whirled.
 */
public class MsoySceneDirector extends SceneDirector
{
    private static const log :Log = Log.getLog(MsoySceneDirector);

    public function MsoySceneDirector (
        ctx :WorldContext, locDir :LocationDirector, repo :SceneRepository)
    {
        super(ctx, locDir, repo, new MsoySceneFactory());
    }

    /**
     * Traverses the specified portal using the MsoySceneService which handles switching between
     * servers and other useful business.
     *
     * @return true if we issued the request, false if it was rejected for some reason.
     */
    public function traversePortal (portalId :int) :Boolean
    {
        // look up the destination scene and location
        var scene :MsoyScene = (getScene() as MsoyScene);
        if (scene == null) {
            log.warning("Asked to traverse portal when we have no scene [id=" + portalId + "].");
            return false;
        }

        // find the portal they're talking about
        var dest :MsoyPortal = (scene.getPortal(portalId) as MsoyPortal);
        if (dest == null) {
            log.warning("Requested to traverse non-existent portal [portalId=" + portalId +
                        ", portals=" + scene.getPortals() + "].");
            return false;
        }

        // prepare to move to this scene (sets up pending data)
        if (!prepareMoveTo(dest.targetSceneId, null)) {
            log.info("Portal traversal vetoed [portalId=" + portalId + "].");
            return false;
        }

        // note our departing portal id and target location in the destination scene
        _departingPortalId = portalId;
        (_pendingData as MsoyPendingData).destLoc = dest.dest;

        // now that everything is noted (in case we have to switch servers) ask to move
        sendMoveRequest();
        return true;
    }

    // from SceneDirector
    override public function moveTo (sceneId :int) :Boolean
    {
        if (sceneId == _sceneId) {
            // ignore this as we're just hearing back from our browser URL update mechanism
            return false;
        }
        return super.moveTo(sceneId);
    }

    // from SceneDirector
    override public function moveSucceeded (placeId :int, config :PlaceConfig) :void
    {
        super.moveSucceeded(placeId, config);
        // tell our controller to update the URL of the browser to reflect our new location
        (_ctx as WorldContext).getMsoyController().wentToScene(_sceneId);
    }

    // from SceneDirector
    override public function requestFailed (reason :String) :void
    {
        _departingPortalId = -1;
        (_ctx as WorldContext).displayFeedback(MsoyCodes.GENERAL_MSGS, reason);
        super.requestFailed(reason);
    }

    // from SceneDirector
    override protected function createPendingData () :PendingData
    {
        return new MsoyPendingData();
    }

    // from SceneDirector
    override protected function sendMoveRequest () :void
    {
        // check the version of our cached copy of the scene to which we're requesting to move; if
        // we were unable to load it, assume a cached version of zero
        var sceneVers :int = 0;
        if (_pendingData.model != null) {
            sceneVers = _pendingData.model.version;
        }

        // extract our destination location from the pending data
        var destLoc :MsoyLocation = (_pendingData as MsoyPendingData).destLoc;

        // note: _departingPortalId is only needed *before* a server switch, so we intentionally
        // allow it to get cleared out in the clientDidLogoff() call that happens as we're
        // switching from one server to another

        // issue a moveTo request
        log.info("Issuing moveTo(" + _pendingData.sceneId + ", " + sceneVers + ", " +
                 _departingPortalId + ", " + destLoc + ").");
        _msservice.moveTo(_wctx.getClient(), _pendingData.sceneId, sceneVers,
                          _departingPortalId, destLoc, this);
    }

    // documentation inherited
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);
        _departingPortalId = -1;
    }

    // from SceneDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);
        // get a handle on our special scene service
        _msservice = (client.requireService(MsoySceneService) as MsoySceneService);
    }

    protected var _msservice :MsoySceneService;
    protected var _departingPortalId :int = -1;
}
}
