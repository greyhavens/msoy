//
// $Id$

package com.threerings.msoy.notify.client {

import com.threerings.util.ExpiringSet;
import com.threerings.util.MessageBundle;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.ui.AwardPanel;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.notify.data.BadgeEarnedNotification;
import com.threerings.msoy.notify.data.EntityCommentedNotification;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.data.FollowInviteNotification;
import com.threerings.msoy.notify.data.GameInviteNotification;
import com.threerings.msoy.notify.data.GenericNotification;
import com.threerings.msoy.notify.data.InviteAcceptedNotification;
import com.threerings.msoy.notify.data.MoneyNotification;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.data.PartyInviteNotification;
import com.threerings.msoy.notify.data.ReleaseNotesNotification;

public class NotificationDirector extends BasicDirector
    implements AttributeChangeListener, SetListener, MessageListener
{
    // statically reference classes we require
    BadgeEarnedNotification;
    EntityCommentedNotification;
    FollowInviteNotification;
    GameInviteNotification;
    InviteAcceptedNotification;
    LevelUpNotification;
    MoneyNotification;
    PartyInviteNotification;
    ReleaseNotesNotification;

    public function NotificationDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
        _membersLoggingOff = new ExpiringSet(MEMBER_EXPIRE_TIME);
        _currentNotifications = new ExpiringSet(NOTIFICATION_EXPIRE_TIME);
        _currentNotifications.addEventListener(ExpiringSet.ELEMENT_EXPIRED, notificationExpired);

        ctx.getControlBar().setNotificationDisplay(
            _notificationDisplay = new NotificationDisplay(ctx));

        // clear our display if we lose connection to the server
        ctx.getClient().addClientObserver(new ClientAdapter(null, null, null, null, null,
            function () :void {
                _notificationDisplay.clearDisplay();
        }, null, null));
    }

    /**
     * Send a generic notification about the currently playing music.
     */
    public function notifyMusic (song :String, artist :String) :void
    {
        addGenericNotification(MessageBundle.tcompose("m.song", song, artist),
            Notification.BUTTSCRATCHING);
    }

    public function addGenericNotification (
        announcement :String, category :int, sender :MemberName = null) :void
    {
        addNotification(new GenericNotification(announcement, category, sender));
    }

    public function addNotification (notification :Notification) :void
    {
        const sender :MemberName = notification.getSender();
        if (sender != null && _mctx.getMuteDirector().isMuted(sender)) {
            // we have muted this sender: do not notify.
            return;
        }

        // we can't just store the notifications in the array, because some notifications may be
        // identical (bob invites you to play captions twice within 15 minutes);
        _currentNotifications.add(_lastId++);
        _notifications.push(notification);
        _notificationDisplay.displayNotification(notification);
    }

    public function getCurrentNotifications () :Array
    {
        return _notifications;
    }

    /**
     * Display an award in an {@ AwardPanel}.
     */
    public function displayAward (award :Object) :void
    {
        if (_awardPanel == null) {
            _awardPanel = new AwardPanel(_mctx);
        }
        _awardPanel.displayAward(award);

        // also gin up a local notification and post that
        var msg :String = null;
        if (award is Trophy) {
            var trophy :Trophy = (award as Trophy);
            msg = MessageBundle.tcompose("m.trophy_earned", trophy.name, trophy.gameId);
        } else if (award is Item) {
            var prize :Item = (award as Item);
            msg = MessageBundle.tcompose("m.prize_earned", prize.name, prize.getType());
        } else if (award is Badge) {
            var badge :Badge = (award as Badge);
            var nm :String = _mctx.xlate(MsoyCodes.PASSPORT_MSGS, badge.nameProp, badge.levelName);
            msg = MessageBundle.tcompose("m.badge_awarded", nm, badge.coinValue);
        }
        if (msg != null) {
            addGenericNotification(msg, Notification.BUTTSCRATCHING);
        }
    }

    // from interface AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NEW_MAIL_COUNT) {
            const diff :int = int(event.getValue()) - int(event.getOldValue());
            if (diff > 0) {
                notifyNewMail(diff);
            }
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = event.getEntry() as FriendEntry;
            var notif :String = MessageBundle.tcompose("m.friend_added",
                entry.name, entry.name.getMemberId());
            if (entry.online) {
                notif += " " + MessageBundle.tcompose("m.friend_visit", entry.name.getMemberId());
            }
            addGenericNotification(notif, Notification.PERSONAL);
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            var memberId :int = entry.name.getMemberId();

            // display a message if they just signed on
            if (entry.online != oldEntry.online) {
                // show friends logging on in the notification area
                if (entry.online) {
                    // if they weren't listed in the set, report them as newly coming online
                    if (!_membersLoggingOff.remove(entry)) {
                        addGenericNotification(
                            MessageBundle.tcompose("m.friend_online", entry.name, memberId),
                            Notification.BUTTSCRATCHING);
                    }

                } else {
                    _membersLoggingOff.add(entry);
                }
            }

            // they may have changed something else we'd like to know about.
            if (MemberName.BY_DISPLAY_NAME(entry.name, oldEntry.name) != 0) {
                addGenericNotification(
                    MessageBundle.tcompose("m.friend_name_changed", entry.name, memberId,
                        oldEntry.name),
                    Notification.BUTTSCRATCHING);

            } else if (entry.status != oldEntry.status) {
                addGenericNotification(
                    MessageBundle.tcompose("m.friend_status_changed", entry.name, memberId,
                        entry.status),
                    Notification.BUTTSCRATCHING);
            }
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            addGenericNotification(
                MessageBundle.tcompose("m.friend_removed", oldEntry.name,
                    oldEntry.name.getMemberId()),
                Notification.PERSONAL);
        }
    }

    // from interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NOTIFICATION) {
            var notification :Notification = event.getArgs()[0] as Notification;
            if (notification != null) {
                addNotification(notification);
            }
        }
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);
        client.getClientObject().addListener(this);

        // and, let's always update the control bar button
        if (!_didStartupNotifs) {
            _didStartupNotifs = true;
            showStartupNotifications();
        }

        // tell the server to go ahead and dispatch any notifications it had saved up.
        const msvc :MemberService = client.requireService(MemberService) as MemberService;
        msvc.dispatchDeferredNotifications(_ctx.getClient());
    }

    /**
     * Called once the user is logged on and the chat system is ready.
     * Display any notifications that we generate by inspecting the user object,
     * or external data, or whatever.
     */
    protected function showStartupNotifications () :void
    {
        const clobj :Object = _ctx.getClient().getClientObject();
        const newMail :int = (clobj is MemberObject) ? MemberObject(clobj).newMailCount : 0;
        if (newMail > 0) {
            notifyNewMail(newMail);
        }
    }

    protected function notifyNewMail (count :int) :void
    {
        addGenericNotification(MessageBundle.tcompose("m.new_mail", count), Notification.PERSONAL);
    }

    protected function notificationExpired (event :ValueEvent) :void
    {
        // all we currently need to do is check if this list is empty, and if so, have the
        // display fade it out.
        if (_currentNotifications.size() == 0) {
            _notifications.length = 0;
            _notificationDisplay.clearDisplay();
            return;
        }

        // we'll always get one event per element, so we can just lop the oldest element off
        _notifications.splice(0, 1);
    }

    /** Give members 15 seconds to get back on before we announce that they're left */
    protected static const MEMBER_EXPIRE_TIME :int = 15;

    /** Give notifications 15 minutes to be relevant. */
    protected static const NOTIFICATION_EXPIRE_TIME :int = 15 * 60; // in seconds

    protected var _mctx :MsoyContext;

    protected var _notificationDisplay :NotificationDisplay;

    /** An ExpiringSet to track members that may only be switching servers. */
    protected var _membersLoggingOff :ExpiringSet;

    /** An ExpiringSet to track which notifications are relevant */
    protected var _currentNotifications :ExpiringSet;

    protected var _didStartupNotifs :Boolean;
    protected var _lastId :uint = 0;
    protected var _notifications :Array = [];
    protected var _awardPanel :AwardPanel;
}
}
