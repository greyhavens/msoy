//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;
import flash.display.MovieClip;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.geom.Rectangle;

import flash.utils.Timer;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.core.ScrollPolicy;

import com.threerings.flex.FlexWrapper;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Log;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.HeaderBar;

import com.threerings.msoy.chat.client.MsoyChatDirector;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

import com.threerings.msoy.game.client.GameChatDirector;

public class ChatTabBar extends HBox
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
        displayScrollTabs(w < getExplicitOrMeasuredWidth());
        horizontalScrollPosition = _scrollPercent * maxHorizontalScrollPosition;
    }

    override public function addChildAt (dispObj :DisplayObject, index :int) :DisplayObject
    {
        var retVal :DisplayObject = super.addChildAt(dispObj, index);
        return retVal;
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
        activeTab.displayChat();
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
                if ((_tabs[ii] as ChatTab).controller == null) {
                    removeTabAt(ii);
                    break;
                }
            }
        } else if (name != null) {
            if (_tabs.length == 0 || ((_tabs[0] as ChatTab).controller != null)) {
                addTab(new ChatTab(_ctx, this, null, null, name), 0);
            } else {
                (_tabs[0] as ChatTab).text = name;
            }
            selectedIndex = 0;
        }
    }

    public function setChatDirector (dir :ChatDirector) :void
    {
        _chatDirector = dir;
    }

    public function displayChat (channel :ChatChannel, history :HistoryList = null,
        inFront :Boolean = false) :void
    {
        var index :int = getControllerIndex(channel);
        if (index != -1) {
            if (inFront) {
                moveTabToFront(channel);
                selectedIndex = 0;
            } else {
                selectedIndex = index;
            }
            return;
        }

        // this tab hasn't been created yet.
        if (history == null) {
            log.warning("Cannot display chat for an unknown channel without a history [" +
                channel + "]");
            return;
        }
        if (inFront) {
            addTab(new ChatTab(_ctx, this, channel, history), 0);
            selectedIndex = 0;
        } else {
            addAndSelect(new ChatTab(_ctx, this, channel, history));
        }
    }

    public function addMessage (channel :ChatChannel, msg :ChatMessage) :void
    {
        var controller :ChatChannelController;
        if (channel == null) {
            controller = getCurrentController();
        } else {
            controller = getController(channel);
        }
        if (controller != null) {
            controller.addMessage(msg);
            var index :int = getControllerIndex(channel);
            if (index != _selectedIndex && channel != null) {
                var tab :ChatTab = _tabs[index] as ChatTab;
                tab.setVisualState(ChatTab.ATTENTION);
                if (_rightScroll.visible && tab.x > (horizontalScrollPosition + width)) {
                    _rightScroll.play();
                } else if (_leftScroll.visible && (tab.x + tab.width) < horizontalScrollPosition) {
                    _leftScroll.play();
                }
            }
            return;
        }

        if (channel == null) {
            if (getLocationHistory() != null) {
                getLocationHistory().addMessage(msg);
            } else {
                log.warning("Dropping " + msg);
            }
            locationReceivedMessage();
            return;
        }

        // if this is a message from a member, we can pop up a new tab, and set it to ATTENTION
        if (channel.type == ChatChannel.MEMBER_CHANNEL ||
                channel.type == ChatChannel.JABBER_CHANNEL) {
            var history :HistoryList = _ctx.getMsoyChatDirector().getHistory(channel);
            history.addMessage(msg);
            addTab(new ChatTab(_ctx, this, channel, history));
            (_tabs[_tabs.length - 1] as ChatTab).setVisualState(ChatTab.ATTENTION);
        } else {
            // else this arrived (most likely) after we already closed the channel tab.
            log.info("Dropping late arriving channel chat message [msg=" + msg + ", localtype=" + 
                      msg.localtype + "].");
        }
    }

    public function locationReceivedMessage () :void
    {
        if (_selectedIndex != 0 && _tabs.length > 0) {
            (_tabs[0] as ChatTab).setVisualState(ChatTab.ATTENTION);
        }
    }

    public function reinitController (channel :ChatChannel, ccobj :ChatChannelObject) :void
    {
        var controller :ChatChannelController = getController(channel);
        if (controller != null) {
            controller.init(ccobj);
        }
    }

    public function containsTab (channel :ChatChannel) :Boolean
    {
        return getControllerIndex(channel) != -1;
    }

    public function getCurrentController () :ChatChannelController
    {
        return _tabs.length > 0 ? (_tabs[_selectedIndex] as ChatTab).controller : null;
    }

    public function getLocationHistory () :HistoryList
    {
        if (_chatDirector is GameChatDirector) {
            return (_chatDirector as GameChatDirector).getGameHistory();
        }

        log.warning("asked for location history in a non-game");
        // Log.dumpStack();
        return null;
    }

    public function moveTabToFront (channel :ChatChannel) :void
    {
        var index :int = getControllerIndex(channel);
        if (index == -1) {
            log.debug("asked to move unknown tab to front [" + channel + "]");
            return;
        }

        var selected :Boolean = _selectedIndex == index;
        var tab :ChatTab = _tabs[index];
        removeTabAt(index);
        addTab(tab, 0);
        if (selected) {
            selectedIndex = 0;
        }
    }

    public function displayActiveChat (defaultList :HistoryList) :void
    {
        if (_selectedIndex < 0 || _tabs.length <= 0) {
            _ctx.getTopPanel().getChatOverlay().setHistory(defaultList);
            return;
        }

        (_tabs[_selectedIndex] as ChatTab).displayChat();
    }

    public function chatTabIndex (tab :ChatTab) :int
    {
        return _tabs.indexOf(tab);
    }

    public function tabChecked (channel :ChatChannel) :Boolean
    {
        var index :int = getControllerIndex(channel);
        if (index == -1) {
            log.debug("asked for checked on a tab we don't appear to have [" + channel + "]");
            return false;
        }

        return (_tabs[index] as ChatTab).checked;
    }

    public function shouldReconnectChannel (channel :ChatChannel) :Boolean
    {
        // if this is anything but a room channel, it should be reconnected.
        if (channel.type != ChatChannel.ROOM_CHANNEL) {
            return true;
        }

        // if we don't have a tab for this channel, assume the caller knows what its doing and say
        // yes
        var index :int = getControllerIndex(channel);
        if (index == -1) {
            return true;
        }

        // if this room channel does not have its check box checked, it should not be reconnected
        return (_tabs[index] as ChatTab).checked;
    }

    public function clearUncheckedRooms () :void
    {
        for (var ii :int = 0; ii < _tabs.length; ii++) {
            var tab :ChatTab = _tabs[ii] as ChatTab;
            if (tab.controller != null && tab.controller.channel.type == ChatChannel.ROOM_CHANNEL &&
                !tab.checked) {
                removeTabAt(ii, true);
            }
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

        // init the controller with its previously set channel
        if (tab.controller != null) {
            var channel :ChatChannel = tab.controller.channel;
            tab.controller.init(_ctx.getMsoyChatDirector().getChannelObject(channel));
        }
    }

    protected function addAndSelect (tab :ChatTab) :void
    {
        addTab(tab);
        selectedIndex = _tabs.length - 1;
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

        removeTabAt(index, true);
    }

    protected function removeTabAt (index :int, shutdown :Boolean = false) :void
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
            tab.controller.shutdown();
            _ctx.getMsoyChatDirector().closeChannel(tab.controller.channel);
        }
    }

    protected function getController (channel :ChatChannel) :ChatChannelController
    {
        var index :int = getControllerIndex(channel);
        if (index != -1) {
            return (_tabs[index] as ChatTab).controller;
        }
        return null;
    }

    protected function getControllerIndex (channel :ChatChannel) :int
    {
        for (var ii :int = 0; ii < _tabs.length; ii++) {
            var controller :ChatChannelController = (_tabs[ii] as ChatTab).controller;
            if (controller != null && controller.channel.equals(channel)) {
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
    protected var _chatDirector :ChatDirector;
    protected var _scrollLayer :Canvas;
    protected var _leftScroll :MovieClip;
    protected var _rightScroll :MovieClip;
    protected var _scrollDirection :int = 0;
    protected var _scrollRepeater :Timer;
    protected var _scrollPercent :Number = 0;
}
}
