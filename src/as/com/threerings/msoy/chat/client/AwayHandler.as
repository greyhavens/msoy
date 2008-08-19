//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.MsoyContext;

/**
 * Sets the user's away status.
 */
public class AwayHandler extends CommandHandler
{
    public function AwayHandler (away :Boolean = true)
    {
        _away = away;
    }

    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String,
        history :Array) :String
    {
        (ctx as MsoyContext).getMsoyController().setAway(_away, args);
        return ChatCodes.SUCCESS;
    }

    protected var _away :Boolean;
}
}
