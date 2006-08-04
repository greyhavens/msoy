//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Name;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Extends Name with persistent member information.
 */
public class MemberName extends Name
{
    /**
     * Create a new MemberName.
     */
    public function MemberName (displayName :String = null, memberId :int = -1)
    {
        super(displayName);
        _memberId = memberId;
    }

    /**
     * Return the memberId of this user, or -1 if they're a guest.
     */
    public function getMemberId () :int
    {
        return _memberId;
    }

    override public function equals (other :Object) :Boolean
    {
        if (other is MemberName) {
            var otherId :int = (other as MemberName).getMemberId();
            return (otherId == _memberId) &&
                ((_memberId != -1) || super.equals(other));
        }
        return false;
    }

    override protected function normalize (name :String) :String
    {
        return name; // do not adjust
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeInt(_memberId);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        _memberId = ins.readInt();
    }

    /** The member id of the member we represent. */
    protected var _memberId :int;
}
}
