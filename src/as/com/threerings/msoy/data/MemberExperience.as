//
// $Id: ItemListInfo.as 8847 2008-04-15 17:18:01Z nathan $

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Long;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Contains metadata for a list of items.
 */
public class MemberExperience
    implements Streamable, DSet_Entry
{
    public var dateOccurred :Long;
    public var action :int;
    public var data :Object;

    public function MemberExperience ()
    {
        // used for deserialization
    }

    // from DSet.Entry
    public function getKey () :Object
    {
        return dateOccurred;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        dateOccurred = (ins.readField(Long) as Long);
        action = ins.readByte();
        data = ins.readObject();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(dateOccurred);
        out.writeByte(action);
        out.writeObject(data);
    }
}
}
