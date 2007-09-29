//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

public class GameState
    implements Streamable, DSet_Entry
{
    public var key :String;

    public var value :ByteArray;

    // from DSet_Entry
    public function getKey () :Object
    {
        return key;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        key = ins.readField(String) as String;
        value = ins.readField(ByteArray) as ByteArray;
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(key);
        out.writeField(value);
    }
}
}
