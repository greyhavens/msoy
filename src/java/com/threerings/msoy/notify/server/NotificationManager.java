//
// $Id$

package com.threerings.msoy.notify.server;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.notify.data.GuestInviteNotification;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.data.NotifyMessage;

import static com.threerings.msoy.Log.log;

/**
 * Manages most notifications to users.
 */
public class NotificationManager
{
    /**
     * Notify the specified user of the specified notification.
     */
    public void notify (MemberObject target, Notification note)
    {
        if (note.isPersistent()) {
            target.notify(note);
        } else {
            dispatchChatOnlyNotification(target, note.getAnnouncement());
        }
    }

    /**
     * Notify the specified member that an invitation they sent has been accepted by a new whirled
     * member!
     */
    public void notifyInvitationAccepted (
        MemberName inviter, String inviteeDisplayName, String inviteeEmail)
    {
        // avoid creating any objects unless the target is around to receive it
        // PEER TODO: user may be resolved on another world server
        MemberObject target = MsoyServer.lookupMember(inviter);
        if (target != null) {
            dispatchChatOnlyNotification(target, MessageBundle.tcompose(
                "m.invite_accepted", inviteeEmail, inviteeDisplayName));
        }
    }

    /**
     * Notify the specified guest that they've received an invitation to Whirled.
     */
    public void notifyIssuedInvitation (MemberName guest, String inviteId)
    {
        MemberObject target = MsoyServer.lookupMember(guest);
        if (guest != null) {
            target.notify(new GuestInviteNotification(inviteId));
        }
    }

    /**
     * Notifies the target player that they've been invited to play a game.
     */
    public void notifyGameInvite (MemberObject target, String inviter, int inviterId,
                                  String game, int gameId)
    {
        dispatchChatOnlyNotification(
            target, MessageBundle.tcompose("m.game_invite", inviter, inviterId, game, gameId));
    }

    /**
     * Dispatch a chat-only notification to the specified target.
     */
    protected void dispatchChatOnlyNotification (MemberObject target, String msg)
    {
        SpeakUtil.sendMessage(target, new NotifyMessage(msg));
    }

    /** The next id we'll use for a notification. */
    protected int _nextNotificationId;
}
