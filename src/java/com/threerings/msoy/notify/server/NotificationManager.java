//
// $Id$

package com.threerings.msoy.notify.server;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.data.NotifyMessage;

/**
 * Manages most notifications to users.
 */
public class NotificationManager
{
    public NotificationManager ()
    {
    }

    /**
     * Notify the specified user of the specified notification.
     */
    public void notify (MemberName name, Notification note)
    {
        MemberObject target = getUser(name);
        if (target != null) {
            if (note.isPersistent()) {
                target.notify(note);
            } else {
                dispatchChatOnlyNotification(target, note.getAnnouncement());
            }
        }
    }

    /**
     * Notify the specified member that an invitation they sent has been accepted
     * by a new whirled member!
     */
    public void notifyInvitationAccepted (
        MemberName inviter, String inviteeDisplayName, String inviteeEmail)
    {
        // avoid creating any objects unless the target is around to receive it
        MemberObject target = getUser(inviter);
        if (target != null) {
            dispatchChatOnlyNotification(target, 
                MessageBundle.tcompose("m.invite_accepted", inviteeEmail, inviteeDisplayName));
        }
    }

    /**
     * Notify the specified user that they've leveled up to the specified level.
     */
    public void notifyLeveledUp (MemberObject target, int newLevel)
    {
        if (target != null) {
            // leveling up is pretty much chat-only, but it apparently happens prior
            // to the user logging all the way in. Perhaps we need some new thinking here...
            target.notify(new LevelUpNotification(newLevel));
        }
    }

    /**
     * Convenience method to look up a user by username.
     */
    protected MemberObject getUser (MemberName name)
    {
        return MsoyServer.lookupMember(name);
    }

    /**
     * Dispatch a chat-only notification to the specified target.
     */
    protected void dispatchChatOnlyNotification (MemberObject target, String msg)
    {
        SpeakProvider.sendMessage(target, new NotifyMessage(msg));
    }

    /** The next id we'll use for a notification. */
    protected int _nextNotificationId;
}
