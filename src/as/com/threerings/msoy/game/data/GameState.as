//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Contains a single state datum for a member for a game.
 */
public class GameState
    implements Streamable, DSet_Entry
{
    /** The key for this state datum. */
    public var key :String;

    /** The actual contents of the state datum. */
    public var value :ByteArray;

    /** Whether or not this datum will be persistently stored between sessions. */
    public var persistent :Boolean;

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
        persistent = ins.readBoolean();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(key);
        out.writeField(value);
        out.writeBoolean(persistent);
    }

}
}
