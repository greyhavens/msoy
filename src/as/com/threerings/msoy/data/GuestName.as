//
// $Id$

package com.threerings.msoy.data {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.ClassUtil;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.msoy.data.all.MemberName;

/**
 * Used to track guests via a unique token for the duration of their session.
 */
public class GuestName extends Name
{
    // from Name
    override public function hashCode () :int
    {
        return StringUtil.hashCode(_sessionToken.toString());
    }

    // from Name
    override public function equals (other :Object) :Boolean
    {
        if (ClassUtil.isSameClass(this, other)) {
            return _sessionToken == (other as GuestName)._sessionToken;
        } else {
            return false;
        }
    }

    // from Name
    override public function compareTo (o :Object) :int
    {
        // guests sort lower than members
        if (!ClassUtil.isSameClass(this, other)) {
            return -1;
        }
        // if our session tokens are equal return 0, otherwise sort by name
        var other :GuestName = (o as GuestName);
        return (_sessionToken == other._sessionToken) ? 0 : super.compareTo(o);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _sessionToken = ins.readField(ByteArray) as ByteArray;
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(_sessionToken);
    }

    /** A unique token assigned to us at logon time. */
    protected var _sessionToken :ByteArray;
}
}
