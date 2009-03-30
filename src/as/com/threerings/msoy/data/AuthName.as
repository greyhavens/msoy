//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.ClassUtil;
import com.threerings.util.Name;

/**
 * Represents the authentication username for our various sessions (world, game, party).
 */
public class AuthName extends Name
{
    /** Used for unserializing. We never create these directly in Flash. */
    public function AuthName ()
    {
    }

    /** Returns this member's unique id. */
    public function getMemberId () :int
    {
        return _memberId;
    }

    // from Name
    override public function hashCode () :int
    {
        return _memberId;
    }

    // from Name
    override public function equals (other :Object) :Boolean
    {
        return (other != null) && ClassUtil.isSameClass(this, other) &&
            (AuthName(other).getMemberId() == getMemberId());
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _memberId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(_memberId);
    }

    protected var _memberId :int;
}
}
