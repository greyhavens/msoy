//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import com.threerings.util.Name;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

/**
 * Displays an actual chat channel.
 */
public class ChannelChatTab extends ChatTab
{
    public var channel :ChatChannel;

    public function ChannelChatTab (ctx :WorldContext, channel :ChatChannel)
    {
        super(ctx);
        this.channel = channel;

        _overlay = new ChatOverlay(ctx);
        _overlay.setClickableGlyphs(true);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    public function getOverlay () :ChatOverlay
    {
        return _overlay;
    }

    // @Override // from ChatTab
    override public function sendChat (message :String) :void
    {
        if (channel.type == ChatChannel.FRIEND_CHANNEL) {
            _ctx.getChatDirector().requestTell(channel.ident as Name, message, null);

        } else {
            var ccobj :ChatChannelObject =
                (_ctx.getChatDirector() as MsoyChatDirector).getChannelObject(channel);
            var result :String = (ccobj == null) ? "m.channel_closed" :
                _ctx.getChatDirector().requestChat(ccobj.speakService, message, false);
            if (result != ChatCodes.SUCCESS) {
                _ctx.getChatDirector().dispatchMessage(createFeedback(result));
            }
        }
    }

    protected function createFeedback (message :String) :SystemMessage
    {
        var msg :SystemMessage = new SystemMessage();
        msg.attentionLevel = SystemMessage.FEEDBACK;
        msg.setClientInfo(Msgs.CHAT.xlate(message), channel.toLocalType());
        return msg;
    }

    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            _overlay.setTarget(this);
        } else {
            _overlay.setTarget(null);
        }
    }

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;
}
}
