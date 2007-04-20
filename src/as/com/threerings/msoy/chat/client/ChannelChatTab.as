//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import com.threerings.util.Name;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.client.WorldContext;

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
        // TODO: the right thing for friends vs real chat channels
        _ctx.getChatDirector().requestTell(channel.ident as Name, message, null);
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
