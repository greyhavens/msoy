package com.threerings.msoy.chat.client {

import flash.display.DisplayObjectContainer;

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
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        _overlay.setTarget((p == null) ? null : this);
    }

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;
}
}
