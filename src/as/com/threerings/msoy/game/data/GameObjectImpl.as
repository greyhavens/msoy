package com.threerings.msoy.game.data {

import flash.errors.IllegalOperationError;

import flash.events.Event;
import flash.events.EventDispatcher;

import com.metasoy.game.GameObject;

public class GameObjectImpl extends EventDispatcher
    implements GameObject
{
    public function GameObjectImpl (gameObj :FlashGameObject)
    {
        _gameObj = gameObj;
    }

    public function get data () :Object
    {
        return _gameObj.getGameData();
    }

    override public function willTrigger (type :String) :Boolean
    {
        throw new IllegalOperationError();
    }

    override public function dispatchEvent (event :Event) :Boolean
    {
        throw new IllegalOperationError();
    }

    protected var _gameObj :FlashGameObject;
}
}
