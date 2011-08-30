//
// $Id$

package com.threerings.msoy.tutorial.client {

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.world.client.WorldContext;

/**
 * Chat command for popping up a tutorial tip for testing.
 */
public class TutorialHandler extends CommandHandler
{
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String, history :Array)
        :String
    {
        var names :Array = args.split(/ +/).filter(function (str :String, ...rest) :Boolean {
            return str.length > 0;
        });

        if (names.length != 1) {
            return "m.usage_tut";
        }

        WorldContext(ctx).getTutorialDirector().testTip(names[0]);
        return ChatCodes.SUCCESS;
    }
}
}
