//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.HashMap;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.msoy.client.WorldContext;

/**
 * Handles the dispatching of chat messages based on their "channel" (room/game, individual, or
 * actual custom channel). Manages chat history tracking for same.
 */
public class MsoyChatDirector extends ChatDirector
{
    public function MsoyChatDirector (ctx :WorldContext)
    {
        super(ctx, ctx.getMessageManager(), "general");
        _wctx = ctx;
        _ccpanel = new ChatChannelPanel(_wctx);
    }

    /**
     * Makes the specified chat overlay the current target for room chat. Any previous target will
     * no longer receive chat until {@link #popRoomChatOverlay} is called to remove this overlay
     * from the top of the stack.
     */
    public function pushRoomChatOverlay (target :ChatOverlay) :void
    {
        _roomTargets.unshift(target);
    }

    /**
     * Removes the specified room chat overlay from the list.
     */
    public function popRoomChatOverlay (target :ChatOverlay) :void
    {
        var idx :int = _roomTargets.indexOf(target);
        if (idx != -1) {
            _roomTargets.splice(idx, 1);
        }
    }

    /**
     * Opens a chat channel to the specified friend, selecting the appropriate tab if said channel
     * is already open.
     */
    public function openFriendChannel (friend :Name) :void
    {
        var channel :ChatChannel = ChatChannel.makeFriendChannel(friend);
        _ccpanel.getChatDisplay(channel, getHistory(channel), true);
    }

    // from ChatDirector
    override public function pushChatDisplay (display :ChatDisplay) :void
    {
        if (display is ChatOverlay) {
            (display as ChatOverlay).setHistory(_roomHistory);
        }
        super.pushChatDisplay(display);
    }

    // from ChatDirector
    override public function addChatDisplay (display :ChatDisplay) :void
    {
        if (display is ChatOverlay) {
            (display as ChatOverlay).setHistory(_roomHistory);
        }
        super.addChatDisplay(display);
    }

    // from ChatDirector
    override public function dispatchMessage (msg :ChatMessage) :void
    {
        // determine which channel to which this message is targeted
        var channel :ChatChannel = determineChannel(msg);
        if (channel != null) {
            var history :HistoryList = getHistory(channel);
            history.addMessage(msg);
            _ccpanel.getChatDisplay(channel, history, false).displayMessage(msg, false);

        } else {
            // add this message to the room chat history
            _roomHistory.addMessage(msg);
            // dispatch it normally as room chat displays are registered normally
            super.dispatchMessage(msg);
        }
    }

    /**
     * Maps the supplied chat message to the (translatable string) name of a chat channel. Returns
     * null if the message is from the current room rather than a chat channel.
     */
    protected function determineChannel (msg :ChatMessage) :ChatChannel
    {
        if ((msg.localtype == ChatCodes.USER_CHAT_TYPE && msg is UserMessage) ||
            msg is TellFeedbackMessage) {
            var umsg :UserMessage = (msg as UserMessage);
            return ChatChannel.makeFriendChannel(umsg.getSpeakerDisplayName() as Name);
        }
        // TODO: real custom chat channels
        return null;
    }

    /**
     * Returns the chat history for the specified channel, creating it if necessary.
     */
    protected function getHistory (channel :ChatChannel) :HistoryList
    {
        var history :HistoryList = (_histories.get(channel) as HistoryList);
        if (history == null) {
            _histories.put(channel, history = new HistoryList());
        }
        return history;
    }

    protected var _wctx :WorldContext;
    protected var _ccpanel :ChatChannelPanel;

    protected var _roomTargets :Array = new Array();
    protected var _roomHistory :HistoryList = new HistoryList();

    protected var _targets :HashMap = new HashMap();
    protected var _histories :HashMap = new HashMap();
}
}
