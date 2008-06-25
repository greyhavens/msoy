//
// $Id$

package com.threerings.msoy.notify.server;

import com.threerings.presents.annotation.EventThread;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.server.MemberNodeActions;

import com.threerings.msoy.notify.data.EntityCommentedNotification;
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
        MemberNodeActions.sendNotification(inviterId, 
            new InviteAcceptedNotification(inviteeEmail, inviteeDisplayName));
    }

    /**
     * Notifies the member that someone commented on something they own or created
     */
    public void notifyEntityCommented (
        int targetId, int entityType, int entityId, String entityName)
    {
        MemberNodeActions.sendNotification(targetId,
            new EntityCommentedNotification(entityType, entityId, entityName));
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
