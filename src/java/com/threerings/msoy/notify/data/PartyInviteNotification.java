//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.data.all.MemberName;

@com.threerings.util.ActionScript(omit=true)
public class PartyInviteNotification extends Notification
{
    /** Suitable for unserialization. */
    public PartyInviteNotification () {}

    /** Normal constructor. */
    public PartyInviteNotification (MemberName inviter, int partyId, String partyName)
    {
        _inviter = inviter.toMemberName();
        _partyId = partyId;
        _partyName = partyName;
    }

    @Override
    public String getAnnouncement ()
    {
        return null; // implemented on the client
    }

    @Override
    public MemberName getSender ()
    {
        return _inviter;
    }

    protected MemberName _inviter;
    protected int _partyId;
    protected String _partyName;
}
