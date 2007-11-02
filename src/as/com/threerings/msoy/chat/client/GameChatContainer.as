//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import flash.geom.Rectangle;

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

        _overlay = new ChatOverlay(_ctx.getMessageManager());
        _overlay.setClickableGlyphs(true);
        _chatDtr.addChatDisplay(_overlay);

        _chatDtr = chatDtr;
        _playerList = playerList;
        _playerList.x = PAD;
        _playerList.y = PAD;
        _playerList.width = width - PAD * 2;
        addChild(playerList);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
    }

    public function shutdown () :void
    {
        _chatDtr.removeChatDisplay(_overlay);
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
        var chatTop :Number = _playerList.y + _playerList.height;
        _overlay.setTarget(this, new Rectangle(PAD, chatTop, width - PAD * 2, 
            height - chatTop - PAD));
    }

    protected static const PAD :int = 0; // set to non-0 for some padding around the edges

    protected var _ctx :WorldContext;
    protected var _overlay :ChatOverlay;
    protected var _chatDtr :ChatDirector;
    protected var _playerList :UIComponent;
}
}
