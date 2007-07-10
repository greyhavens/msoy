//
// $Id$

package com.threerings.msoy.notify.client {

import com.threerings.util.MessageBundle;

import com.threerings.flex.CommandButton;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.notify.data.NotifyMessage;
import com.threerings.msoy.notify.data.Notification;

public class NotificationDirector extends BasicDirector
    implements SetListener
{
    public function NotificationDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Display all currently pending notifications.
     */
    public function displayNotifications (btn :CommandButton) :void
    {
        if (_notifyPanel != null) {
            _notifyPanel.close();
            return;
        }

        _notifyPanel = new NotificationDisplay(
            _wctx, _wctx.getMemberObject().notifications.toArray());
        _notifyPanel.open();

        _notifyBtn = btn;
    }

    public function notificationPanelClosed () :void
    {
        _notifyPanel = null;
        _notifyBtn.selected = false;
    }

    public function acknowledgeNotification (notifyId :int) :void
    {
        _msvc.acknowledgeNotification(_wctx.getClient(), notifyId, new ReportingListener(_wctx));
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);
        client.getClientObject().addListener(this);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        super.registerServices(client);
        client.addServiceGroup(MsoyCodes.BASE_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);
        _msvc = (client.requireService(MemberService) as MemberService);
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NOTIFICATIONS) {
            updateNotifications();
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
                var text :String = MessageBundle.tcompose(
                    entry.online ? "m.friend_online" : "m.friend_offline",
                    entry.name, entry.name.getMemberId());
                var msg :NotifyMessage = new NotifyMessage(text);
                _wctx.getChatDirector().dispatchMessage(msg, ChatCodes.USER_CHAT_TYPE);
            }
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NOTIFICATIONS) {
            updateNotifications();
        }
    }

    protected function updateNotifications () :void
    {
        _wctx.getTopPanel().getControlBar().setNotificationsAvailable(
            _wctx.getMemberObject().notifications.size() > 0);
    }

    protected var _wctx :WorldContext;

    protected var _msvc :MemberService;

    protected var _notifyPanel :NotificationDisplay;

    protected var _notifyBtn :CommandButton;

    /** Contains a list of notification popups currently being shown. */
    protected var _popups :Array = new Array();
}
}
