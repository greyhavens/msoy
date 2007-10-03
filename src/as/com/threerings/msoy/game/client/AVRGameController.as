//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Controller;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.client.GameContext;

public class AVRGameController extends Controller
    implements Subscriber
{
    public static const log :Log = Log.getLog(AVRGameController);

    public function AVRGameController (ctx :WorldContext, gctx :GameContext, gameOid :int)
    {
        super();

        _mctx = ctx;
        _gctx = gctx;

        _subscriber = new SafeSubscriber(gameOid, this)
        _subscriber.subscribe(_gctx.getDObjectManager());

        _panel = new AVRGamePanel(_mctx, _gctx, this);
        setControlledPanel(_panel);

        _mctx.getTopPanel().setBottomPanel(_panel);
    }

    public function forceShutdown () :void
    {
        shutdown();
    }

    // from interface Subscriber
    public function objectAvailable (obj :DObject) :void
    {
        if (_gameObj) {
            log.warning("Already subscribed to game object [gameObj=" + _gameObj + "]");
            return;
        }

        _gameObj = (obj as AVRGameObject);
        _panel.init(_gameObj);
    }

    // from interface Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        log.warning("Failed to subscribe to world game object [oid=" + oid +
                    ", cause=" + cause + "].");
        _gameObj = null;
    }

    public function getAVRGameObject () :AVRGameObject
    {
        return _gameObj;
    }

    protected function shutdown () :void
    {
        _subscriber.unsubscribe(_mctx.getDObjectManager());
        _mctx.getTopPanel().clearBottomPanel(_panel);
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _gameObj :AVRGameObject;
    protected var _subscriber :SafeSubscriber;
    protected var _panel :AVRGamePanel;
}
}

import flash.display.Loader;

import mx.containers.Canvas;

import com.threerings.flash.MediaContainer;
import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.client.AVRGameControlBackend;
import com.threerings.msoy.game.client.AVRGameController;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.AVRGameObject;

class AVRGamePanel extends Canvas
{
    public static const log :Log = Log.getLog(AVRGamePanel);

    public function AVRGamePanel (
        mctx :WorldContext, gctx :GameContext, ctrl :AVRGameController)
    {
        super();

        _mctx = mctx;
        _gctx = gctx;
        _ctrl = ctrl;
    }

    override protected function createChildren () :void
    {
        _mediaHolder = new MediaContainer();
        this.rawChildren.addChildAt(_mediaHolder, 0);

        // TODO: A nice wee X
        var quit :CommandButton = new CommandButton(MsoyController.LEAVE_AVR_GAME);
        quit.label = Msgs.GAME.get("b.leave_world_game");
        this.addChildAt(quit, 0);
        this.height = 100;
    }

    public function init (gameObj :AVRGameObject) :void
    {
        _gameObj = gameObj;
        _mediaHolder.setMedia(gameObj.gameMedia.getMediaPath());
        _backend = new AVRGameControlBackend(_mctx, _gctx, _gameObj, _ctrl);
        _backend.init(Loader(_mediaHolder.getMedia()));
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :MediaContainer;
    protected var _gameObj :AVRGameObject;
    protected var _backend :ControlBackend;
}
