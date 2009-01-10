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
    PartyAuthName // filled into username

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static const MEMBER_NAME :String = "memberName";

    /** The field name of the <code>partyId</code> field. */
    public static const PARTY_ID :String = "partyId";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public var memberName :VizMemberName;

    /** The party to which this partier is party. */
    public var partyId :int;

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

    // from BodyObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        memberName = VizMemberName(ins.readObject());
        partyId = ins.readInt();
    }
}
}
