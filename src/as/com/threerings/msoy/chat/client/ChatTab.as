//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.Container;

import com.threerings.msoy.client.WorldContext;

/**
 * A base for our channel chat tabs.
 */
public class ChatTab extends Container
{
    public function ChatTab (ctx :WorldContext)
    {
        styleName = "channelChatTab";
        _ctx = ctx;
    }

    public function sendChat (message :String) :void
    {
        throw new Error("abstract");
    }

    /** Giver of life, context. */
    protected var _ctx :WorldContext;
}
}
