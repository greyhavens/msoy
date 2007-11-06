// 
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;

import mx.events.ItemClickEvent;

import mx.collections.ArrayCollection;

import flexlib.controls.SuperTabBar;
import flexlib.controls.tabBarClasses.SuperTab;

import com.threerings.util.ConfigValueSetEvent;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.client.MsoyChatDirector;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

import com.threerings.msoy.game.client.GameChatDirector;

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

        // listen for preferences changes, update history mode
        Prefs.config.addEventListener(ConfigValueSetEvent.TYPE, handlePrefsUpdated, false, 0, true);

        // hackery to accept a click on the tabs, even when we do our no-tab-is-selected business
        addEventListener(MouseEvent.MOUSE_DOWN, onMouseDown);
    }

    /**
     * Set the active tab one to the right, or wrap around to the first tab.
     */
    public function nextTab () :void
    {
        selectedIndex = (_selectedIndex + 1) % _tabs.length;
    }

    /**
     * Set the active tab one to the left, or wrap around to the last tab.
     */
    public function prevTab () :void
    {
        selectedIndex = _selectedIndex <= 0 ? _tabs.length - 1 : _selectedIndex - 1;
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
        createAndSelectChatTab(channel, history);
    }

    public function displayMessage (channel :ChatChannel, msg :ChatMessage, 
        history :HistoryList) :void
    {
        var controller :ChatChannelController = getController(channel);
        if (controller != null) {
            controller.displayMessage(msg);
            return;
        }

        // if this is a message from a member, we can pop up the new display.
        if (channel.type == ChatChannel.MEMBER_CHANNEL) {
            createAndSelectChatTab(channel, history);
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
        return _currentController;
    }

    /** 
     * Thanks a lot flex team - you've got at least two classes in this hierarchy with
     * _selectedIndex variables that mean exactly the same damn thing, and they're both private...
     * so I have to make a third one.
     */
    override public function set selectedIndex (ii :int) :void
    {
        super.selectedIndex = ii;
        if (_selectedIndex != ii) {
            _selectedIndex = ii;
            // when a tab is actually clicked on, one of our parent's _selectedIndex's is changed 
            // in the background, without calling this setter, so this won't actually make 
            // tabSelected get called twice.
            tabSelected();
        }
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
    }

    protected function onMouseDown (event :MouseEvent) :void
    {
        // any click on the tab bar should ensure that the chat history is showing
        if (!Prefs.getShowingChatHistory()) {
            Prefs.setShowingChatHistory(true);
        }
    }

    protected function handlePrefsUpdated (event :ConfigValueSetEvent) :void
    {
        switch (event.name) {
        case Prefs.CHAT_HISTORY:
            if (Boolean(event.value)) {
                if (_unhideIndex >= 0 && _unhideIndex < numChildren) {
                    if (_selectedIndex == -1) {
                        selectedIndex = _unhideIndex;
                    }
                    var tab :SuperTab = getChildAt(_unhideIndex) as SuperTab;
                    tab.styleName = "msoyTabButton";
                    _unhideIndex = -1;
                }
                setStyle("selectedTabTextStyleName", "selectedMsoyTabButton");
            } else {
                _unhideIndex = _selectedIndex;
                // hackery to get the tab to look like it was unselected
                selectedIndex = -1;
                setStyle("selectedTabTextStyleName", "");
                tab = getChildAt(_unhideIndex) as SuperTab;
                if (tab != null) {
                    tab.styleName = "msoyForcedUpTab";
                }
            }
        }
    }

    protected function createAndSelectChatTab (channel :ChatChannel, history :HistoryList) :void 
    {
        var controller :ChatChannelController = new ChatChannelController(_ctx, channel, history);
        _tabs.addItem({ label: channel.ident, controller: controller });
        selectedIndex = _tabs.length - 1;
        controller.init((_ctx.getChatDirector() as MsoyChatDirector).getChannelObject(channel));
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

    protected function getDirectorHistory () :HistoryList
    {
        if (_chatDirector == null) {
            return (_ctx.getChatDirector() as MsoyChatDirector).getRoomHistory();
        } else if (_chatDirector is MsoyChatDirector) {
            return (_chatDirector as MsoyChatDirector).getRoomHistory();
        } else if (_chatDirector is GameChatDirector) {
            return (_chatDirector as GameChatDirector).getGameHistory();
        }
        return null;
    }

    protected function tabSelected (event :ItemClickEvent = null) :void
    {
        _selectedIndex = event == null ? _selectedIndex : event.index;
        // this is a stupid hack, but it seems to be the only way to get "Super"TabNav to actually
        // do what's its supposed to and allow some tabs to be closeable and others not.
        closePolicy = _selectedIndex < 1 ? SuperTab.CLOSE_NEVER : SuperTab.CLOSE_SELECTED;

        // if our _selectedIndex is -1, we've just hidden the history, so no change to the display.
        if (_selectedIndex != -1) {
            // make sure our chat history is visible before we notify the overlay.
            if (!Prefs.getShowingChatHistory()) {
                Prefs.setShowingChatHistory(true);
            }

            _currentController = 
                _tabs.getItemAt(_selectedIndex).controller as ChatChannelController;
            if (_currentController != null) {
                _currentController.displayChat();
            } else {
                var overlay :ChatOverlay = _ctx.getTopPanel().getChatOverlay();
                if (overlay != null) {
                    overlay.setHistory(getDirectorHistory());
                }
            }
        } else {
            // if the history is hidden, make sure chat goes to the room.
            _currentController = null;
        }
    }

    protected var _tabs :ArrayCollection = new ArrayCollection;

    // The value returned from get selectedIndex() does not always reflect the value that was 
    // just immeadiately set via set selectedIndex(), so lets keep track of what we really want.
    protected var _selectedIndex :int = -1;

    // The tab to re-enable when chat is un-hidden, if it was done some other way from clicking
    // directly on a tab.
    protected var _unhideIndex :int = -1;

    protected var _currentController :ChatChannelController;
    protected var _ctx :WorldContext;
    protected var _chatDirector :ChatDirector;
}
}
