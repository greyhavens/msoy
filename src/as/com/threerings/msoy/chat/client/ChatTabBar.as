//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.MovieClip;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.utils.Timer;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.core.ScrollPolicy;

import com.threerings.flex.FlexWrapper;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyService;

import com.threerings.msoy.chat.data.MsoyChatChannel;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RoomName;

/**
 * Displays our chat tabs.
 */
public class ChatTabBar extends HBox
    implements ChatDisplay, SetListener
{
    public function ChatTabBar (ctx :MsoyContext)
    {
        super();
        _ctx = ctx;
        setStyle("horizontalGap", 0);
        explicitMinWidth = 0;
        percentWidth = 100;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override public function setActualSize (w :Number, h :Number) :void
    {
        super.setActualSize(w, h);
        rawChildren.setChildIndex(_scrollLayer, rawChildren.numChildren - 1);
        _scrollLayer.setActualSize(w, h);
        checkScrollTabs();
        horizontalScrollPosition = _scrollPercent * maxHorizontalScrollPosition;
    }

    public function get selectedIndex () :int
    {
        return _selectedIndex;
    }

    public function set selectedIndex (ii :int) :void
    {
        var activeTab :ChatTab;
        if (_selectedIndex != -1) {
            activeTab = _tabs[_selectedIndex] as ChatTab;
            activeTab.setVisualState(ChatTab.UNSELECTED);
            _selectedIndex = -1;
        }

        if (_tabs.length == 0) {
            return;
        }

        _selectedIndex = (ii + _tabs.length) % _tabs.length;
        activeTab = _tabs[_selectedIndex] as ChatTab;
        activeTab.setVisualState(ChatTab.SELECTED);
        _ctx.getControlBar().setChatColor(getChatColor(activeTab.getChannelType()));
        _ctx.getControlBar().setChatAllowed(activeTab.isSpeakableChannel());
        var overlay :ChatOverlay = _ctx.getTopPanel().getChatOverlay();
        if (overlay != null) {
            overlay.setLocalType(activeTab.localtype);
        }
    }

    /**
     * Called by the chat director to let us know when we enter a primary place.
     * TODO: listen to the LocationDirector directly? Right now there's one for each client
     * connection, so that may not make sense.
     */
    public function setPlaceName (name :Name) :void
    {
        if (name == null) {
            name = createDefaultPlaceName(); // will end up disabling speaking (sceneId 0)

        } else if (!(name is RoomName)) {
            // TODO: presently we reuse the same 'place' tab for all primary place chat. ugh
            // So right here we're taking a game name and shoving it into a room
            name = new RoomName(name.toString(), 1);
        }

        var roomTabIx :int = getLocalTypeIndex(ChatCodes.PLACE_CHAT_TYPE);
        if (roomTabIx == -1) {
            log.warning("This shouldn't happen", new Error());
        }
        // modify the place chat tab, which should always be up
        _tabs[roomTabIx].setChannel(MsoyChatChannel.makeRoomChannel(RoomName(name)));
        // jiggle the selectedIndex to fix any reactions
        if (roomTabIx == _selectedIndex) {
            selectedIndex = roomTabIx; // no underscore, use the setter, get the side-effects
        }
    }

    public function memberObjectUpdated (obj :MemberObject) :void
    {
        obj.addListener(this);
    }

    // from SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        // NOOP
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            // If a friend changes their display name, change the text in the tab
            var newEntry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            var newNameStr :String = newEntry.name.toString();
            if (newNameStr != oldEntry.name.toString()) {
                for each (var tab :ChatTab in _tabs) {
                    if (tab.getTellMemberId() == newEntry.name.getId()) {
                        tab.text = newNameStr;
                        break;
                    }
                }
            }
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        // NOOP
    }

    /**
     * @param fromUserAction if this is the result of the user choosing to open the channel.
     */
    public function openChannelTab (channel :MsoyChatChannel, fromUserAction :Boolean) :void
    {
        var index :int = getLocalTypeIndex(channel.toLocalType());
        if (index != -1) {
            if (fromUserAction) {
                selectedIndex = index;
            }
            return;
        }

        // this tab hasn't been created yet.
        addTab(new ChatTab(_ctx, this, channel, ""+channel.ident));
        if (fromUserAction) {
            selectedIndex = _tabs.length - 1;

            // when the user opens a group channel explicitly, we want to make sure
            // we always tell the server that they want to hear it, just in case.
            if (channel.type == MsoyChatChannel.GROUP_CHANNEL) {
                (_ctx.getClient().getService(MsoyService) as MsoyService).setHearingGroupChat(
                    (channel.ident as GroupName).getGroupId(), true, _ctx.confirmListener());
            }
        }
    }

    public function containsTab (channel :MsoyChatChannel) :Boolean
    {
        return getLocalTypeIndex(channel.toLocalType()) != -1;
    }

    public function getCurrentChannel () :MsoyChatChannel
    {
        if (_selectedIndex == -1) {
            log.warning("getCurrentChannel() with no selected tab", new Error());
            return null;
        }
        if (_tabs.length == 0) {
            log.warning("getCurrentChannel() without tabs", "selectedIndex", _selectedIndex,
                new Error());
            return null;
        }
        if (_selectedIndex >= _tabs.length) {
            log.warning("getCurrentChannel() indexing error", "selectedIndex", _selectedIndex,
                        "tabs.length", _tabs.length, new Error());
            return null;
        }
        return (_tabs[_selectedIndex] as ChatTab).channel;
    }

    public function getCurrentLocalType () :String
    {
        return _tabs.length > 0 ? (_tabs[_selectedIndex] as ChatTab).localtype :
            ChatCodes.PLACE_CHAT_TYPE;
    }

    public function chatTabIndex (tab :ChatTab) :int
    {
        return _tabs.indexOf(tab);
    }

    // from ChatDisplay
    public function clear () :void
    {
        // remove any shinies
        for each (var tab :ChatTab in _tabs) {
            if (tab.getVisualState() == ChatTab.ATTENTION) {
                tab.setVisualState(ChatTab.UNSELECTED);
            }
        }
    }

    // from ChatDisplay
    public function displayMessage (msg :ChatMessage) :void
    {
        if (_tabs.length == 0) {
            // if we receive any messages before we have any tabs, there's nothing to do here.
            return;
        }

        var index :int = -1;
        // If this is a SystemMessage, broadcast with PLACE_CHAT_TYPE localtype,
        // it's aimed for the first tab, regardless of that tab's actual localtype
        if (msg.localtype == ChatCodes.PLACE_CHAT_TYPE &&
            (msg is SystemMessage ||
            (msg is UserMessage && (msg as UserMessage).mode == ChatCodes.BROADCAST_MODE))) {
            index = 0;
        } else {
            index = getLocalTypeIndex(msg.localtype);
        }

        if (index != -1 && index != _selectedIndex) {
            var tab :ChatTab = _tabs[index] as ChatTab;
            tab.setVisualState(ChatTab.ATTENTION);
            if (_rightScroll.visible && tab.x > (horizontalScrollPosition + width)) {
                _rightScroll.play();
            } else if (_leftScroll.visible && (tab.x + tab.width) < horizontalScrollPosition) {
                _leftScroll.play();
            }
        }

        if (index != -1 || !(msg is UserMessage)) {
            // if we already took care of tab state for the new message, or its not a UserMessage
            // we can leave here.
            return;
        }

        // if this is a message from a member or from jabber, we can pop up a new tab, and set it
        // to ATTENTION
        var umsg :UserMessage = msg as UserMessage;
        if (MsoyChatChannel.typeOf(umsg.localtype) == MsoyChatChannel.MEMBER_CHANNEL) {
            var member :MemberName = umsg.getSpeakerDisplayName() as MemberName;
            addTab(new ChatTab(_ctx, this, MsoyChatChannel.makeMemberChannel(member), "" + member));
            (_tabs[_tabs.length - 1] as ChatTab).setVisualState(ChatTab.ATTENTION);
        } else if (MsoyChatChannel.typeOf(umsg.localtype) == MsoyChatChannel.JABBER_CHANNEL) {
            var jabberer :JabberName = umsg.getSpeakerDisplayName() as JabberName;
            addTab(new ChatTab(_ctx, this,
                    MsoyChatChannel.makeJabberChannel(jabberer), "" + jabberer));
            (_tabs[_tabs.length - 1] as ChatTab).setVisualState(ChatTab.ATTENTION);
        } else {
            log.info("Dropping unknown user message [msg=" + msg + ", localtype=" +
                      msg.localtype + ", mode=" + umsg.mode + "]");
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _scrollLayer = new Canvas();
        rawChildren.addChild(_scrollLayer);

        _leftScroll = new SCROLL_ARROW() as MovieClip;
        _leftScroll.x = _leftScroll.width / 2;
        _leftScroll.y = _leftScroll.height / 2;
        _leftScroll.visible = false;
        _leftScroll.gotoAndStop(1);
        var wrapper :FlexWrapper = new FlexWrapper(_leftScroll);
        wrapper.setStyle("left", 0);
        wrapper.includeInLayout = false;
        _scrollLayer.addChild(wrapper);
        _leftScroll.addEventListener(MouseEvent.MOUSE_DOWN, genScrollListener(LEFT_SCROLL, true));
        var offListener :Function = genScrollListener(LEFT_SCROLL, false);
        _leftScroll.addEventListener(MouseEvent.MOUSE_UP, offListener);
        _leftScroll.addEventListener(MouseEvent.MOUSE_OUT, offListener);
        _leftScroll.buttonMode = true;

        _rightScroll = new SCROLL_ARROW() as MovieClip;
        _rightScroll.scaleX = -1;
        _rightScroll.x = -_rightScroll.width / 2;
        _rightScroll.y = _rightScroll.height / 2;
        _rightScroll.visible = false;
        _rightScroll.gotoAndStop(1);
        wrapper = new FlexWrapper(_rightScroll);
        wrapper.setStyle("right", 0);
        wrapper.includeInLayout = false;
        _scrollLayer.addChild(wrapper);
        _rightScroll.addEventListener(MouseEvent.MOUSE_DOWN, genScrollListener(RIGHT_SCROLL, true));
        offListener = genScrollListener(RIGHT_SCROLL, false);
        _rightScroll.addEventListener(MouseEvent.MOUSE_UP, offListener);
        _rightScroll.addEventListener(MouseEvent.MOUSE_OUT, offListener);
        _rightScroll.buttonMode = true;

        _scrollRepeater = new Timer(SCROLL_REPEAT_DELAY, 0);
        _scrollRepeater.addEventListener(TimerEvent.TIMER, scrollRepeat);

        // and create the default tab, which must be done after setting up
        callLater(createPlaceTab);
    }

    /**
     * Create the defaultly-named place chat tab, which is up if nothing else is.
     */
    protected function createPlaceTab () :void
    {
        var name :RoomName = createDefaultPlaceName();
        addTab(new ChatTab(_ctx, this, MsoyChatChannel.makeRoomChannel(name), ""+name), 0);
        selectedIndex = 0;
    }

    protected function checkScrollTabs () :void
    {
        // this mostly works. Crap.
        callLater(function () :void {
            var w :int = 0;
            for each (var tab :ChatTab in _tabs) {
                w += tab.width;
            }
            displayScrollTabs(w > width);
        });
    }

    protected function displayScrollTabs (display :Boolean) :void
    {
        callLater(function () :void {
            _leftScroll.visible = display && horizontalScrollPosition > 0;
            _rightScroll.visible =
                display && horizontalScrollPosition < maxHorizontalScrollPosition;
        });
        if (!display) {
            _scrollPercent = 0;
            horizontalScrollPosition = 0;
            _rightScroll.gotoAndStop(1);
            _leftScroll.gotoAndStop(1);
        }
    }

    protected function genScrollListener (direction :int, on :Boolean) :Function
    {
        return function (event :MouseEvent) :void {
            if (on) {
                _scrollDirection = direction;
                scrollRepeat();
                _scrollRepeater.start();
            } else {
                _scrollDirection = 0;
                _scrollPercent = horizontalScrollPosition / maxHorizontalScrollPosition;
                _scrollRepeater.stop();
            }
        };
    }

    protected function scrollRepeat (...ignored) :void
    {
        _scrollPercent = horizontalScrollPosition / maxHorizontalScrollPosition;
        horizontalScrollPosition += _scrollDirection;
        if (_scrollDirection == RIGHT_SCROLL) {
            _rightScroll.gotoAndStop(1);
        } else if (_scrollDirection == LEFT_SCROLL) {
            _leftScroll.gotoAndStop(1);
        }
        _leftScroll.visible = horizontalScrollPosition > 0;
        _rightScroll.visible = horizontalScrollPosition < maxHorizontalScrollPosition;
    }

    protected function addTab (tab :ChatTab, index :int = -1) :void
    {
        tab.addEventListener(ChatTab.TAB_CLICK, handleSelectTab);
        tab.addEventListener(ChatTab.TAB_CLOSE_CLICK, handleRemoveTab);
        if (index == -1) {
            addChild(tab);
            _tabs.push(tab);
        } else {
            for (var ii :int = index; ii < _tabs.length; ii++) {
                (_tabs[ii] as ChatTab).x += tab.width;
            }

            addChildAt(tab, index);
            _tabs.splice(index, 0, tab);
            if (index <= _selectedIndex) {
                _selectedIndex++;
            }
        }

        checkScrollTabs();
    }

    protected function handleSelectTab (event :Event) :void
    {
        var tab :ChatTab = event.target as ChatTab;
        if (tab != null) {
            var ii :int = ArrayUtil.indexOf(_tabs, tab);
            if (ii != -1) {
                selectedIndex = ii;
            }
        }
    }

    protected function handleRemoveTab (event :Event) :void
    {
        var tab :ChatTab = event.target as ChatTab;
        if (tab == null) {
            return;
        }

        // if they are trying to close a GroupTab, we want to wait until we confirm with
        // the server that it will deliver no more
        if (tab.channel.type == MsoyChatChannel.GROUP_CHANNEL) {
            tab.displayCloseBox(false);
            (_ctx.getClient().getService(MsoyService) as MsoyService).setHearingGroupChat(
                (tab.channel.ident as GroupName).getGroupId(), false,
                _ctx.confirmListener(function () :void {
                    removeTab(tab);
                    _ctx.getNotificationDirector().addGenericNotification(
                        MessageBundle.tcompose("m.group_chat_closed", tab.channel.ident),
                        Notification.SYSTEM);
                }));

        } else {
            removeTab(tab);
        }
    }

    protected function removeTab (tab :ChatTab) :void
    {
        var index :int = ArrayUtil.indexOf(_tabs, tab);
        if (index != -1) {
            removeTabAt(index);
        }
    }

    protected function removeTabAt (index :int) :void
    {
        var tab :ChatTab = _tabs[index] as ChatTab;
        for (var ii :int = index; ii < _tabs.length; ii++) {
            (_tabs[ii] as ChatTab).x -= tab.width;
        }
        removeChild(tab);
        _tabs.splice(index, 1);
        if (_selectedIndex == index) {
            // if this was the selected tab, we no longer have a selected tab.
            _selectedIndex = -1;
        } else if (_selectedIndex > index) {
            _selectedIndex--;
        }

        // default back to location chat when a tab is closed
        selectedIndex = 0; // sans underscore, calls the setter

        checkScrollTabs();
    }

    protected function getLocalTypeIndex (localtype :String) :int
    {
        for (var ii :int = 0; ii < _tabs.length; ii++) {
            if ((_tabs[ii] as ChatTab).localtype == localtype) {
                return ii;
            }
        }
        return -1;
    }

    protected function createDefaultPlaceName () :RoomName
    {
        return new RoomName(Msgs.CHAT.get("m.default_place"), 0 /* sceneId 0, "no place" */);
    }

    protected function getChatColor (channelType :int) :uint
    {
        switch (channelType) {
        default:
            return COLOR_ROOM;

        case MsoyChatChannel.GROUP_CHANNEL:
            return COLOR_GROUP;

        case MsoyChatChannel.JABBER_CHANNEL: // fall through to MEMBER_CHANNEL
        case MsoyChatChannel.MEMBER_CHANNEL:
            return COLOR_TELL;
        }
    }

    private static const log :Log = Log.getLog(ChatTabBar);

    [Embed(source="../../../../../../../rsrc/media/skins/tab/tab_scroll_arrow.swf#arrow")]
    protected static const SCROLL_ARROW :Class;

    protected static const LEFT_SCROLL :int = -15;
    protected static const RIGHT_SCROLL :int = 15;
    protected static const SCROLL_REPEAT_DELAY :int = 50;

    protected static const COLOR_ROOM :uint = 0xFFFFFF;
    protected static const COLOR_TELL :uint = 0xFFCE7C;
    protected static const COLOR_GROUP :uint = 0xC7DAEA;

    protected var _tabs :Array = [];
    protected var _selectedIndex :int = -1;
    protected var _ctx :MsoyContext;
    protected var _scrollLayer :Canvas;
    protected var _leftScroll :MovieClip;
    protected var _rightScroll :MovieClip;
    protected var _scrollDirection :int = 0;
    protected var _scrollRepeater :Timer;
    protected var _scrollPercent :Number = 0;
}
}
