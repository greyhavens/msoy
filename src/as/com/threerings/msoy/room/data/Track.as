//
// $Id$

// GENERATED PREAMBLE START
package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.Comparable;
import com.threerings.util.Comparators;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.item.data.all.Audio;

// GENERATED PREAMBLE END

// GENERATED CLASSDECL START
public class Track implements DSet_Entry, Comparable
{
// GENERATED CLASSDECL END

// GENERATED STREAMING START
    public var audio :Audio;

    public var order :int;

    public function readObject (ins :ObjectInputStream) :void
    {
        audio = ins.readObject(Audio);
        order = ins.readInt();
    }

    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(audio);
        out.writeInt(order);
    }

// GENERATED STREAMING END

    public function getKey () :Object
    {
        return audio.getKey();
    }

    public function compareTo (o :Object) :int
    {
        var that :Track = Track(o);
        // Most recent tracks first
        return Comparators.compareInts(this.order, that.order);
    }

// GENERATED CLASSFINISH START
}
}
// GENERATED CLASSFINISH END

