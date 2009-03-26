//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.presents.client.Client;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.BroadcastHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.client.MsoyContext;

/**
 * Msoy version of broadcasting. Allow non-admins to access the command but shows a confirmation
 * panel (that will charge them money and then send a slightly less important looking broadcast
 * message).
 */
public class MsoyBroadcastHandler extends BroadcastHandler
{
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService,
        cmd :String, args :String, history :Array) :String
    {
        // get member object. if it is admin, do a normal broadcast
        var client :Client = ctx.getClient();
        var memObj :MemberObject = MemberObject(client.getClientObject());
        if (memObj == null || memObj.getTokens().isAdmin()) {
            return super.handleCommand(ctx, speakSvc, cmd, args, history);
        }

        // otherwise, show the confirmation popup. it will do the rest
        BroadcastPanel.show(MsoyContext(ctx), args);
        history[0] = cmd + " ";
        return ChatCodes.SUCCESS;
    }

    override public function checkAccess (user :BodyObject) :Boolean
    {
        // guests can't do this
        if (!(user is MemberObject) || MemberObject(user).isPermaguest()) {
            return super.checkAccess(user);
        }
        return true;
    }
}
}
