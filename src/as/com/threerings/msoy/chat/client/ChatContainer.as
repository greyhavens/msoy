//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import mx.core.Container;

import com.threerings.msoy.client.WorldContext;

/**
 * A very simple container that merely hosts the chat overlay.
 */
public class ChatContainer extends Container
{
    public function ChatContainer (ctx :WorldContext)
    {
        _ctx = ctx;
        _overlay = new ChatOverlay(ctx.getMessageManager());
        _overlay.setClickableGlyphs(true);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            _ctx.getChatDirector().addChatDisplay(_overlay);
            _overlay.setTarget(this);
        } else {
            _ctx.getChatDirector().removeChatDisplay(_overlay);
            _overlay.setTarget(null);
        }
    }

    /** Giver of life, context. */
    protected var _ctx :WorldContext;

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;
}
}
