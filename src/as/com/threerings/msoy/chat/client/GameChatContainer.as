//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import flash.geom.Rectangle;

import mx.containers.HBox;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;

public class GameChatContainer extends LayeredContainer
{
    public function GameChatContainer (ctx :WorldContext, chatDtr :ChatDirector, 
        playerList :UIComponent)
    {
        _ctx = ctx;
        _chatDtr = chatDtr;

        var topPanel :TopPanel = _ctx.getTopPanel();
        width = TopPanel.RIGHT_SIDEBAR_WIDTH;
        height = 500; // games are given 500 vertical pixels, so so are we.

        _overlay = new ChatOverlay(_ctx.getMessageManager(), ChatOverlay.SCROLL_BAR_RIGHT);
        _overlay.setClickableGlyphs(true);
        _chatDtr.addChatDisplay(_overlay);

        _chatDtr = chatDtr;
        _playerList = playerList;
        _playerList.width = width;
        addChild(playerList);

        var tabs :UIComponent = _ctx.getTopPanel().getHeaderBar().removeTabsContainer();
        _tabBar = new HBox();
        _tabBar.horizontalScrollPolicy = ScrollPolicy.OFF;
        _tabBar.y = _playerList.height
        _tabBar.height = _ctx.getTopPanel().getHeaderBar().height;
        _tabBar.width = width;
        _tabBar.styleName = "headerBar";
        _tabBar.addChild(tabs);
        addChild(_tabBar);

        _ctx.getTopPanel().getControlBar().inSidebar(true);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
    }

    public function shutdown () :void
    {
        _chatDtr.removeChatDisplay(_overlay);
        _ctx.getTopPanel().getHeaderBar().replaceTabsContainer();
        _ctx.getTopPanel().getControlBar().inSidebar(false);
    }

    public function sendChat (message :String) :void
    {
        var result :String = _chatDtr.requestChat(null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _chatDtr.displayFeedback(null, result);
        }
    }

    protected function handleAddRemove (event :Event) :void
    {
        var chatTop :Number = _tabBar.y + _tabBar.height;
        _overlay.setTarget(this, new Rectangle(0, chatTop, width, height - chatTop));
        _overlay.setHistoryEnabled(true);
    }

    protected var _ctx :WorldContext;
    protected var _overlay :ChatOverlay;
    protected var _chatDtr :ChatDirector;
    protected var _playerList :UIComponent;
    protected var _tabBar :HBox;
}
}
