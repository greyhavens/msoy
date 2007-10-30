//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import mx.core.UIComponent;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;

/**
 * Displays chat during a game in the sidebar.
 */
public class GameChatTab extends ChatTab
{
    public function GameChatTab (ctx :WorldContext, chatDtr :ChatDirector, playerList :UIComponent)
    {
        super(ctx);
        _chatDtr = chatDtr;
        _playerList = playerList;

        playerList.includeInLayout = false;
        addChild(playerList);

        _overlay = new ChatOverlay(ctx.getMessageManager());
        _overlay.setSubtitlePercentage(.75);
        _overlay.setClickableGlyphs(true);
        _chatDtr.addChatDisplay(_overlay);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    public function shutdown () :void
    {
        _chatDtr.removeChatDisplay(_overlay);
    }

    // @Override // from ChatTab
    override public function sendChat (message :String) :void
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

    /** Our game chat director. */
    protected var _chatDtr :ChatDirector;

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;

    /** Displays occupants and scores. */
    protected var _playerList :UIComponent;
}
}
