//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.util.Hashable;
import com.threerings.util.Integer;
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
        var val :int = n1.toString().toLowerCase().localeCompare(n2.toString().toLowerCase());
        // massage the value into something that a Sort can handle
        return (val > 0) ? 1 : ((val == 0) ? 0 : -1);
    };

    /** Used to reprepsent a member that has been deleted but is still referenced as an item
     * creator or mail message sender, etc. */
    public static const DELETED_MEMBER :MemberName = new MemberName("", -1);

    /** The minimum allowable length of a permaname. */
    public static const MINIMUM_PERMANAME_LENGTH :int = 4;

    /** The maximum allowable length of a permaname. */
    public static const MAXIMUM_PERMANAME_LENGTH :int = 12;

    /**
     * Returns true if the supplied member id represents an anonymous viewer.
     */
    public static function isViewer (memberId :int) :Boolean
    {
        return memberId == 0;
    }

    /**
     * Returns true if the supplied member id represents a guest.
     */
    public static function isGuest (memberId :int) :Boolean
    {
        return memberId <= 0;
    }

    /**
     * Create a new MemberName.
     */
    public function MemberName (displayName :String = "", memberId :int = 0)
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

    /**
     * Returns true if this name represents a guest member or a viewer.
     */
    public function isGuest () :Boolean
    {
        return MemberName.isGuest(_memberId);
    }

    /**
     * Returns true if this name represents an anonymous viewer.
     */
    public function isViewer () :Boolean
    {
        return MemberName.isViewer(_memberId);
    }

    /**
     * Ensure that this object is just a base MemberName, and not an extended class with
     * additional information. This is sometimes desired when sending names "over the wire".
     */
    public function toMemberName () :MemberName
    {
        return this;
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return _memberId;
    }

    // from Name
    override public function hashCode () :int
    {
        return _memberId;
    }

    // from Name
    override public function compareTo (o :Object) :int
    {
        // Note: You may be tempted to have names sort by the String value, but Names are used
        // as DSet keys in various places and so each user's must be unique.
        // Use BY_DISPLAY_NAME to sort names for display.
        return Integer.compare(_memberId, (o as MemberName)._memberId);
    }

    // from Name
    override public function equals (other :Object) :Boolean
    {
        return (other is MemberName) && ((other as MemberName)._memberId == _memberId);
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
