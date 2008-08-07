//
// $Id$

package com.threerings.msoy.room.data {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Equalable;
import com.threerings.util.Hashable;
import com.threerings.util.StringUtil;
import com.threerings.util.Util;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Contains a single property for shared room state.
 */
public class RoomPropertyEntry
    implements DSet_Entry, Hashable
{
    public static const MAX_ENTRIES :int = 16;
    public static const MAX_KEY_LENGTH :int = 64;
    public static const MAX_VALUE_LENGTH :int = 256;

    public static const MAX_ENCODED_PROPERTY_LENGTH :int = MAX_ENTRIES * MAX_VALUE_LENGTH;

    /** The key for this property. */
    public var key :String;

    /** The actual contents of the property. */
    public var value :ByteArray;

    public function RoomPropertyEntry (key: String = null, value :ByteArray = null)
    {
        this.key = key;
        this.value = value;
    }

    // from Hashable
    public function hashCode () :int
    {
        return StringUtil.hashCode(key);
    }

    // from Equalable
    public function equals (other :Object) :Boolean
    {
        return (other is RoomPropertyEntry) &&
            Util.equals(key, (other as RoomPropertyEntry).key);
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return key;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        key = (ins.readField(String) as String);
        value = (ins.readField(ByteArray) as ByteArray);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(key);
        out.writeField(value);
    }
}
}
