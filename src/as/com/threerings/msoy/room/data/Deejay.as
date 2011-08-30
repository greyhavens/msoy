//
// $Id$

// GENERATED PREAMBLE START
package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.Comparable;
import com.threerings.util.Comparators;
import com.threerings.util.Long;

import com.threerings.presents.dobj.DSet_Entry;

// GENERATED PREAMBLE END

import com.threerings.util.Comparators;

// GENERATED CLASSDECL START
public class Deejay implements Comparable, DSet_Entry
{
// GENERATED CLASSDECL END

// GENERATED STREAMING START
    public var memberId :int;

    public var startedAt :Long;

    public function readObject (ins :ObjectInputStream) :void
    {
        memberId = ins.readInt();
        startedAt = ins.readLong();
    }

    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(memberId);
        out.writeLong(startedAt);
    }

// GENERATED STREAMING END

    public function compareTo (o :Object) :int
    {
        var that :Deejay = Deejay(o);
        return Comparators.compareNumbers(this.startedAt.toNumber(), that.startedAt.toNumber());
    }

    public function getKey () :Object
    {
        return memberId;
    }

// GENERATED CLASSFINISH START
}
}
// GENERATED CLASSFINISH END

