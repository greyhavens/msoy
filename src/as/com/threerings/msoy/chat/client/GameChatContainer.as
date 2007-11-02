//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

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
        _playerList = playerList;

        width = 300;

        playerList.includeInLayout = false;
        addChild(playerList);

        _overlay = new ChatOverlay(ctx.getMessageManager());
        _overlay.setSubtitlePercentage(.75);
        _overlay.setClickableGlyphs(true);
        _chatDtr.addChatDisplay(_overlay);

        _chatDtr = chatDtr;
        _playerList = playerList;

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
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
        if (event.type == Event.ADDED_TO_STAGE) {
            _overlay.setTarget(this, TopPanel.RIGHT_SIDEBAR_WIDTH);
        } else {
            _overlay.setTarget(null);
        }
    }

    // from Container
    override protected function updateDisplayList (
        unscaledWidth :Number, unscaledHeight :Number) :void
    {
        // hand-position the playerList so that it consumes 25% of the height
        // (The chat is set to use 75%, in our constructor.)
        const GAP :int = 10;
        _playerList.x = GAP;
        _playerList.y = 0;
        _playerList.width = unscaledWidth - GAP;
        _playerList.height = int(unscaledHeight * .25) - GAP;

        super.updateDisplayList(unscaledWidth, unscaledHeight);
    }

    protected var _ctx :WorldContext;
    protected var _overlay :ChatOverlay;
    protected var _chatDtr :ChatDirector;
    protected var _playerList :UIComponent;
}
}
