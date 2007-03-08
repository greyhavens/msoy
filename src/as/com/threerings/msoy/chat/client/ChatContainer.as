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
        _overlay = new ChatOverlay(ctx);
        _overlay.setClickableGlyphs(true);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    protected function handleAddRemove (event :Event) :void
    {
        _overlay.setTarget((event.type == Event.ADDED_TO_STAGE) ? this : null);
    }

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;
}
}
