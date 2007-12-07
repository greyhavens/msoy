//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Controller;
import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.BaseClient;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.client.GameContext;

public class AVRGameController extends Controller
    implements Subscriber
{
    public static const log :Log = Log.getLog(AVRGameController);

    public function AVRGameController (ctx :GameContext, gameId :int, gameOid :int)
    {
        super();

        _gctx = ctx;
        _mctx = ctx.getWorldContext();
        _gameId = gameId;

        _mctx.getClient().addEventListener(
            BaseClient.MINI_WILL_CHANGE, function (ev :ValueEvent) :void {
                miniWillChange(ev.value);
            });

        _panel = new AVRGamePanel(_mctx, _gctx, this);
        setControlledPanel(_panel);

        _subscriber = new SafeSubscriber(gameOid, this)
        _subscriber.subscribe(_gctx.getDObjectManager());
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

    // called by the Panel when it's finished loading the AVRG media
    public function gameIsReady () :void
    {
        // if we played a lobbied game recently, tell the tutorial code
        if (_mctx.getGameDirector().popMostRecentLobbyGame() != 0) {
            tutorialEvent("gamePlayed");
        }

        var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
        if (view) {
            view.avrGameAvailable(_gameId, _panel.getAVRGameBackend());
        }
    }

    public function tutorialEvent (eventName :String) :void
    {
        if (_panel) {
            _panel.tutorialEvent(eventName);
        }
    }

    public function getGameId () :int
    {
        return _gameId;
    }

    public function getAVRGameObject () :AVRGameObject
    {
        return _gameObj;
    }

    protected function miniWillChange (mini :Boolean) :void
    {
        if (_mctx.getGameDirector().isPlayingTutorial()) {
            tutorialEvent(mini ? "willMinimize" : "willUnminimize");
        }
    }

    protected function shutdown () :void
    {
        _mctx.getClient().removeEventListener(BaseClient.MINI_WILL_CHANGE, miniWillChange);
        _subscriber.unsubscribe(_mctx.getDObjectManager());

        var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
        if (view) {
            view.avrGameAvailable(_gameId, null);
        }

        _panel.shutdown();
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _gameId :int;
    protected var _gameObj :AVRGameObject;
    protected var _subscriber :SafeSubscriber;
    protected var _panel :AVRGamePanel;
}
}
