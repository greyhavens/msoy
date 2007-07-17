//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.msoy.server.ServerConfig;

/**
 * Notifies a guest that they've been invited.
 */
public class GuestInviteNotification extends Notification
{
    public GuestInviteNotification ()
    {
    }

    @ActionScript(omit=true)
    public GuestInviteNotification (String inviteId) 
    {
        _inviteId = inviteId;
        _serverUrl = ServerConfig.getServerURL();
    }

    // from Notification
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose("m.guest_invite");
    }

    protected String _inviteId;
    protected String _serverUrl;
}
