//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.room.client.WorldContext;

import com.threerings.msoy.game.client.GameLiaison;
import com.threerings.msoy.game.data.MsoyGameConfig;

import com.threerings.msoy.avrg.data.AVRGameMarshaller;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.AVRMarshaller;

/**
 * Handles the AVRG-specific aspects of the game server connection.
 */
public class AVRGameLiaison extends GameLiaison
{
    public static const log :Log = Log.getLog(AVRGameLiaison);

    public function AVRGameLiaison (ctx :WorldContext, gameId :int)
    {
        super(ctx, gameId);

        // ensure that the compiler includes these necessary symbols
        var c :Class;
        c = AVRGameMarshaller;
        c = AVRMarshaller;
    }
    
    override public function clientWillLogon (event :ClientEvent) :void
    {
        super.clientWillLogon(event);

        // AVRG's need access to the world services, too.
        _gctx.getClient().addServiceGroup(MsoyCodes.WORLD_GROUP);

    }

    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);

        var svc :AVRService = (_gctx.getClient().requireService(AVRService) as AVRService);
        var cb :ResultWrapper = new ResultWrapper(
            function (cause :String) :void {
                _gctx.getChatDirector().displayFeedback(MsoyCodes.GAME_MSGS, cause);
                shutdown();
            },
            function (result :Object) :void {
                log.info("Initializing AVRG... [gameId=" + _gameId + "]");
                _ctrl = new AVRGameController(_wctx, _gctx, _gameId, int(result));
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

    public function leaveAVRGame () :void
    {
        var svc :AVRService = (_gctx.getClient().requireService(AVRService) as AVRService);
        svc.deactivateGame(_gctx.getClient(), _gameId, new ConfirmAdapter (
            function (cause :String) :void {
                log.warning("Failed to deactivate AVRG [gameId=" + _gameId +
                            ", cause=" + cause + "].");        
            }, shutdown));
    }

    /**
     * Returns the backend if we're currently in an AVRG, null otherwise.
     */
    public function getAVRGameBackend () :AVRGameBackend
    {
        return (_ctrl == null) ? null : _ctrl.getAVRGameBackend();
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
