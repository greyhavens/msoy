//
// $Id$

package com.threerings.msoy.notify.server;

import com.threerings.presents.annotation.EventThread;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.data.FollowInviteNotification;
import com.threerings.msoy.notify.data.InviteAcceptedNotification;
import com.threerings.msoy.notify.data.GameInviteNotification;

/**
 * Manages most notifications to users.
 */
@EventThread
public class NotificationManager
{
    /**
     * Notify the specified user of the specified notification.
     */
    public void notify (MemberObject target, Notification note)
    {
        target.postMessage(MemberObject.NOTIFICATION, note);
    }

    /**
     * Notify the specified member that an invitation they sent has been accepted by a new whirled
     * member!
     */
    public void notifyInvitationAccepted (
        int inviterId, String inviteeDisplayName, String inviteeEmail)
    {
        // avoid creating any objects unless the target is around to receive it
        // PEER TODO: user may be resolved on another world server
        MemberObject target = MsoyServer.lookupMember(inviterId);
        if (target != null) {
            notify(target, new InviteAcceptedNotification(inviteeEmail, inviteeDisplayName));
        }
    }

    /**
     * Notifies the target player that they've been invited to play a game.
     */
    public void notifyGameInvite (MemberObject target, String inviter, int inviterId,
                                  String game, int gameId)
    {
        if (target.isAvailableTo(inviterId)) {
            notify(target, new GameInviteNotification(inviter, inviterId, game, gameId));
        }
    }

    /**
     * Notifies the target player that they've been invited to follow someone.
     */
    public void notifyFollowInvite (MemberObject target, String inviter, int inviterId)
    {
        if (target.isAvailableTo(inviterId)) {
            notify(target, new FollowInviteNotification(inviter, inviterId));
        }
    }
}
