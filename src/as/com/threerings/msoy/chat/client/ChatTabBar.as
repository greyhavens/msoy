//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import flash.events.Event;

import mx.containers.HBox;

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
                (_tabs[index] as ChatTab).setVisualState(ChatTab.ATTENTION);
            }
            return;
        } 

        if (channel == null) {
            getLocationHistory().addMessage(msg);
            locationReceivedMessage();
            return;
        }

        // if this is a message from a member, we can pop up a new tab, and set it to ATTENTION
        if (channel.type == ChatChannel.MEMBER_CHANNEL) {
            var history :HistoryList = _ctx.getMsoyChatDirector().getHistory(channel);
            history.addMessage(msg);
            addTab(new ChatTab(_ctx, this, channel, history));
            (_tabs[_tabs.length - 1] as ChatTab).setVisualState(ChatTab.ATTENTION);
        } else {
            // else this arrived (most likely) after we already closed the channel tab.
            log.info("Dropping late arriving channel chat message [msg=" + msg + "].");
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
            controller.reinit(ccobj);
        }
    }

    public function containsTab (channel :ChatChannel) :Boolean
    {
        return getControllerIndex(channel) != -1;
    }

    public function getCurrentController () :ChatChannelController
    {
        return (_tabs[_selectedIndex] as ChatTab).controller;
    }

    public function getLocationHistory () :HistoryList
    {
        if (_chatDirector is GameChatDirector) {
            return (_chatDirector as GameChatDirector).getGameHistory();
        }

        log.debug("asked for location history in a non-game");
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
        tab.controller.shutdown();
        _ctx.getMsoyChatDirector().closeChannel(tab.controller.channel);

        var index :int = ArrayUtil.indexOf(_tabs, event.target);
        if (index < 0) {
            return;
        }

        removeTabAt(index);
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
        }

        // default back to location chat when a tab is closed
        selectedIndex = 0;
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

    protected var _tabs :Array = [];
    protected var _selectedIndex :int = -1;
    protected var _ctx :MsoyContext;
    protected var _chatDirector :ChatDirector;
}
}
