//
// $Id$

package com.threerings.msoy.world.data {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Equalable;
import com.threerings.util.Hashable;
import com.threerings.util.Util;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.item.web.ItemIdent;

/**
 * Contains a single memory datum for a "smart" item in a scene.
 */
public class MemoryEntry
    implements DSet_Entry, Hashable
{
    /** The item with which this memory datum is associated. */
    public var item :ItemIdent;

    /** The key for this memory datum. */
    public var key :String;

    /** The actual contents of the memory datum. */
    public var value :ByteArray;

    /** Used to track whether the memory is modified and should be flushed when the scene is
     * unloaded. */
    public var modified :Boolean;

    public function MemoryEntry ()
    {
    }

    // from Hashable
    public function hashCode () :int
    {
        return item.hashCode() /* ^ key.hashCode() */; // TODO: add Util.hashCode(String)?
    }

    // from Equalable
    public function equals (other :Object) :Boolean
    {
        // equality is based on key only
        var oentry :MemoryEntry = (other as MemoryEntry);
        return item.equals(oentry.item) && Util.equals(key, oentry.key);
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return this;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        item = (ins.readObject() as ItemIdent);
        key = (ins.readField(String) as String);
        value = (ins.readObject() as ByteArray);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(item);
        out.writeField(key);
        out.writeObject(value);
    }
}
}
