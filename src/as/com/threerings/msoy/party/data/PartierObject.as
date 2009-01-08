//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.BodyObject;

import com.threerings.msoy.data.all.VizMemberName;

/**
 * Contains information on a party member logged into the server.
 */
public class PartierObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static const MEMBER_NAME :String = "memberName";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public var memberName :VizMemberName;

    // from BodyObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        memberName = VizMemberName(ins.readObject());
    }

    /**
     * Returns this member's unique id.
     */
    public function getMemberId () :int
    {
        return memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return memberName.isGuest();
    }
}
}
