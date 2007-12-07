//
// $Id$

package com.threerings.msoy.notify.client {

import flash.utils.Dictionary;

import com.threerings.io.TypedArray;
import com.threerings.util.MessageBundle;

import com.threerings.flex.CommandButton;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.MemberService;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.notify.data.NotifyMessage;
import com.threerings.msoy.notify.data.Notification;

public class NotificationDirector extends BasicDirector
    implements AttributeChangeListener, SetListener
{
    public function NotificationDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Display all currently pending notifications.
     */
    public function displayNotifications () :void
    {
        if (_notifyPanel != null) {
            _notifyPanel.close();
            return;
        }

        _notifyPanel = new NotificationDisplay(_wctx);
        _notifyPanel.open();
        for each (var notif :Notification in _wctx.getMemberObject().notifications.toArray()) {
            if (notif.isPersistent()) {
                _notifyPanel.addNotification(notif);
            }
        }
        updateNotificationButton(true);
    }

    public function notificationPanelClosed () :void
    {
        _notifyPanel = null;
        updateNotificationButton(false);
    }

    public function acknowledgeNotification (id :int) :void
    {
        var ids :TypedArray = TypedArray.create(int);
        ids.push(id);
        acknowledgeNotifications(ids);
    }

    protected function acknowledgeNotifications (notifyIds :TypedArray /* of int */) :void
    {
        // record that we've sent off an ack to the server, from here on out we act like
        // each of these notifications doesn't exist on the client...
        for each (var id :int in notifyIds) {
            _acked[id] = true;
        }
        _msvc.acknowledgeNotifications(_wctx.getClient(), notifyIds, new ReportingListener(_wctx));
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);
        client.getClientObject().addListener(this);

        // and, let's always update the control bar button
        updateNotifications();
        showStartupNotifications();
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        super.registerServices(client);
        client.addServiceGroup(MsoyCodes.MEMBER_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);
        _msvc = (client.requireService(MemberService) as MemberService);
    }

    // from interface AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NEW_MAIL_COUNT) {
            if (event.getValue() > 0 && !event.getOldValue()) {
                dispatchChatNotification("m.new_mail");
            }
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NOTIFICATIONS) {
            updateNotifications();

        } else if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = event.getEntry() as FriendEntry;
            dispatchChatNotification(MessageBundle.tcompose(
                "m.friend_added", entry.name, entry.name.getMemberId()));
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            // display the message if the status changed
            if (entry.online != oldEntry.online) {
                dispatchChatNotification(MessageBundle.tcompose(
                    entry.online ? "m.friend_online" : "m.friend_offline",
                    entry.name, entry.name.getMemberId()));
            }
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NOTIFICATIONS) {
            // delete any memories associated with this notification
            var id :int = event.getKey() as int;
            delete _acked[id];
            delete _announced[id];
            // and update the button if applicable
            updateNotifications();

        } else if (name == MemberObject.FRIENDS) {
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            dispatchChatNotification(MessageBundle.tcompose("m.friend_removed", oldEntry.name));
        }
    }

    /**
     * Called once the user is logged on and the chat system is ready.
     * Display any notifications that we generate by inspecting the user object,
     * or external data, or whatever.
     */
    protected function showStartupNotifications () :void
    {
        var us :MemberObject = _wctx.getMemberObject();
        if (us.newMailCount > 0) {
            dispatchChatNotification("m.new_mail");
        }

        // and so forth..
    }

    protected function updateNotifications () :void
    {
        var hasPersistent :Boolean = false;
        var shouldAck :TypedArray = TypedArray.create(int);
        for each (var notif :Notification in _wctx.getMemberObject().notifications.toArray()) {
            // skip it if we've already acked it
            if (_acked[notif.id]) {
                continue;
            }
            // if we haven't announced it, do that now
            if (!_announced[notif.id]) {
                _announced[notif.id] = true;
                var ann :String = notif.getAnnouncement();
                if (ann != null) {
                    dispatchChatNotification(ann);
                }
                // if it's announcement-only, ack it
                if (!notif.isPersistent()) {
                    shouldAck.push(notif.id);
                }
            }
            if (notif.isPersistent()) {
                hasPersistent = true;
                // if the panel is currently showing, add the notification
                if (_notifyPanel != null) {
                    _notifyPanel.addNotification(notif);
                }
            }
        }

        // ack those we need to ack
        if (shouldAck.length > 0) {
            acknowledgeNotifications(shouldAck);
        }
        // and update the button if there are any persistent notifications
        var controlBar :ControlBar = _wctx.getTopPanel().getControlBar();
        if (controlBar != null) {
            controlBar.setNotificationsAvailable(hasPersistent);
        }
    }

    /**
     * Dispatch a notification chat message to the user.
     */
    protected function dispatchChatNotification (text :String) :void
    {
        var msg :NotifyMessage = new NotifyMessage(text);
        _wctx.getChatDirector().dispatchMessage(msg, ChatCodes.USER_CHAT_TYPE);
    }

    /**
     * Update the button on the control bar to indicate whether notifications are 
     * showing or not.
     */
    protected function updateNotificationButton (notifsShowing :Boolean) :void
    {
        _wctx.getTopPanel().getControlBar().setNotificationsShowing(notifsShowing);
    }

    protected var _wctx :WorldContext;
    protected var _msvc :MemberService;
    protected var _notifyPanel :NotificationDisplay;

    /** Tracks notifications we've announced. */
    protected var _announced :Dictionary = new Dictionary();

    /** Tracks notifications we've acknowledged but that haven't yet been removed. */
    protected var _acked :Dictionary = new Dictionary();

    /** Contains a list of notification popups currently being shown. */
    protected var _popups :Array = new Array();
}
}
