//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.util.CrowdContext;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.util.Log;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.badge.ui.BadgeListPanel;

public class BadgesHandler extends CommandHandler
{
    // from CommandHandler
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String, 
        history :Array) :String
    {
        var client :Client = ctx.getClient();
        var msvc :MemberService = client.requireService(MemberService) as MemberService;
        msvc.loadAllBadges(client, new ResultAdapter(function (cause :String) :void {
                log.warning("Unable load badges [cause=" + cause + "].");
            }, function (badges :Array /* of EarnedBadge */) :void {
                (new BadgeListPanel(ctx as MsoyContext, badges)).open();
            }));

        return ChatCodes.SUCCESS;
    }

    protected var log :Log = Log.getLog(BadgesHandler);
}
}
