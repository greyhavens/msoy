//
// $Id: AwayHandler.as 10968 2008-08-19 01:58:34Z nathan $

package com.threerings.msoy.chat.client {

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MemberObject;

/**
 * Sets global bleep status.
 */
public class BleepAllHandler extends CommandHandler
{
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String,
        history :Array) :String
    {
        Prefs.setGlobalBleep(!Prefs.isGlobalBleep());
        return ChatCodes.SUCCESS;
    }

    override public function checkAccess (user :BodyObject) :Boolean
    {
        return MemberObject(user).tokens.isSupport();
    }
}
}
