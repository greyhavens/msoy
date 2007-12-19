//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.StringUtil;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

/**
 * Sets the user as being away.
 */
public class AwayHandler extends CommandHandler
{
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String,
        history :Array) :String
    {
        (ctx as MsoyContext).getMsoyController().setAway(true, args);
        return ChatCodes.SUCCESS;
    }
}
}
