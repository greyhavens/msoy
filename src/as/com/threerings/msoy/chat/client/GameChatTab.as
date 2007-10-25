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

    /** Our game chat director. */
    protected var _chatDtr :ChatDirector;

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;
}
}
