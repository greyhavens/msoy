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
     */
    public void notify (MemberObject target, Notification note)
    {
        // if they have not yet reported in with a call to dispatchDeferredNotifications then we
        // need to queue this notification up rather than dispatch it directly
        final MemberLocal local = target.getLocal(MemberLocal.class);
        if (local.deferredNotifications != null) {
            local.deferredNotifications.add(note);
        } else {
            target.postMessage(MemberObject.NOTIFICATION, note);
        }
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
            for (Notification note : notes) {
                target.postMessage(MemberObject.NOTIFICATION, note);
            }
            target.commitTransaction();
        }
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
    public void notifyGameInvite (MemberObject target, MemberName inviter,
                                  String game, int gameId)
    {
        if (target.isAvailableTo(inviter.getMemberId())) {
            notify(target, new GameInviteNotification(inviter, game, gameId));
        }
    }

    /**
     * Notifies the target player that they've been invited to follow someone.
     */
    public void notifyFollowInvite (MemberObject target, MemberName inviter)
    {
        if (target.isAvailableTo(inviter.getMemberId())) {
            notify(target, new FollowInviteNotification(inviter));
        }
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
            log.warning("Client requested deferred notifications, but they've already been " +
                        "dispatched [who=" + memobj.who() + "].");
        }
    }
}
