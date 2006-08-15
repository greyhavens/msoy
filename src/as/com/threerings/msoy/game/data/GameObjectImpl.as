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

    // from GameObject
    public function get data () :Object
    {
        return _gameObj.getGameData();
    }

    // from GameObject
    public function set (propName :String, value :Object, index :int = -1) :void
    {
        _gameObj.requestPropertyChange(propName, value, index);
    }

    // from GameObject
    public function get (propName :String, index :int = -1) :Object
    {
        var value :Object = data[propName];
        if (index >= 0) {
            if (value is Array) {
                return (value as Array)[index];

            } else {
                throw new ArgumentError("Property " + propName +
                    " is not an array.");
            }
        }
        return value;
    }

    // from GameObject
    public function getPlayerNames () :Array
    {
        return []; // TODO
    }

    override public function willTrigger (type :String) :Boolean
    {
        throw new IllegalOperationError();
    }

    override public function dispatchEvent (event :Event) :Boolean
    {
        // Ideally we want to not be an IEventDispatcher so that people
        // won't try to do this on us, but if we do that, then some other
        // object will be the target during dispatch, and that's confusing.
        // It's really nice to be able to 
        throw new IllegalOperationError();
    }

    /**
     * Secret function to dispatch property changed events.
     */
    msoy_internal function dispatch (
        propName :String, newValue :Object, oldValue :Object, index :int) :void
    {
        super.dispatchEvent(
            new PropertyChangedEvent(propName, newValue, oldValue, index));
    }

    protected var _gameObj :FlashGameObject;
}
}
