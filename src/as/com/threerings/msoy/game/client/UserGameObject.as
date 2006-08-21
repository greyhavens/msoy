package com.threerings.msoy.game.client {

import flash.errors.IllegalOperationError;

import flash.events.Event;
import flash.events.EventDispatcher;

import flash.utils.IExternalizable;
import flash.utils.ByteArray;

import com.threerings.io.TypedArray;

import com.threerings.util.ClassUtil;
import com.threerings.util.FlashObjectMarshaller;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;

import com.threerings.msoy.msoy_internal;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.game.data.FlashGameObject;
import com.threerings.msoy.game.data.PropertySetEvent;

import com.metasoy.game.GameObject;
import com.metasoy.game.PropertyChangedEvent;

public class UserGameObject extends EventDispatcher
    implements GameObject
{
    public function UserGameObject (ctx :MsoyContext, gameObj :FlashGameObject)
    {
        _ctx = ctx;
        _gameObj = gameObj;
        _gameData = new GameData(this, _gameObj.getUserProps());
    }

    // from GameObject
    public function get data () :Object
    {
        return _gameData;
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
    public function set (propName :String, value :Object, index :int = -1) :void
    {
        validatePropertyChange(propName, value, index);

        _gameObj.postEvent(
            new PropertySetEvent(_gameObj.getOid(), propName, value, index));

        // set it immediately in the game object
        _gameObj.applyPropertySet(propName, value, index);
    }

    // from GameObject
    public function setCollection (collName :String, values :Array) :void
    {
        populateCollection(collName, values, true);
    }

    // from GameObject
    public function addToCollection (collName :String, values :Array) :void
    {
        populateCollection(collName, values, false);
    }

    // from GameObject
    public function pickFromCollection (
        collName :String, count :int, msgOrPropName :String,
        playerIndex :int = -1) :void
    {
        getFromCollection(collName, count, msgOrPropName, playerIndex,
            false, null);
    }

    // from GameObject
    public function dealFromCollection (
        collName :String, count :int, msgOrPropName :String,
        callback :Function = null, playerIndex :int = -1) :void
    {
        getFromCollection(collName, count, msgOrPropName, playerIndex,
            true, callback);
    }

    // from GameObject
    public function mergeCollection (srcColl :String, intoColl :String) :void
    {
        validateName(srcColl);
        validateName(intoColl);
        _gameObj.flashGameService.mergeCollection(_ctx.getClient(),
            srcColl, intoColl, createLoggingListener("mergeCollection"));
    }

    // from GameObject
    public function sendMessage (messageName :String, value :Object) :void
    {
        sendPlayerMessage(-1, messageName, value);
    }

    // from GameObject
    public function sendPlayerMessage (
        playerIdx :int, messageName :String, value :Object) :void
    {
        validateName(messageName);
        validateValue(value);

        var data :ByteArray = FlashObjectMarshaller.encode(value);

        // dispatch the message to all, or just to one player
        if (playerIdx == -1) {
            _gameObj.postMessage(FlashGameObject.USER_MESSAGE,
                [ messageName, data ]);

        } else {
            _gameObj.flashGameService.sendMessage(_ctx.getClient(),
                playerIdx, messageName, data, 
                createLoggingListener("sendPlayerMessage"));
        }
    }

    // from GameObject
    public function sendChat (msg :String) :void
    {
        validateChat(msg);
        // Post a message to the game object, the controller
        // will listen and call localChat().
        _gameObj.postMessage(FlashGameObject.GAME_CHAT, [ msg ]);
    }

    // from GameObject
    public function localChat (msg :String) :void
    {
        validateChat(msg);
        // The sendChat() messages will end up being routed
        // through this method on each client.
        // TODO: make this look distinct from other system chat
        _ctx.displayInfo(null, MessageBundle.taint(msg));
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

    // from GameObject
    public function getTurnHolderIndex () :int
    {
        return _gameObj.getPlayerIndex(_gameObj.turnHolder);
    }

    // from GameObject
    public function getWinnerIndexes () :Array /* of int */
    {
        var arr :Array = new Array();
        if (_gameObj.winners != null) {
            for (var ii :int = 0; ii < _gameObj.winners.length; ii++) {
                if (_gameObj.winners[ii]) {
                    arr.push(ii);
                }
            }
        }
        return arr;
    }

    // from GameObject
    public function isMyTurn () :Boolean
    {
        return _ctx.getClientObject().getVisibleName().equals(
            _gameObj.turnHolder);
    }

    // from GameObject
    public function isInPlay () :Boolean
    {
        return _gameObj.isInPlay();
    }

    // from GameObject
    public function endTurn (nextPlayerIndex :int = -1) :void
    {
        _gameObj.flashGameService.endTurn(_ctx.getClient(), nextPlayerIndex,
            createLoggingListener("endTurn"));
    }

    // from GameObject
    public function endGame (winnerIndex :int, ... rest) :void
    {
        var winners :TypedArray = TypedArray.create(int);
        winners.push(winnerIndex);
        while (rest.length > 0) {
            winners.push(int(rest.shift()));
        }
        _gameObj.flashGameService.endGame(_ctx.getClient(), winners,
            createLoggingListener("endGame"));
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

    /**
     * Create a listener for service requests.
     */
    private function createLoggingListener (
        service :String) :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(function (cause :String) :void {
            Log.getLog(GameObject).warning("Service failure " +
                "[service=" + service + ", cause=" + cause + "].");
        });
    }

    /**
     * Helper method for setCollection and addToCollection.
     */
    private function populateCollection (
        collName :String, values :Array, clearExisting :Boolean) :void
    {
        validateName(collName);
        if (values == null) {
            throw new ArgumentError("Collection values may not be null.");
        }
        validateValue(values);

        var encodedValues :TypedArray = TypedArray.create(ByteArray);
        for (var ii :int = 0; ii < values.length; ii++) {
            encodedValues[ii] = FlashObjectMarshaller.encode(values[ii]);
        }

        _gameObj.flashGameService.addToCollection(
            _ctx.getClient(), collName, encodedValues, clearExisting,
            createLoggingListener("populateCollection"));
    }

    /**
     * Helper method for pickFromCollection and dealFromCollection.
     */
    private function getFromCollection(
        collName :String, count :int, msgOrPropName :String, playerIndex :int,
        consume :Boolean, callback :Function) :void
    {
        validateName(collName);
        validateName(msgOrPropName);
        if (count < 1) {
            throw new ArgumentError("Must retrieve at least one element!");
        }

        var listener :InvocationService_ConfirmListener;
        if (callback != null) {
            // TODO: Figure out the method sig of the callback, and what it
            // means
            var fn :Function = function (cause :String = null) :void {
                if (cause == null) {
                    callback(count);
                } else {
                    callback(parseInt(cause));
                }
            };
            listener = new ConfirmAdapter(fn, fn);

        } else {
            listener = createLoggingListener("getFromCollection");
        }

        _gameObj.flashGameService.getFromCollection(
            _ctx.getClient(), collName, consume, count, msgOrPropName,
            playerIndex, listener);
    }

    /**
     * Verify that the property name / value are valid.
     */
    private function validatePropertyChange (
        propName :String, value :Object, index :int) :void
    {
        validateName(propName);

        // check that we're setting an array element on an array
        if (index >= 0) {
            if (!(_gameData[propName] is Array)) {
                throw new ArgumentError("Property " + propName +
                    " is not an Array.");
            }
        }

        // validate the value too
        validateValue(value);
    }

    /**
     * Verify that the specified name is valid.
     */
    private function validateName (name :String) :void
    {
        if (name == null) {
            throw new ArgumentError(
                "Property, message, and collection names must not be null.");
        }
    }

    private function validateChat (msg :String) :void
    {
        if (StringUtil.isBlank(msg)) {
            throw new ArgumentError(
                "Empty chat may not be displayed.");
        }
    }

    /**
     * Verify that the value is legal to be streamed to other clients.
     */
    private function validateValue (value :Object) :void
    {
        if (value == null) {
            return;

        } else if (value is IExternalizable) {
            throw new ArgumentError(
                "IExternalizable is not yet supported");

        } else if (value is Array) {
            if (ClassUtil.getClass(value) != Array) {
                // We can't allow arrays to be serialized as IExternalizables
                // because we need to know element values (opaquely) on the
                // server. Also, we don't allow other types because we wouldn't
                // create the right class on the other side.
                throw new ArgumentError(
                    "Custom array subclasses are not supported");
            }
            // then, continue on with the sub-properties check (below)

        } else {
            var type :String = typeof(value);
            if (type == "number" || type == "string" || type == "boolean" ) {
                // kosher!
                return;
            }
            if (ClassUtil.getClass(value) != Object) {
                throw new ArgumentError(
                    "Non-simple properties may not be set.");
            }
            // fall through and verify the object's sub-properties
        }

        // check sub-properties (of arrays and objects)
        for each (var arrValue :Object in (value as Array)) {
            validateValue(arrValue);
        }
    }

    protected var _ctx :MsoyContext;

    protected var _gameObj :FlashGameObject;

    protected var _gameData :GameData;
}
}
