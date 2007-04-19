//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.mx_internal;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.events.ChildExistenceChangedEvent;
import mx.events.FlexEvent;
import mx.utils.StringUtil;

import flexlib.containers.SuperTabNavigator;
import flexlib.controls.tabBarClasses.SuperTab;

import com.threerings.flex.CommandButton;
import com.threerings.util.HashMap;
import com.threerings.util.Name;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.msoy.chat.client.ChatChannel;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;

/**
 * Displays our various chat channels in tabs.
 */
public class ChatChannelPanel extends VBox
{
    public function ChatChannelPanel (ctx :WorldContext)
    {
        _ctx = ctx;
        width = 200;

        addChild(_tabnav = new SuperTabNavigator());
        _tabnav.closePolicy = SuperTab.CLOSE_SELECTED;
        _tabnav.percentWidth = 100;
        _tabnav.percentHeight = 100;
        _tabnav.addEventListener(ChildExistenceChangedEvent.CHILD_REMOVE, tabRemoved);

// if this simply worked, I could perhaps have spent the last three hours of my life working on
// something that made people happy; but it doesn't (nor do a dozen other attempts to accomplish
// the same goal); I still don't know how we're going to get the TabBar to draw a black background
//         _tabnav.mx_internal::getTabBar().setStyle("backgroundColor", "#000000");

        // create a UI for sending chat which we'll show when we're active
        _inputBox = new HBox();
        _inputBox.styleName = "chatControl";
        _inputBox.addChild(_input = new ChatInput());
        _input.width = 100;
        _input.height = ControlBar.HEIGHT-4;
        _input.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
        var send :CommandButton = new CommandButton();
        send.label = Msgs.GENERAL.get("b.send");
        send.setFunction(sendChat);
        send.height = ControlBar.HEIGHT-4;
        _inputBox.addChild(send);

        _ctx.getClient().addEventListener(WorldClient.MINIZATION_CHANGED, minimizationChanged);
    }

    /**
     * Called when the client is minimized.
     */
    public function setMinimized (minimized :Boolean) :void
    {
    }

    /**
     * Returns the chat display to use for the specified channel.
     */
    public function getChatDisplay (
        channel :ChatChannel, history :HistoryList, select :Boolean) :ChatDisplay
    {
        var tabidx :int = -1;
        var tab :ChatTab = null;
        for (var ii :int = 0; ii < _tabnav.numChildren; ii++) {
            var ctab :ChatTab = (_tabnav.getChildAt(ii) as ChatTab);
            if (ctab.channel.equals(channel)) {
                tab = ctab;
                tabidx = ii;
                break;
            }
        }

        // create a new tab if we did not find one already in use
        if (tab == null) {
            tab = new ChatTab(_ctx, channel, this);
            tab.label = Msgs.GENERAL.xlate(channel.getName());
            tab.getOverlay().setHistory(history);
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
    }

    protected function minimizationChanged (event :ValueEvent) :void
    {
        var minimized :Boolean = (event.value as Boolean);
        // TODO: hijack the PlaceBox, stuff it into a tab, more bits
    }

    /**
     * Called when the user presses enter in the chat input field or clicks the "Send" button.
     */
    protected function sendChat (... ignored) :void
    {
        var tab :ChatTab = (_tabnav.getChildAt(_tabnav.selectedIndex) as ChatTab);
        if (tab == null) {
            // wtf?
            return;
        }

        var message :String = StringUtil.trim(_input.text);
        if ("" == message) {
            return;
        }

        // TODO: request listener
        _ctx.getChatDirector().requestTell(tab.channel.ident as Name, message, null);
        _input.text = "";
    }

    protected var _ctx :WorldContext;
    protected var _tabnav :SuperTabNavigator;
    protected var _inputBox :HBox;
    protected var _input :ChatInput;
}
}

import flash.events.Event;
import mx.core.Container;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.client.ChatChannel;
import com.threerings.msoy.chat.client.ChatChannelPanel;
import com.threerings.msoy.chat.client.ChatOverlay;

/**
 * Displays a single chat tab.
 */
class ChatTab extends Container
{
    public var channel :ChatChannel;

    public function ChatTab (ctx :WorldContext, channel :ChatChannel, host :ChatChannelPanel)
    {
        styleName = "channelChatTab";
        this.channel = channel;
        _host = host;
        _overlay = new ChatOverlay(ctx);
        _overlay.setClickableGlyphs(true);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    public function getOverlay () :ChatOverlay
    {
        return _overlay;
    }

    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            _overlay.setTarget(this);
        } else {
            _overlay.setTarget(null);
        }
    }

    /** Our tab-managing host. */
    protected var _host :ChatChannelPanel;

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;
}
