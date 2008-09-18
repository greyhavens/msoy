//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MsoyCodes;

/**
 * Does some special business for handling chat on the game server side of things.
 */
public class GameChatDirector extends ChatDirector
{
    public function GameChatDirector (gctx :GameContext)
    {
        super(gctx, gctx.getMsoyContext().getMessageManager(), MsoyCodes.CHAT_MSGS);
        _mctx = gctx.getMsoyContext();
        addChatDisplay(_mctx.getTopPanel().getHeaderBar().getChatTabs());
        addChatDisplay(_mctx.getMsoyChatDirector().getHistoryList());
    }

    // from ChatDirector
    override public function requestChat (speakSvc :SpeakService, text :String, 
        record :Boolean) :String
    {
        if (_mctx.getTopPanel().getHeaderBar().getChatTabs().
                getCurrentLocalType() != ChatCodes.PLACE_CHAT_TYPE) {
            // if there is a tab other than game active, let the other chat director handle it.
            return _mctx.getMsoyChatDirector().requestChat(speakSvc, text, record);
        }
        return super.requestChat(speakSvc, text, record);
    }

    override protected function suppressTooManyCaps () :Boolean
    {
        return false;
    }

    override protected function clearChatOnClientExit () :Boolean
    {
        return false; // TODO: we need this because on msoy we "exit" when change servers
    }

    protected var _mctx :MsoyContext;
}
}
