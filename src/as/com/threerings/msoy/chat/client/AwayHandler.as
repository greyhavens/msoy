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
    // Note that we are registered to handle "dnd", "away", "afk", and "back".
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String,
        history :Array) :String
    {
        const mctx :MsoyContext = MsoyContext(ctx);
        // filter the args first so that we turn naughty things into blank
        args = ctx.getChatDirector().filter(args, null, true);
        const hasMsg :Boolean = !StringUtil.isBlank(args);
        var msg :String = null;

        if (cmd == "back") {
            if (hasMsg) {
                return "m.usage_back"; // back can only be used to disable awayness
            }

        } else if (hasMsg || (cmd != "dnd") ||
                !MemberObject(mctx.getClient().getClientObject()).isAway()) {
            msg = hasMsg ? args : Msgs.GENERAL.get("m.awayDefault");
        }
        // do the change, give feedback
        var feedback :String = (msg == null) ? "m.back" : MessageBundle.tcompose("m.away", msg);
        MemberService(mctx.getClient().requireService(MemberService)).setAway(
            mctx.getClient(), msg, mctx.confirmListener(feedback));
        return ChatCodes.SUCCESS;
    }
}
}
