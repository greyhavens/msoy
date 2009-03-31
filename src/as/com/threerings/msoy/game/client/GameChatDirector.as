//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.BaseChatDirector;

/**
 * Does some special business for handling chat on the game server side of things.
 */
public class GameChatDirector extends BaseChatDirector
{
    public function GameChatDirector (gctx :GameContext)
    {
        super(gctx, gctx.getMsoyContext());
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
}
}
