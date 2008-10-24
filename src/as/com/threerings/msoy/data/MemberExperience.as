//
// $Id: ItemListInfo.as 8847 2008-04-15 17:18:01Z nathan $

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.ClassUtil;
import com.threerings.util.Hashable;
import com.threerings.util.Long;
import com.threerings.util.Util;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Contains metadata for a list of items.
 */
public class MemberExperience
    implements Hashable, Streamable, DSet_Entry
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
        return this;
    }

    // from Hashable
    public function hashCode () :int
    {
        // this is not the best hashCode, but it's what we've got here on the as side..
        return action;
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        if (this == other) {
            return true;
        }
        if (other == null || !ClassUtil.isSameClass(this, other)) {
            return false;
        }
        const that :MemberExperience = MemberExperience(other);
        return (this.action == that.action) && Util.equals(this.data, that.data) &&
            Util.equals(this.dateOccurred, that.dateOccurred);
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
