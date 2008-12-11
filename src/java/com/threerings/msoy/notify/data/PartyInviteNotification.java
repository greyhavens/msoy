//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.msoy.data.all.MemberName;

public class PartyInviteNotification extends Notification
{
    /** Suitable for unserialization. */
    public PartyInviteNotification () {}
    
    /** Normal constructor. */
    public PartyInviteNotification (MemberName inviter, int partyId, String partyName)
    {
        _inviter = inviter;
        _partyId = partyId;
        _partyName = partyName;
    }

    @Override
    public String getAnnouncement ()
    {
        return null; // implemented on the client
    }

    protected MemberName _inviter;
    protected int _partyId;
    protected String _partyName;
}
