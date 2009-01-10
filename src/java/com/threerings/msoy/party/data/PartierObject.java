//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.crowd.data.BodyObject;

import com.threerings.msoy.data.all.VizMemberName;

/**
 * Contains information on a party member logged into the server.
 */
public class PartierObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>partyId</code> field. */
    public static final String PARTY_ID = "partyId";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public VizMemberName memberName;

    /** The party to which this partier is party. */
    public int partyId;

    /**
     * Returns this member's unique id.
     */
    public int getMemberId ()
    {
        return memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public boolean isGuest ()
    {
        return memberName.isGuest();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>memberName</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMemberName (VizMemberName value)
    {
        VizMemberName ovalue = this.memberName;
        requestAttributeChange(
            MEMBER_NAME, value, ovalue);
        this.memberName = value;
    }

    /**
     * Requests that the <code>partyId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPartyId (int value)
    {
        int ovalue = this.partyId;
        requestAttributeChange(
            PARTY_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.partyId = value;
    }
    // AUTO-GENERATED: METHODS END
}
