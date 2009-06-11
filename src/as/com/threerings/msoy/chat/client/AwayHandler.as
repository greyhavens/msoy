//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.MessageBundle;
import com.threerings.util.StringUtil;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

/**
 * Sets the user's away status.
 */
public class AwayHandler extends CommandHandler
{
    // Note that we are registered to handle "away", "afk", and "back".
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String,
        history :Array) :String
    {
        const mctx :MsoyContext = MsoyContext(ctx);
        //const isAway :Boolean = MemberObject(mctx.getClient().getClientObject()).isAway();
        args = ctx.getChatDirector().filter(args, null, true);
        const hasMsg :Boolean = !StringUtil.isBlank(args);
        var message :String;
        var feedback :String;

        if (cmd == "back") {
            // back can only be used to disable awayness
            if (hasMsg) {
                return "m.usage_back";
            }
            message = null;
            feedback = "m.back";

        } else {
            message = hasMsg ? args : Msgs.GENERAL.get("m.awayDefault");
            feedback = MessageBundle.tcompose("m.away", message);
        }

        // do the change, give feedback
        MemberService(mctx.getClient().requireService(MemberService)).setAway(
            mctx.getClient(), message, mctx.confirmListener(feedback));
        return ChatCodes.SUCCESS;
    }
}
}
