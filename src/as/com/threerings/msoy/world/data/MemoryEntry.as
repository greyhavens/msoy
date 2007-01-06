//
// $Id$

package com.threerings.msoy.world.data {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Comparable;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Contains a single memory datum for a scene entity.
 */
public class MemoryEntry
    implements DSet_Entry, Comparable
{
    /** The entity with which this memory is associated. */
    public var entity :EntityIdent;

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

    // from interface Comparable
    public function compareTo (other :Object) :int
    {
        var oentry :MemoryEntry = (other as MemoryEntry);
        var rv :int = entity.compareTo(oentry.entity);
        return (rv != 0) ? rv : key.localeCompare(oentry.key);
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return this;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        entity = (ins.readObject() as EntityIdent);
        key = (ins.readField(String) as String);
        value = (ins.readObject() as ByteArray);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(entity);
        out.writeField(key);
        out.writeObject(value);
    }
}
}
