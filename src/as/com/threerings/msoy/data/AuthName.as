//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Name;

import com.threerings.msoy.data.all.MemberName;

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

    /** Returns true if this name represents a guest member or a viewer. */
    public function isGuest () :Boolean
    {
        return MemberName.isGuest(_memberId);
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
