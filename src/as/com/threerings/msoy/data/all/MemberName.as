//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.util.Hashable;
import com.threerings.util.Name;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Extends Name with persistent member information.
 */
public class MemberName extends Name
    implements Hashable, DSet_Entry
{
    /** A sort function for sorting Names by their display portion, case insensitively.  */
    public static const BY_DISPLAY_NAME :Function = function (n1 :Name, n2 :Name) :int {
        return n1.toString().toLowerCase().localeCompare(n2.toString().toLowerCase());
    };

    /** Used to reprepsent a member that has been deleted but is still referenced as an item
     * creator or mail message sender, etc. */
    public static const DELETED_MEMBER :MemberName = new MemberName("", -1);

    /** The minimum allowable length of a permaname. */
    public static const MINIMUM_PERMANAME_LENGTH :int = 4;

    /** The maximum allowable length of a permaname. */
    public static const MAXIMUM_PERMANAME_LENGTH :int = 12;

    /** The "member id" used for guests. */
    public static const GUEST_ID :int = 0;

    /**
     * Create a new MemberName.
     */
    public function MemberName (displayName :String = "", memberId :int = GUEST_ID)
    {
        super(displayName);
        _memberId = memberId;
    }

    /**
     * Return the memberId of this user, or 0 if they're a guest.
     */
    public function getMemberId () :int
    {
        return _memberId;
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return _memberId;
    }

    // from Name
    override public function hashCode () :int
    {
        // we return a different hash for guests so that they don't end up all in the same bucket
        // in a Map.
        return (_memberId != GUEST_ID) ? _memberId : super.hashCode();
    }

    // from Name
    override public function compareTo (o :Object) :int
    {
        var that :MemberName = MemberName(o);
        var diff :int = this._memberId - that._memberId;
        // memberId is the primary sorting key
        if (diff != 0) {
            return diff;
        }

        // return 0 if diff is the same (they have the same memberId) UNLESS the member is 0, in
        // which case they're a guest and we compare by name
       return (_memberId != GUEST_ID) ? 0 : BY_DISPLAY_NAME(this, that);
    }

    // from Name
    override public function equals (other :Object) :Boolean
    {
        if (other is MemberName) {
            var otherId :int = (other as MemberName).getMemberId();
            return (otherId == _memberId) && ((_memberId != GUEST_ID) || super.equals(other));
        }
        return false;
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

    // from Name
    override protected function normalize (name :String) :String
    {
        return name; // do not adjust
    }

    /** The member id of the member we represent. */
    protected var _memberId :int;
}
}
