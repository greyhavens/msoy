//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

/**
 * Makes it easy to wire up chat commands to trigger hacky testy stuff. Takes a function with the
 * following signature: function (args :String) :void.
 */
public class HackHandler extends CommandHandler
{
    public function HackHandler (onTrigger :Function)
    {
        _onTrigger = onTrigger;
    }

    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String, history :Array) :String
    {
        _onTrigger(args);
        return ChatCodes.SUCCESS;
    }

    protected var _onTrigger :Function;
}
}
