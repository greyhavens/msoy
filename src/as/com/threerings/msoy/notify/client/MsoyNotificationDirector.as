//
// $Id$

package com.threerings.msoy.notify.client {
import flash.utils.setTimeout;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.Set;
import com.threerings.util.Sets;
import com.threerings.util.StringUtil;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.orth.notify.client.NotificationDirector;
import com.threerings.orth.notify.client.NotificationDisplay;
import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyService;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.notify.data.BadgeEarnedNotification;
import com.threerings.msoy.notify.data.EntityCommentedNotification;
import com.threerings.msoy.notify.data.FollowInviteNotification;
import com.threerings.msoy.notify.data.GameInviteNotification;
import com.threerings.msoy.notify.data.InviteAcceptedNotification;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.data.MoneyNotification;
import com.threerings.msoy.notify.data.PartyInviteNotification;
import com.threerings.msoy.notify.data.PokeNotification;
import com.threerings.msoy.ui.AwardPanel;
import com.threerings.msoy.utils.Capabilities;

public class MsoyNotificationDirector extends NotificationDirector
    implements AttributeChangeListener, SetListener
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
    PokeNotification;

    public function MsoyNotificationDirector (ctx :MsoyContext)
    {
        super(ctx, MemberObject.NOTIFICATION);
        _mctx = ctx;

        var ndheight :int = ctx.getControlBar().getControlHeight();
        ctx.getControlBar().setNotificationDisplay(
            _notificationDisplay = new MsoyNotificationDisplay(ctx, ndheight));
    }

    override protected function getDisplay () :NotificationDisplay
    {
        return _notificationDisplay;
    }

    override protected function dispatchDeferredNotifications () :void
    {
        // tell the server to go ahead and dispatch any notifications it had saved up.
        MsoyService(_ctx.getClient().requireService(MsoyService))
            .dispatchDeferredNotifications();
    }

    override protected function isMuted (sender :Name) :Boolean
    {
        return super.isMuted(sender);
    }

    /**
     * Send a generic notification about the currently playing music.
     */
    public function notifyMusic (owner :MemberName, song :String, artist :String) :void
    {
        var text :String = (owner != null) ?
            MessageBundle.tcompose("m.song_owned", owner.toString(), owner.getId(), song, artist) :
            MessageBundle.tcompose("m.song", song, artist);

        addGenericNotification(text, Notification.BUTTSCRATCHING);
    }

    /**
     * Turn a game system message into a notification.
     */
    public function addGameSystemMessage (bundle :String, msg :String) :void
    {
        if (bundle == null) {
            msg = MessageBundle.taint(msg);
        } else {
            msg = MessageBundle.qualify(bundle, msg);
        }
        // TODO: a "GAME" notification level?
        addGenericNotification(msg, Notification.PERSONAL);
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
            addGenericNotification(msg, Notification.PERSONAL);
        }
    }

    /**
     * Displays a notification (generally to permaguests) saying something to the effect of "You
     * earned coins! Sign up to save those mofongos!"
     */
    public function displayPayoutUpsell (coins :int, hasCookies :Boolean) :void
    {
        if (coins > 1) { // save ourselves the plurality headache
            addGenericNotification(MessageBundle.tcompose("m.payout_upsell", coins),
                Notification.PERSONAL);
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
            // if they weren't listed in the set, report them as newly coming online
            if (!_membersLoggingOff.remove(entry)) {
                addGenericNotification(
                    MessageBundle.tcompose("m.friend_online", entry.name, entry.name.getId()),
                    Notification.BUTTSCRATCHING, entry.name);
            }
        }

// TODO: Restore new friend notification
//        if (name == MemberObject.FRIENDS) {
//            var entry :FriendEntry = event.getEntry() as FriendEntry;
//            var online :String = entry.online ?
//                Msgs.NOTIFY.get("m.friend_visit", entry.name.getMemberId()) : "";
//            var notif :String = MessageBundle.tcompose("m.friend_added",
//                entry.name, entry.name.getMemberId(), online);
//            addGenericNotification(notif, Notification.PERSONAL, entry.name);
//        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            var memberId :int = entry.name.getId();

            if (entry.name.toString() != oldEntry.name.toString()) {
                addGenericNotification(
                    MessageBundle.tcompose("m.friend_name_changed", entry.name, memberId,
                        oldEntry.name),
                    Notification.BUTTSCRATCHING, entry.name);

            } else if (entry.status != oldEntry.status) {
                _statusDelays.add(memberId);
            }
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();
        // TODO: dear god fix this hackery
        if (name == MemberObject.FRIENDS) {
            _membersLoggingOff.add(event.getOldEntry());
        }
    }

    override protected function showStartupNotifications () :void
    {
        super.showStartupNotifications();

        const memobj :MemberObject = _mctx.getMemberObject();
        if (memobj.newMailCount > 0) {
            notifyNewMail(memobj.newMailCount);
        }

        // check their flash version
        if (Capabilities.getFlashMajorVersion() < FLASH_UPGRADE_VERSION) {
            // just show this in 1 seconds
            setTimeout(addGenericNotification, 1 * 1000, "m.flash_upgrade", Notification.SYSTEM);
        }
    }

    protected function notifyNewMail (count :int) :void
    {
        addGenericNotification(MessageBundle.tcompose("m.new_mail", count), Notification.PERSONAL);
    }

    protected function statusDelayExpired (memberId :int) :void
    {
        var entry :FriendEntry = _mctx.getMemberObject().friends.get(memberId) as FriendEntry;
        if (entry != null) {
            var statusString :String =
                _mctx.getChatDirector().filter(entry.status, entry.name, false);
            if (!StringUtil.isBlank(statusString)) {
                addGenericNotification(
                    MessageBundle.tcompose("m.friend_status_changed", entry.name, memberId,
                        statusString),
                    Notification.BUTTSCRATCHING, entry.name);
            }
        }
    }

    /** Give members 15 seconds to get back on before we consider them a fresh logon. */
    protected static const MEMBER_EXPIRE_TIME :int = 15 * 1000; // 15 seconds

    protected static const STATUS_UPDATE_DELAY :int = 15 * 1000; // 15 seconds

    protected static const FLASH_UPGRADE_VERSION :int = 10;

    protected var _mctx :MsoyContext;

    protected var _notificationDisplay :MsoyNotificationDisplay;

    /** An Expiring Set to track members that may only be switching servers. */
    protected var _membersLoggingOff :Set = Sets.newBuilder(FriendEntry)
        .makeExpiring(MEMBER_EXPIRE_TIME).build();

    /** Tracks when it's ok to show a status update for a member. */
    protected var _statusDelays :Set = Sets.newBuilder(int)
        .makeExpiring(STATUS_UPDATE_DELAY, statusDelayExpired).build();

    protected var _awardPanel :AwardPanel;
}
}
