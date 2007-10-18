//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;

import com.whirled.client.PlayerList;

import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;

/**
 * Displays chat during a game in the sidebar.
 */
public class GameChatTab extends ChatTab
{
    public function GameChatTab (ctx :WorldContext, chatDtr :ChatDirector, plobj :PlaceObject)
    {
        super(ctx);
        _chatDtr = chatDtr;

        _playerList = new PlayerList();
        addChild(_playerList);
        _playerList.startup(plobj);

        _overlay = new ChatOverlay(ctx.getMessageManager());
        _overlay.setSubtitlePercentage(.5);
        _overlay.setClickableGlyphs(true);
        _chatDtr.addChatDisplay(_overlay);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    public function shutdown () :void
    {
        _chatDtr.removeChatDisplay(_overlay);
        _playerList.shutdown();
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

    /** Displays the occupants and players of the game. */
    protected var _playerList :PlayerList;

    /** Our game chat director. */
    protected var _chatDtr :ChatDirector;

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;
}
}
