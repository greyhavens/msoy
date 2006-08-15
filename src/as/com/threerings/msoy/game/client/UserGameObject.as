package com.threerings.msoy.game.client {

import flash.errors.IllegalOperationError;

import flash.events.Event;
import flash.events.EventDispatcher;

import com.threerings.util.Name;

import com.threerings.msoy.msoy_internal;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.game.data.FlashGameObject;

import com.metasoy.game.GameObject;
import com.metasoy.game.PropertyChangedEvent;

public class UserGameObject extends EventDispatcher
    implements GameObject
{
    public function UserGameObject (ctx :MsoyContext, gameObj :FlashGameObject)
    {
        _ctx = ctx;
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
        var names :Array = new Array();
        for each (var name :Name in _gameObj.players) {
            names.push((name == null) ? null : name.toString());
        }
        return names;
    }

    // from GameObject
    public function getMyIndex () :int
    {
        return _gameObj.getPlayerIndex(_ctx.getClientObject().getVisibleName());
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
    msoy_internal function dispatch (event :Event) :void
    {
        try {
            super.dispatchEvent(event);
        } catch (err :Error) {
            var log :Log = Log.getLog(this);
            log.warning("Error dispatching event to user game.");
            log.logStackTrace(err);
        }
    }

    protected var _ctx :MsoyContext;

    protected var _gameObj :FlashGameObject;
}
}
