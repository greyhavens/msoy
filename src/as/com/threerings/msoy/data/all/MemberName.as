//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.orth.data.OrthName;

/**
 * Extends Name with persistent member information.
 */
public class MemberName extends OrthName
    implements DSet_Entry
{
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
     * Create a new MemberName.
     */
    public function MemberName (displayName :String = "", memberId :int = 0)
    {
        super(displayName, memberId);
    }

    /**
     * Returns true if this name represents an anonymous viewer.
     */
    public function isViewer () :Boolean
    {
        return MemberName.isViewer(getId());
    }

    /**
     * Ensure that this object is just a base MemberName, and not an extended class with
     * additional information. This is sometimes desired when sending names "over the wire".
     */
    public function toMemberName () :MemberName
    {
        return this;
    }

    // from Name
    override public function equals (other :Object) :Boolean
    {
        return (other is MemberName) && super.equals(other);
    }
}
}
