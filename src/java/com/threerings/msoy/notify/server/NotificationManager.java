//
// $Id$

package com.threerings.msoy.notify.server;

import java.util.List;

import com.google.inject.Singleton;

import com.threerings.presents.annotation.EventThread;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberNodeActions;

import com.threerings.msoy.notify.data.EntityCommentedNotification;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.data.FollowInviteNotification;
import com.threerings.msoy.notify.data.InviteAcceptedNotification;
import com.threerings.msoy.notify.data.GameInviteNotification;

import com.threerings.msoy.data.all.MemberName;

import static com.threerings.msoy.Log.log;

/**
 * Manages most notifications to users.
 */
@Singleton @EventThread
public class NotificationManager
{
    /**
     * Sends a notification to the specified member.
     * @return true if the notification was sent or queued, or false if the notification
     * was discarded because the recipient is unavailable to the sender.
     */
    public boolean notify (MemberObject target, Notification note)
    {
        // suppress notifications from people we're unavailable to
        if (!isSendable(target, note)) {
            return false;
        }

        // if they have not yet reported in with a call to dispatchDeferredNotifications then we
        // need to queue this notification up rather than dispatch it directly
        final MemberLocal local = target.getLocal(MemberLocal.class);
        if (local.deferredNotifications != null) {
            local.deferredNotifications.add(note);
        } else {
            target.postMessage(MemberObject.NOTIFICATION, note);
        }
        return true;
    }

    /**
     * Dispatches a batch of notifications all at once.
     */
    public void notify (MemberObject target, List<Notification> notes)
    {
        final MemberLocal local = target.getLocal(MemberLocal.class);
        if (local.deferredNotifications != null) {
            local.deferredNotifications.addAll(notes);
        } else {
            target.startTransaction();
            try {
                for (Notification note : notes) {
                    if (isSendable(target, note)) {
                        target.postMessage(MemberObject.NOTIFICATION, note);
                    }
                }
            } finally {
                target.commitTransaction();
            }
        }
    }

    /**
     * Return true if the specified notification is sendable to the recipient.
     * A note is unsenable if the target is unavailable to the sender.
     */
    public boolean isSendable (MemberObject target, Notification note)
    {
        MemberName sender = note.getSender();
        return (sender == null) || target.isAvailableTo(sender.getMemberId());
    }

    /**
     * Notify the specified member that an invitation they sent has been accepted by a new whirled
     * member!
     */
    public void notifyInvitationAccepted (
        int inviterId, MemberName invitee, String inviteeEmail)
    {
        MemberNodeActions.sendNotification(inviterId,
            new InviteAcceptedNotification(invitee, inviteeEmail));
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
    public boolean notifyGameInvite (MemberObject target, MemberName inviter,
                                  String game, int gameId)
    {
        return notify(target, new GameInviteNotification(inviter, game, gameId));
    }

    /**
     * Notifies the target player that they've been invited to follow someone.
     */
    public boolean notifyFollowInvite (MemberObject target, MemberName inviter)
    {
        return notify(target, new FollowInviteNotification(inviter));
    }

    /**
     * Dispatches any deferred notifications for the specified member and marks them as ready to
     * receive notifications in real time.
     */
    public void dispatchDeferredNotifications (MemberObject memobj)
    {
        final MemberLocal local = memobj.getLocal(MemberLocal.class);
        if (local.deferredNotifications != null) {
            List<Notification> notes = local.deferredNotifications;
            local.deferredNotifications = null;
            notify(memobj, notes);
        } else {
            log.warning("Client requested deferred notifications, but they've already been sent",
                "who", memobj.who());
        }
    }
}
