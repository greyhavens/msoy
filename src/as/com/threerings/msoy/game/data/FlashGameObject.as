package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

import com.threerings.util.FlashObjectMarshaller;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.parlor.game.data.GameObject;

public class FlashGameObject extends GameObject
{
    public function FlashGameObject ()
    {
        _impl = new GameObjectImpl(this);
    }

    public function getGameData () :Object
    {
        return _gameData;
    }

    public function getImpl () :GameObjectImpl
    {
        return _impl;
    }

    protected function propertySet (property :String, data :Object) :void
    {
        _gameData[property] = data;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        var keys :Array = [];
        var key :String;
        for (key in _gameData) {
            keys.push(key);
        }
        out.writeInt(keys.length);
        for (key in keys) {
            out.writeUTF(key);
            out.writeObject(FlashObjectMarshaller.encode(_gameData[key]));
        }
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        var count :int = ins.readInt();
        while (count-- > 0) {
            var key :String = ins.readUTF();
            var bytes :ByteArray = (ins.readObject() as ByteArray);
            _gameData[key] = FlashObjectMarshaller.decode(bytes);
        }
    }

    /** The current state of game data. */
    protected var _gameData :Object = new Object();

    /** The proxy that implements the GameObject API. */
    protected var _impl :GameObjectImpl;
}
}
