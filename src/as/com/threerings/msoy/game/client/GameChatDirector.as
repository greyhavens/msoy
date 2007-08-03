//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.HistoryList;

/**
 * Does some special business for handling chat on the game server side of things.
 */
public class GameChatDirector extends ChatDirector
{
    public function GameChatDirector (ctx :GameContext)
    {
        super(ctx, ctx.getMessageManager(), MsoyCodes.CHAT_MSGS);
    }

    // from ChatDirector
    override public function pushChatDisplay (display :ChatDisplay) :void
    {
        if (display is ChatOverlay) {
            (display as ChatOverlay).setHistory(_gameHistory);
        }
        super.pushChatDisplay(display);

        // redirect chat to this chat director
        (_ctx as GameContext).getTopPanel().getControlBar().setChatDirector(this);
    }

    // from ChatDirector
    override public function addChatDisplay (display :ChatDisplay) :void
    {
        if (display is ChatOverlay) {
            (display as ChatOverlay).setHistory(_gameHistory);
        }
        super.addChatDisplay(display);

        // redirect chat to this chat director
        (_ctx as GameContext).getTopPanel().getControlBar().setChatDirector(this);
    }

    // from ChatDirector
    override public function removeChatDisplay (display :ChatDisplay) :void
    {
        super.removeChatDisplay(display);

        if (_displays.size() == 0) {
            // restore chat to the default director
            (_ctx as GameContext).getTopPanel().getControlBar().setChatDirector(null);
        }
    }

    // from ChatDirector
    override public function clearDisplays () :void
    {
        super.clearDisplays();
        _gameHistory.clear();
    }

    // from ChatDirector
    override protected function dispatchPreparedMessage (msg :ChatMessage) :void
    {
        _gameHistory.addMessage(msg);
        super.dispatchPreparedMessage(msg);
    }

    protected var _gameHistory :HistoryList = new HistoryList();
}
}
