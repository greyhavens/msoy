//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.MessageBundle;
import com.threerings.util.StringUtil;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.msoy.data.MsoyCodes;

/**
 * Allows users to switch their avatar's state or trigger an action from /slash commands entered in
 * chat.
 */
public class AvatarActionHandler extends CommandHandler
{
    public function AvatarActionHandler (states :Boolean)
    {
        _states = states;
    }

    override public function handleCommand (ctx :CrowdContext, speakSvc :SpeakService,
                                            cmd :String, args :String, history :Array) :String
    {
        if (StringUtil.isBlank(args)) {
            return "m.usage_" + cmd;
        }

        var wctx :WorldContext = (ctx as WorldContext);
        var roomView :RoomView = (wctx.getTopPanel().getPlaceView() as RoomView);
        if (roomView == null) {
            // can't do it, not in a room
            return "e.avatar_only_cmd";
        }

        var actions :Array = _states ? roomView.getMyStates() : roomView.getMyActions();
        var match :String = findBestMatch(args, actions);
        if (match == null) {
            return MessageBundle.tcompose(
                _states ? "e.no_matching_states" : "e.no_matching_actions",
                "\"" + actions.join("\", \"") + "\"");
        }

        if (_states) {
            roomView.getRoomController().doAvatarState(match);
        } else {
            roomView.getRoomController().doAvatarAction(match);
        }
        wctx.getChatDirector().displayFeedback(MsoyCodes.CHAT_MSGS,
            MessageBundle.tcompose(_states ? "m.changed_state" : "m.changed_action", match));
        return ChatCodes.SUCCESS;
    }

    /**
     * Find the action in the specified array that is the best match for the supplied arg.
     * The best match is the first action containing the case-insensitive arg at the
     * earliest position.
     */
    protected function findBestMatch (arg :String, actions :Array) :String
    {
        arg = arg.toLowerCase();

        var bestIdx :int = int.MAX_VALUE;
        var best :String = null;
        for each (var action :String in actions) {
            if (action != null) {
                // test against a lower-cased version of the action
                var idx :int = action.toLowerCase().indexOf(arg);
                if (idx != -1 && idx < bestIdx) {
                    // but if it matches, remember the original action
                    bestIdx = idx;
                    best = action;
                }
            }
        }
        return best;
    }

    /** Are we working with states rather than actions? */
    protected var _states :Boolean;
}
}
