package com.threerings.msoy.game.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.events.Event;

import flash.utils.Dictionary;

import mx.containers.Canvas;
import mx.containers.VBox;

import mx.core.Container;
import mx.core.IChildList;

import mx.utils.DisplayUtil;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.chat.client.ChatTextArea;

import com.threerings.msoy.world.client.MsoySprite;

import com.threerings.msoy.game.data.FlashGameConfig;
import com.threerings.msoy.game.data.FlashGameObject;

import com.metasoy.game.Game;

public class FlashGameView extends VBox
    implements PlaceView
{
    public function FlashGameView (ctx :MsoyContext, ctrl :FlashGameController)
    {
        _ctx = ctx;
        _ctrl = ctrl;

        // add a listener so that we hear about all new children
        addEventListener(Event.ADDED, childAdded);

        var cfg :FlashGameConfig = (ctrl.getPlaceConfig() as FlashGameConfig);
        _gameView = new MsoySprite(cfg.game);
        addChild(_gameView);

        addChild(new ChatTextArea(ctx));
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // don't start notifying anything of the game until we've
        // notified the game manager that we're in the game
        // (done in GameController, and it uses callLater, so we do it twice!)
        _ctx.getClient().callLater(function () :void {
            _ctx.getClient().callLater(function () :void {
                _gameObject = (plobj as FlashGameObject);

                // we don't want to notify the view that 
                notifyOfGame(_gameView);
            });
        });
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        _gameObject = null;
    }

    /**
     * Handle ADDED events.
     */
    protected function childAdded (event :Event) :void
    {
        if (_gameObject != null) {
            notifyOfGame(event.target as DisplayObject);
        }
    }

    /**
     * Find any children of the specified object that implement
     * com.metasoy.game.Game and provide them with the GameObject.
     */
    protected function notifyOfGame (root :DisplayObject) :void
    {
        DisplayUtil.walkDisplayObjects(root,
            function (disp :DisplayObject) :void
            {
                if (disp is Game) {
                    // only notify the Game if we haven't seen it before
                    if (null == _seenGames[disp]) {
                        (disp as Game).setGameObject(_ctrl.userGameObj);
                        _seenGames[disp] = true;
                    }
                }
            });
    }

    protected var _ctx :MsoyContext;
    protected var _ctrl :FlashGameController;

    /** A weak-key hash of the Game interfaces we've already seen. */
    protected var _seenGames :Dictionary = new Dictionary(true);

    protected var _gameView :MsoySprite;
    protected var _gameObject :FlashGameObject;
}
}
