package com.threerings.msoy.game.data {

import flash.errors.IllegalOperationError;

import flash.events.Event;
import flash.events.EventDispatcher;

import com.threerings.msoy.msoy_internal;

import com.metasoy.game.GameObject;
import com.metasoy.game.PropertyChangedEvent;

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

    /**
     * Secret function to dispatch property changed events.
     */
    msoy_internal function dispatch (
        property :String, newValue :Object, oldValue :Object) :void
    {
        super.dispatchEvent(
            new PropertyChangedEvent(property, newValue, oldValue));
    }

    protected var _gameObj :FlashGameObject;
}
}
