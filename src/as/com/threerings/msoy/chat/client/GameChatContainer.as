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

import com.threerings.util.Log;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.MsoyContext;

public class GameChatContainer extends LayeredContainer
{
    public function GameChatContainer (ctx :MsoyContext, chatDtr :ChatDirector, 
        playerList :UIComponent)
    {
        _ctx = ctx;
        _chatDtr = chatDtr;

        width = TopPanel.RIGHT_SIDEBAR_WIDTH;
        height = 500; // games are given 500 vertical pixels, so so are we.
        styleName = "gameChatContainer";

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

        var controlBar :ControlBar = _ctx.getTopPanel().getControlBar();
        controlBar.inSidebar(true);
        controlBar.setChatDirector(_chatDtr);

        addEventListener(Event.ADDED_TO_STAGE, handleAdd);
    }
    
    public function getChatOverlay () :ChatOverlay
    {
        return _overlay;
    }

    public function shutdown () :void
    {
        if (_channelOccList != null && _channelOccList.parent == this) {
            _channelOccList.width = 316;
            removeChild(_channelOccList);
            _channelOccList = null;
        }
        _chatDtr.removeChatDisplay(_overlay);
        _ctx.getTopPanel().getHeaderBar().replaceTabsContainer();
        var controlBar :ControlBar = _ctx.getTopPanel().getControlBar();
        controlBar.inSidebar(false);
        controlBar.setChatDirector(_ctx.getMsoyChatDirector());
    }

    public function sendChat (message :String) :void
    {
        var result :String = _chatDtr.requestChat(null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _chatDtr.displayFeedback(null, result);
        }
    }

    public function displayOccupantList (occList :ChannelOccupantList) :void
    {
        if (_playerList.parent == this) {
            removeChild(_playerList);
        }

        if (_channelOccList != null) {
            if (_channelOccList.parent == this) {
                removeChild(_channelOccList);
            }
            _channelOccList.width = 316;
            _channelOccList = null;
        }

        if (occList == null) {
            addChild(_playerList);
        } else {
            _channelOccList = occList;
            _channelOccList.width = 300;
            addChild(_channelOccList);
        }
    }

    protected function handleAdd (event :Event) :void
    {
        _overlay = new ChatOverlay(_ctx, this, ChatOverlay.SCROLL_BAR_RIGHT, false);
        _overlay.setClickableGlyphs(true);
        // this overlay needs to listen on both the msoy and game chat directors
        _chatDtr.addChatDisplay(_overlay);
        _ctx.getMsoyChatDirector().addChatDisplay(_overlay);
        var chatTop :Number = _tabBar.y + _tabBar.height;
        _overlay.setTargetBounds(new Rectangle(0, chatTop, width, height - chatTop));
    }

    private static const log :Log = Log.getLog(GameChatContainer);

    protected var _ctx :MsoyContext;
    protected var _overlay :ChatOverlay;
    protected var _chatDtr :ChatDirector;
    protected var _playerList :UIComponent;
    protected var _channelOccList :ChannelOccupantList;
    protected var _tabBar :HBox;
}
}
