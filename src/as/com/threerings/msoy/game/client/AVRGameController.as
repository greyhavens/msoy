//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Controller;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.WorldClient;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.client.GameContext;

public class AVRGameController extends Controller
    implements Subscriber
{
    public static const log :Log = Log.getLog(AVRGameController);

    public function AVRGameController (ctx :GameContext, gameOid :int)
    {
        super();

        _gctx = ctx;
        _mctx = ctx.getWorldContext();

        _mctx.getClient().addEventListener(
            WorldClient.MINI_WILL_CHANGE, function (ev :ValueEvent) :void {
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

        miniWillChange(_mctx.getWorldClient().isMinimized());
    }

    // from interface Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        log.warning("Failed to subscribe to world game object [oid=" + oid +
                    ", cause=" + cause + "].");
        _gameObj = null;
    }

    public function tutorialEvent (eventName :String) :void
    {
        if (_panel) {
            _panel.tutorialEvent(eventName);
        }
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
        _mctx.getClient().removeEventListener(WorldClient.MINI_WILL_CHANGE, miniWillChange);
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
