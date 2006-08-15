package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

import com.threerings.util.ClassUtil;
import com.threerings.util.FlashObjectMarshaller;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.msoy_internal;

import com.threerings.parlor.game.data.GameObject;

public class FlashGameObject extends GameObject
{
    public function FlashGameObject ()
    {
        _impl = new GameObjectImpl(this);
        _gameData = new GameData(this, _props);
    }

    public function getGameData () :Object
    {
        return _gameData;
    }

    public function getImpl () :GameObjectImpl
    {
        return _impl;
    }

    /**
     * Called by entities to request a property set from the server.
     */
    internal function requestPropertyChange (
        propName :String, value :Object, index :int = -1,
        setNow :Boolean = true) :void
    {
        validatePropertyChange(propName, value, index);

        // Post the event
        postEvent(new PropertySetEvent(_oid, propName, value, index));

        if (setNow) {
            if (index >= 0) {
                (_props[propName] as Array)[index] = value;

            } else {
                _props[propName] = value;
            }
        }
    }

    protected function validatePropertyChange (
        propName :String, value :Object, index :int) :void
    {
        if (propName == null) {
            throw new ArgumentError();
        }

        // validate the property
        if (index >= 0) {
            if (!(_props[propName] is Array)) {
                throw new ArgumentError("Property " + propName +
                    " is not an Array.");
            }

        } else if ((value is Array) && (ClassUtil.getClass(value) != Array)) {
            // We can't allow arrays to be serialized as IExternalizables
            // because we need to know element values (opaquely) on the
            // server. Also, we don't allow other types because we wouldn't
            // create the right class on the other side.
            throw new ArgumentError(
                "Custom array subclasses are not supported");
        }
    }

    /**
     * Called by a PropertySetEvent to enact a property change.
     */
    public function applyPropertySet (
        propName :String, value :Object, index :int) :void
    {
        var oldValue :Object = _props[propName];
        _props[propName] = value;

        _impl.msoy_internal::dispatch(propName, value, oldValue, index);
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        throw new Error("Un-needed");
        /*

        var keys :Array = [];
        var key :String;
        for (key in _props) {
            keys.push(key);
        }
        out.writeInt(keys.length);
        for (key in keys) {
            out.writeUTF(key);
            out.writeObject(FlashObjectMarshaller.encode(_props[key]));
        }
        */
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        var count :int = ins.readInt();
        while (count-- > 0) {
            var key :String = ins.readUTF();
            var data :Object = ins.readObject();
            if (data is Array) {
                // read an array value
                var ta :Array = (data as Array);
                var array :Array = [];

                for (var ii :int = 0; ii < ta.length; ii++) {
                    array[ii] = FlashObjectMarshaller.decode(
                        ta[ii] as ByteArray);
                }
                _props[key] = array;

            } else {
                _props[key] = FlashObjectMarshaller.decode(
                    data as ByteArray);
            }
        }
    }

    /** The current state of game data. */
    protected var _gameData :GameData;

    /** The proxy that implements the GameObject API. */
    protected var _impl :GameObjectImpl;

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}
}
