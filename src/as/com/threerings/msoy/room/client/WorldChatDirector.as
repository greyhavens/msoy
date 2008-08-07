//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.util.MessageBundle;

import com.threerings.msoy.chat.client.MsoyChatDirector;

/**
 * Extends our standard chat director with custom world bits.
 */
public class WorldChatDirector extends MsoyChatDirector
{
    public function WorldChatDirector (ctx :WorldContext)
    {
        super(ctx);

        var msg :MessageBundle = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "action", new AvatarActionHandler(ctx, false));
        registerCommandHandler(msg, "state", new AvatarActionHandler(ctx, true));
    }
}
}
