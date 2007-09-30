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
    }

    public function forceShutdown () :void
    {
        shutdown();
    }

    // from interface Subscriber
    public function objectAvailable (obj :DObject) :void
    {
        if (_gameObj) {
            log.warning("Already subscribed to object [gameObj=" + _gameObj + "]");
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

    protected function shutdown () :void
    {
        _subscriber.unsubscribe(_mctx.getDObjectManager());
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _gameObj :AVRGameObject;
    protected var _subscriber :SafeSubscriber;
    protected var _panel :AVRGamePanel;
}
}

import flash.display.Loader;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.ControlBackend;

import com.threerings.msoy.game.client.AVRGameControlBackend;
import com.threerings.msoy.game.client.AVRGameController;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.AVRGameObject;

class AVRGamePanel extends MediaContainer
{
    public function AVRGamePanel (
        mctx :WorldContext, gctx :GameContext, ctrl :AVRGameController)
    {
        super(null);

        _mctx = mctx;
        _gctx = gctx;
        _ctrl = ctrl;
    }

    public function init (gameObj :AVRGameObject) :void
    {
        _gameObj = gameObj;
        setMedia(gameObj.gameMedia.getMediaPath());
        _backend = new AVRGameControlBackend(_mctx, _gameObj, _ctrl);
        _backend.init(Loader(_media));
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _gameObj :AVRGameObject;
    protected var _backend :ControlBackend;
}
