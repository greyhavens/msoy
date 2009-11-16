//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.MessageBundle;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;

/**
 * Chat command for listing and manipulating flash cookies.
 */
public class WipeHandler extends CommandHandler
{
    override public function handleCommand (
        ctx :CrowdContext, speakSvc :SpeakService, cmd :String, args :String, history :Array)
        :String
    {
        var names :Array = args.split(/ +/).filter(function (str :String, ...rest) :Boolean {
            return str.length > 0;
        });

        var msoyChat :String = Msgs.CHAT.getPath();

        if (names.length == 0) {
            return "m.usage_wipe";

        } else if (names.indexOf("all") >= 0) {
            if (names.length != 1) {
                return "m.usage_wipe";
            }
            names = Prefs.ALL_KEYS;

        } else if (names.indexOf("list") >= 0) {
            if (names.length != 1) {
                return "m.usage_wipe";
            }
            ctx.getChatDirector().displayFeedback(msoyChat, "m.wipe_available");
            for each (var name2 :String in Prefs.ALL_KEYS) {
                for each (var pair :Array in Prefs.getByName(name2)) {
                    ctx.getChatDirector().displayFeedback(msoyChat,
                        "  " + pair[0] + " = " + pair[1]);
                }
            }
            return ChatCodes.SUCCESS;

        }  else {
            for each (var name :String in names) {
                if (Prefs.ALL_KEYS.indexOf(name) == -1) {
                    return MessageBundle.tcompose("m.wipe_unknown", name);
                }
            }
        }

        var count :int = Prefs.removeAll(names);
        ctx.getChatDirector().displayFeedback(msoyChat,
            MessageBundle.tcompose("m.wipe_success", String(count)));
        return ChatCodes.SUCCESS;
    }
}
}
