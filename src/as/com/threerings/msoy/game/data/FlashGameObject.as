package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

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
        property :String, value :Object) :void
    {
        postEvent(new PropertySetEvent(_oid, property, value));
    }

    /**
     * Called by a PropertySetEvent to enact a property change.
     */
    public function applyPropertySet (property :String, data :Object) :void
    {
        var oldValue :Object = _props[property];
        _props[property] = data;

        _impl.msoy_internal::dispatch(property, data, oldValue);
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

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
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        var count :int = ins.readInt();
        while (count-- > 0) {
            var key :String = ins.readUTF();
            var bytes :ByteArray = (ins.readObject() as ByteArray);
            _props[key] = FlashObjectMarshaller.decode(bytes);
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
