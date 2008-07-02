//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

/**
 * Notifies a user that an invitation was accepted
 */
public class InviteAcceptedNotification extends Notification
{
    public InviteAcceptedNotification ()
    {
    }

    @ActionScript(omit=true)
    public InviteAcceptedNotification (
        String inviteeEmail, String inviteeDisplayName, int inviteeId)
    {
        _inviteeEmail = inviteeEmail;
        _inviteeDisplayName = inviteeDisplayName;
        _inviteeId = inviteeId;
    }

    // from Notification
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose(
            "m.invite_accepted", _inviteeEmail, _inviteeDisplayName, _inviteeId);
    }

    protected String _inviteeEmail;
    protected String _inviteeDisplayName;
    protected int _inviteeId;
}
