// // $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.HBox;

import com.threerings.util.ArrayUtil;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.client.MsoyChatDirector;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

import com.threerings.msoy.game.client.GameChatDirector;

public class ChatTabBar extends HBox
{
    public function ChatTabBar (ctx :WorldContext)
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
            activeTab.displayCloseBox(false);
            activeTab.styleName = "unselectedChatTab";
        }
        
        _selectedIndex = (ii + _tabs.length) % _tabs.length;
        activeTab = _tabs[_selectedIndex] as ChatTab;
        activeTab.styleName = "selectedChatTab";
        activeTab.displayCloseBox(_selectedIndex != 0);
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
        if (_tabs.length == 0) {
            addAndSelect(new ChatTab(_ctx, this, null, null, name));
        } else {
            (_tabs[0] as ChatTab).text = name;
        }
        selectedIndex = 0;
    }

    public function setChatDirector (dir :ChatDirector) :void
    {
        _chatDirector = dir;
    }

    public function displayChat (channel :ChatChannel, history :HistoryList = null) :void
    {
        var index :int = getControllerIndex(channel);
        if (index != -1) {
            selectedIndex = index;
            return;
        }

        // this tab hasn't been created yet.
        if (history == null) {
            Log.getLog(this).warning(
                "Cannot display chat for an unknown channel without a history [" + channel + "]");
            return;
        }
        addAndSelect(new ChatTab(_ctx, this, channel, history));
    }

    public function addMessage (channel :ChatChannel, msg :ChatMessage) :void
    {
        var controller :ChatChannelController = getController(channel);
        if (controller != null) {
            controller.displayMessage(msg);
            return;
        }

        // if this is a message from a member, we can pop up the new display.
        if (channel.type == ChatChannel.MEMBER_CHANNEL) {
            var history :HistoryList = _ctx.getMsoyChatDirector().getHistory(channel);
            addAndSelect(new ChatTab(_ctx, this, channel, history));
        } else {
            // else this arrived (most likely) after we already closed the channel tab.
            Log.getLog(this).info(
                "Dropping late arriving channel chat message [msg=" + msg + "].");
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
        if (_chatDirector == null) {
            return _ctx.getMsoyChatDirector().getRoomHistory();
        } else if (_chatDirector is MsoyChatDirector) {
            return (_chatDirector as MsoyChatDirector).getRoomHistory();
        } else if (_chatDirector is GameChatDirector) {
            return (_chatDirector as GameChatDirector).getGameHistory();
        }
        return null;
    }

    protected function addAndSelect (tab :ChatTab) :void
    {
        addChild(tab);
        _tabs.push(tab);

        tab.addEventListener(MouseEvent.CLICK, selectTab);
        selectedIndex = _tabs.length - 1;
    }

    protected function selectTab (event :MouseEvent) :void
    {
        var tab :ChatTab = findChatTab(event.target as DisplayObject);
        if (tab == null) {
            Log.getLog(this).debug("wtf @ not chat tab >< [" + event.target + "]");
            return;
        }

        var ii :int = ArrayUtil.indexOf(_tabs, tab);
        if (ii >= 0) {
            selectedIndex = ii;
        }
    }

    protected function removeTab (event :MouseEvent) :void
    {
        var tab :ChatTab = findChatTab(event.target as DisplayObject);
        if (tab == null) {
            return;
        }
        tab.controller.shutdown();
        _ctx.getMsoyChatDirector().closeChannel(tab.controller.getChannel());

        var index :int = ArrayUtil.indexOf(_tabs, event.target);
        if (index < 0) {
            return;
        }
        for (var ii :int = index; ii < _tabs.length; ii++) {
            (_tabs[ii] as ChatTab).x -= tab.width;
        }
        removeChild(tab);
        _tabs.splice(index, 1);

        // default back to location chat when a tab is closed
        selectedIndex = 0;
    }

    protected function findChatTab (dispObj :DisplayObject) :ChatTab
    {
        // head up the display list 3 time at most
        for (var ii :int = 0; ii < 3; ii++) {
            if (dispObj is ChatTab) {
                return dispObj as ChatTab;
            } else if (dispObj == null) {
                return null;
            }
            dispObj = dispObj.parent;
        }
        return null;
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
            if (controller != null && controller.getChannel().equals(channel)) {
                return ii;
            }
        }
        return -1;
    }

    protected var _tabs :Array = [];
    protected var _selectedIndex :int = -1;
    protected var _ctx :WorldContext;
    protected var _chatDirector :ChatDirector;
}
}
