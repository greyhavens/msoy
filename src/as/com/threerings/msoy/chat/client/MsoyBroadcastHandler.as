//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.BroadcastHandler;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.client.MsoyContext;

/**
 * Msoy version of broadcasting. Allow non-admins to access the command but shows a confirmation
 * panel (that will charge them money and then send a slightly less important looking broadcast
 * message).
 */
public class MsoyBroadcastHandler extends BroadcastHandler
{
    override public function checkAccess (user :BodyObject) :Boolean
    {
        // no permaguests
        return (user is MemberObject) && !MemberObject(user).isPermaguest();
    }

    override protected function doBroadcast (ctx :CrowdContext, msg :String) :void
    {
        // if they have access to the normal broadcast, that's what they get
        if (super.checkAccess(BodyObject(ctx.getClient().getClientObject()))) {
            super.doBroadcast(ctx, msg);

        } else {
            BroadcastPanel.show(MsoyContext(ctx), msg);
        }
    }
}
}
