// 
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import flash.events.Event;

import mx.events.ItemClickEvent;

import mx.collections.ArrayCollection;

import flexlib.controls.SuperTabBar;
import flexlib.controls.tabBarClasses.SuperTab;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

/**
 * SuperTabBar doesn't leave any way of notifying its creator when a tab is closed, so since we
 * need that information, we have to extend it and do it ourselves.
 */
public class ChatTabBar extends SuperTabBar
{
    public function ChatTabBar (ctx :WorldContext)
    {
        super();
        _ctx = ctx;

        closePolicy = SuperTab.CLOSE_NEVER;
        dataProvider = _tabs;
        dragEnabled = false;
        dropEnabled = false;
        addEventListener(ItemClickEvent.ITEM_CLICK, tabSelected);
    }

    public function getLocationName () :String
    {
        if (_tabs.length == 0) {
            return null;
        }
        return _tabs.getItemAt(0).label as String;
    }

    public function setLocationName (name :String) :void
    {
        var tab :Object = { label: name, controller: null };
        if (_tabs.length == 0) {
            _tabs.addItem(tab);
        } else {
            _tabs.setItemAt(tab, 0);
        }
        selectedIndex = 0;
        tabSelected();
    }

    public function displayChat (channel :ChatChannel, history :HistoryList = null) :void
    {
        var index :int = getControllerIndex(channel);
        if (index != -1) {
            selectedIndex = index;
            tabSelected();
            return;
        }

        // this tab hasn't been created yet.
        if (history == null) {
            Log.getLog(this).warning(
                "Cannot display chat for an unknown channel without a history [" + channel + "]");
            return;
        }
        createAndSelectChatTab(channel, history);
    }

    public function displayMessage (channel :ChatChannel, msg :ChatMessage, 
        history :HistoryList) :void
    {
        var controller :ChatChannelController = getController(channel);
        if (controller == null) {
            // if this is a message from a member, we can pop up the new display.
            if (channel.type == ChatChannel.MEMBER_CHANNEL) {
                createAndSelectChatTab(channel, history);
            } else {
                // else this arrived (most likely) after we already closed the channel tab.
                Log.getLog(this).info(
                    "Dropping late arriving channel chat message [msg=" + msg + "].");
            }
            return;
        }

        controller.displayMessage(msg);
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

    /** 
     * Thanks a lot flex team - you've got at least two classes in this hierarchy with
     * _selectedIndex variables that mean exactly the same damn thing, and they're both private...
     * so I have to make a third one.
     */
    override public function set selectedIndex (ii :int) :void
    {
        super.selectedIndex = ii;
        _selectedIndex = ii;
    }

    // yay for this not actually being private like they labeled it in the docs.
    override public function onCloseTabClicked (event :Event) :void
    {
        var index :int = getChildIndex(event.currentTarget as DisplayObject);
        var controller :ChatChannelController = 
            _tabs.getItemAt(index).controller as ChatChannelController;

        super.onCloseTabClicked(event);

        controller.shutdown();
        (_ctx.getChatDirector() as MsoyChatDirector).closeChannel(controller.getChannel());

        // default back to room chat when a tab is closed
        selectedIndex = 0;
        tabSelected();
    }

    protected function createAndSelectChatTab (channel :ChatChannel, history :HistoryList) :void 
    {
        var controller :ChatChannelController = new ChatChannelController(_ctx, channel, history);
        _tabs.addItem({ label: channel.ident, controller: controller });
        selectedIndex = _tabs.length - 1;
        tabSelected();
    }

    protected function getController (channel :ChatChannel) :ChatChannelController
    {
        var index :int = getControllerIndex(channel);
        if (index != -1) {
            return _tabs.getItemAt(index).controller as ChatChannelController;
        }
        return null;
    }

    protected function getControllerIndex (channel :ChatChannel) :int
    {
        for (var ii :int = 0; ii < _tabs.length; ii++) {
            var controller :ChatChannelController = 
                _tabs.getItemAt(ii).controller as ChatChannelController;
            if (controller != null && controller.getChannel().equals(channel)) {
                return ii;
            }
        }
        return -1;
    }

    protected function tabSelected (event :ItemClickEvent = null) :void
    {
        var index :int = event == null ? _selectedIndex : event.index;
        // this is a stupid hack, but it seems to be the only way to get "Super"TabNav to actually
        // do what's its supposed to and allow some tabs to be closeable and others not.
        closePolicy = index == 0 ? SuperTab.CLOSE_NEVER : SuperTab.CLOSE_SELECTED;

        // if our index is -1, we've just hidden the history, so no change to the display.
        if (index != -1) {
            var controller :ChatChannelController = 
                _tabs.getItemAt(index).controller as ChatChannelController;
            if (controller != null) {
                controller.displayChat();
            } else {
                var overlay :ChatOverlay = _ctx.getTopPanel().getChatOverlay();
                if (overlay != null) {
                    overlay.setHistory(
                        (_ctx.getChatDirector() as MsoyChatDirector).getRoomHistory());
                }
            }
        }
    }

    protected var _tabs :ArrayCollection = new ArrayCollection;

    // The value returned from get selectedIndex() does not always reflect the value that was 
    // just immeadiately set via set selectedIndex(), so lets keep track of what we really want.
    protected var _selectedIndex :int = -1;

    protected var _ctx :WorldContext;
}
}
