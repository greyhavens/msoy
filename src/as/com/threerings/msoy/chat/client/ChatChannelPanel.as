//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.mx_internal;

import mx.containers.HBox;
import mx.containers.TabNavigator;
import mx.containers.VBox;

import mx.events.ChildExistenceChangedEvent;
import mx.events.FlexEvent;
import mx.utils.StringUtil;

import flexlib.containers.SuperTabNavigator;
import flexlib.controls.tabBarClasses.SuperTab;

import com.threerings.flex.CommandButton;
import com.threerings.util.HashMap;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.data.ChatChannel;

/**
 * Displays our various chat channels in tabs.
 */
public class ChatChannelPanel extends VBox
{
    public function ChatChannelPanel (ctx :WorldContext)
    {
        _ctx = ctx;
        width = TopPanel.RIGHT_SIDEBAR_WIDTH;

        addChild(_tabnav = new ChatTabNavigator());
        _tabnav.closePolicy = SuperTab.CLOSE_SELECTED; // can't do this in the constructor, yay!
        _tabnav.percentWidth = 100;
        _tabnav.percentHeight = 100;
        _tabnav.addEventListener(ChildExistenceChangedEvent.CHILD_REMOVE, tabRemoved);

        // create a UI for sending chat which we'll show when we're active
        _inputBox = new HBox();
        _inputBox.styleName = "chatControl";
        _inputBox.addChild(_input = new ChatInput());
        _input.height = ControlBar.HEIGHT-4;
        _input.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
        var send :CommandButton = new CommandButton();
        send.label = Msgs.CHAT.get("b.send");
        send.setFunction(sendChat);
        send.height = ControlBar.HEIGHT-4;
        _inputBox.addChild(send);

        _ctx.getClient().addEventListener(WorldClient.MINI_WILL_CHANGE, miniWillChange);
    }

    /**
     * Locates the specified chat display, returning null if it is not open.
     */
    public function findChatDisplay (channel :ChatChannel) :ChatDisplay
    {
        for (var ii :int = 0; ii < _tabnav.numChildren; ii++) {
            var ctab :ChatTab = (_tabnav.getChildAt(ii) as ChatTab);
            if (ctab is ChannelChatTab && (ctab as ChannelChatTab).channel.equals(channel)) {
                return (ctab as ChannelChatTab).getOverlay();
            }
        }
        return null;
    }

    /**
     * Returns the chat display to use for the specified channel.
     */
    public function getChatDisplay (
        channel :ChatChannel, history :HistoryList, select :Boolean) :ChatDisplay
    {
        var tabidx :int = -1;
        var tab :ChannelChatTab = null;
        for (var ii :int = 0; ii < _tabnav.numChildren; ii++) {
            var ctab :ChatTab = (_tabnav.getChildAt(ii) as ChatTab);
            if (ctab is ChannelChatTab && (ctab as ChannelChatTab).channel.equals(channel)) {
                tab = (ctab as ChannelChatTab);
                tabidx = ii;
                break;
            }
        }

        // create a new tab if we did not find one already in use
        if (tab == null) {
            tab = new ChannelChatTab(_ctx, channel);
            tab.label = channel.ident.toString();
            tab.getOverlay().setHistory(history);
            tab.init((_ctx.getChatDirector() as MsoyChatDirector).getChannelObject(channel));
            tabidx = _tabnav.numChildren;
            _tabnav.addChild(tab);
        }

        // select this tab if requested
        if (select) {
            _tabnav.selectedIndex = tabidx;
        }

        // if we're not visible, add ourselves
        if (parent == null) {
            _ctx.getTopPanel().setRightPanel(this);
            _ctx.getTopPanel().getControlBar().setChannelChatInput(_inputBox);
        }

        // if we're selecting the tab in question, focus the chat input as well
        if (select) {
            callLater(function () :void {
                _input.setFocus();
            });
        }

        return tab.getOverlay();
    }

    protected function tabRemoved (event :ChildExistenceChangedEvent) :void
    {
        if (event.relatedObject is ChatTab) {
            if (_tabnav.numChildren == 1) {
                _ctx.getTopPanel().clearRightPanel(this);
                _ctx.getTopPanel().getControlBar().setChannelChatInput(null);
            }
        }
        if (event.relatedObject is ChannelChatTab) {
            (event.relatedObject as ChannelChatTab).shutdown();
            var channel :ChatChannel = (event.relatedObject as ChannelChatTab).channel;
            (_ctx.getChatDirector() as MsoyChatDirector).closeChannel(channel);
        }
    }

    protected function miniWillChange (event :ValueEvent) :void
    {
        var minimized :Boolean = (event.value as Boolean);
        if (minimized && _wtab == null) {
            var select :Boolean = (_tabnav.numChildren == 0);
            _wtab = new WorldChatTab(_ctx);
            _wtab.label = Msgs.CHAT.xlate("l.world_channel");
            _tabnav.addChildAt(_wtab, 0);
            _tabnav.setClosePolicyForTab(0, SuperTab.CLOSE_NEVER);

            // select this tab if none are selected
            if (select) {
                _tabnav.selectedIndex = 0;
            }

            // if we're not visible, add ourselves
            if (parent == null) {
                _ctx.getTopPanel().setRightPanel(this);
                _ctx.getTopPanel().getControlBar().setChannelChatInput(_inputBox);
            }

        } else if (!minimized && _wtab != null) {
            _tabnav.removeChild(_wtab);
            _wtab.shutdown();
            _wtab = null;
        }
    }

    /**
     * Called when the user presses enter in the chat input field or clicks the "Send" button.
     */
    protected function sendChat (... ignored) :void
    {
        var message :String = StringUtil.trim(_input.text);
        if ("" == message) {
            return;
        }

        var tab :ChatTab = (_tabnav.getChildAt(_tabnav.selectedIndex) as ChatTab);
        if (tab != null) {
            tab.sendChat(message);
            _input.text = "";
        } else {
            Log.getLog(this).warning("Missing selected chat tab?! Dropping '" + message + "'.");
        }
    }

    protected var _ctx :WorldContext;
    protected var _tabnav :SuperTabNavigator;
    protected var _wtab :WorldChatTab;
    protected var _inputBox :HBox;
    protected var _input :ChatInput;
}
}
