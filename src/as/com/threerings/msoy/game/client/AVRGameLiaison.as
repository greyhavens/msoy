//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.MsoyGameConfig;

/**
 * Handles the AVRG-specific aspects of the game server connection.
 */
public class AVRGameLiaison extends GameLiaison
{
    public static const log :Log = Log.getLog(AVRGameLiaison);

    public function AVRGameLiaison (ctx :WorldContext, gameId :int)
    {
        super(ctx, gameId);
    }
    
    override public function clientDidLogon (event :ClientEvent) :void
    {
        var svc :AVRService = (_gctx.getClient().requireService(AVRService) as AVRService);
        var cb :ResultWrapper = new ResultWrapper(
            function (cause :String) :void {
                _ctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
                shutdown();
            },
            function (result :Object) :void {
                log.info("Successfully activated AVRG [gameId=" + _gameId + "]");
                _ctrl = new AVRGameController(_ctx, _gctx, int(result));
            }
        );
        svc.activateGame(_gctx.getClient(), _gameId, cb);
    }

    override public function shutdown () :void
    {
        if (_ctrl != null) {
            _ctrl.forceShutdown();
            _ctrl = null;
        }

        super.shutdown();
    }

    /**
     * Returns the game object if we're currently in an AVRG, null otherwise.
     */
    public function getAVRGameObject () :AVRGameObject
    {
        return (_ctrl == null) ? null : _ctrl.getAVRGameObject();
    }

    /**
     * Returns the game object if we're currently in an AVRG, null otherwise.
     */
    public function getAVRGameController () :AVRGameController
    {
        return _ctrl;
    }

    /** The controller for the current world game. */
    protected var _ctrl :AVRGameController;
}
}
