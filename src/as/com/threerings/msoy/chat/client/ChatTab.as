//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.Container;
import mx.core.ScrollPolicy;

import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.WorldContext;

/**
 * A base for our channel chat tabs.
 */
public class ChatTab extends LayeredContainer
{
    public function ChatTab (ctx :WorldContext)
    {
        styleName = "channelChatTab";
        _ctx = ctx;

        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;
    }

    public function sendChat (message :String) :void
    {
        throw new Error("abstract");
    }

    /** Giver of life, context. */
    protected var _ctx :WorldContext;
}
}
