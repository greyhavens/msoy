package com.threerings.msoy.game.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.events.Event;

import mx.containers.Canvas;
import mx.containers.VBox;

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
        _gameObject = (plobj as FlashGameObject);
        notifyOfGame(this);
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
    protected function notifyOfGame (disp :DisplayObject) :void
    {
        if (disp is Game) {
            (disp as Game).setGameObject(_gameObject.getImpl());
        }

        if (disp is DisplayObjectContainer) {
            var cont :DisplayObjectContainer = (disp as DisplayObjectContainer);
            for (var ii :int = 0; ii < cont.numChildren; ii++) {
                notifyOfGame(cont.getChildAt(ii));
            }
        }
    }

    protected var _ctx :MsoyContext;
    protected var _ctrl :FlashGameController;

    protected var _gameView :MsoySprite;
    protected var _gameObject :FlashGameObject;
}
}
