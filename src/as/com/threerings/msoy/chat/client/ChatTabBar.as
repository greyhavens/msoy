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

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.chat.data.MsoyChatChannel;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
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
        var overlay :ChatOverlay = _ctx.getTopPanel().getChatOverlay();
        if (overlay != null) {
            overlay.setLocalType(activeTab.localtype);
        }
    }

    public function get locationName () :String
    {
        if (_tabs.length == 0) {
            return null;
        }
        return (_tabs[0] as ChatTab).text;
    }

    /**
     * Will make sure the first tab in the list is a non-channel based tab (currently just games),
     * and titles it with the given name.
     */
    public function set locationName (name :String) :void
    {
        // if this function is called with name == null, a separate call will shuffle the
        // appropriate tab to the front, and we should make sure that if the first tab was a room
        // tab, it is cleared out.
        if (name == null && _tabs.length != 0) {
            for (var ii :int = 0; ii < _tabs.length; ii++) {
                if ((_tabs[ii] as ChatTab).localtype == ChatCodes.PLACE_CHAT_TYPE) {
                    removeTabAt(ii);
                    break;
                }
            }
        } else if (name != null) {
            if (_tabs.length == 0 ||
                ((_tabs[0] as ChatTab).localtype != ChatCodes.PLACE_CHAT_TYPE)) {
                addTab(new ChatTab(_ctx, this, null, name), 0);
            } else {
                (_tabs[0] as ChatTab).text = name;
            }
            selectedIndex = 0;
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
            var newEntry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            var newNameStr :String = newEntry.name.toString();
            if (newNameStr != oldEntry.name.toString()) {
                for each (var tab :ChatTab in _tabs) {
                    if (tab.getTellMemberId() == newEntry.name.getMemberId()) {
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

    public function openChannelTab (channel :MsoyChatChannel, andSelect :Boolean) :void
    {
        var index :int = getLocalTypeIndex(channel.toLocalType());
        if (index != -1) {
            if (andSelect) {
                selectedIndex = index;
            }
            return;
        }

        // this tab hasn't been created yet.
        addTab(new ChatTab(_ctx, this, channel, ""+channel.ident));
        if (andSelect) {
            selectedIndex = _tabs.length - 1;
        }
    }

    public function containsTab (channel :MsoyChatChannel) :Boolean
    {
        return getLocalTypeIndex(channel.toLocalType()) != -1;
    }

    /**
     * Called by the chat director to let us know when we enter a room.
     */
    public function locationDidChange (name :RoomName) :void
    {
        if (name == null) {
            // we don't need to do anything if we're leaving
            return;
        }

        var roomTabIx :int = getLocalTypeIndex(ChatCodes.PLACE_CHAT_TYPE);
        if (roomTabIx >= 0) {
            // there's already a room tab up, just modify it
            _tabs[roomTabIx].text = name.toString();
        } else {
            // else create our room chat tab, in the leftmost position
            addTab(new ChatTab(_ctx, this, MsoyChatChannel.makeRoomChannel(name), ""+name), 0);
            selectedIndex = 0;
        }
    }

    public function getCurrentChannel () :MsoyChatChannel
    {
        return _tabs.length > 0 ? (_tabs[_selectedIndex] as ChatTab).channel : null;
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

    // from ChatDisplay - always returns false, as this ChatDisplay does no actual message display
    public function displayMessage (msg :ChatMessage, alreadyDisplayed :Boolean) :Boolean
    {
        if (_tabs.length == 0) {
            // if we receive any messages before we have any tabs, there's nothing to do here.
            return false;
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
            return false;
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

        return false;
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
        tab.addEventListener(ChatTab.TAB_CLICK, selectTab);
        tab.addEventListener(ChatTab.TAB_CLOSE_CLICK, removeTab);
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

    protected function selectTab (event :Event) :void
    {
        var tab :ChatTab = event.target as ChatTab;
        if (tab == null) {
            return;
        }

        var ii :int = ArrayUtil.indexOf(_tabs, tab);
        if (ii >= 0) {
            selectedIndex = ii;
        }
    }

    protected function removeTab (event :Event) :void
    {
        var tab :ChatTab = event.target as ChatTab;
        if (tab == null) {
            return;
        }

        var index :int = ArrayUtil.indexOf(_tabs, event.target);
        if (index < 0) {
            return;
        }

        removeTabAt(index);
    }

    protected function removeTabAt (index :int, shutdown :Boolean = true) :void
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
        selectedIndex = 0;

        if (shutdown) {
            _ctx.getMsoyChatDirector().tabClosed(tab.localtype);
        }

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

    private static const log :Log = Log.getLog(ChatTabBar);

    [Embed(source="../../../../../../../rsrc/media/skins/tab/tab_scroll_arrow.swf#arrow")]
    protected static const SCROLL_ARROW :Class;

    protected static const LEFT_SCROLL :int = -15;
    protected static const RIGHT_SCROLL :int = 15;
    protected static const SCROLL_REPEAT_DELAY :int = 50;

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
