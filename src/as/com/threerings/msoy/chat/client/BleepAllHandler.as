//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MsoyCodes;

/**
 * Sets global bleep status.
 */
public class BleepAllHandler extends CommandHandler
{
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String,
        history :Array) :String
    {
        const turnOn :Boolean = !Prefs.isGlobalBleep();
        Prefs.setGlobalBleep(turnOn);
        if (turnOn) {
            ctx.getChatDirector().displayFeedback(MsoyCodes.GENERAL_MSGS, "m.bleepall");
        }
        return ChatCodes.SUCCESS;
    }
}
}
