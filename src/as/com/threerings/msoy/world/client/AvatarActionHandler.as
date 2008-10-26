//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.MessageBundle;
import com.threerings.util.StringUtil;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.crowd.chat.client.CommandHandler;
import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.room.client.RoomView;

/**
 * Allows users to switch their avatar's state or trigger an action from /slash commands entered in
 * chat.
 */
public class AvatarActionHandler extends CommandHandler
{
    public function AvatarActionHandler (ctx :WorldContext, states :Boolean)
    {
        _wctx = ctx;
        _states = states;
    }

    override public function handleCommand (ctx :CrowdContext, speakSvc :SpeakService,
                                            cmd :String, args :String, history :Array) :String
    {
        if (StringUtil.isBlank(args)) {
            return "m.usage_" + cmd;
        }

        var roomView :RoomView = (_wctx.getPlaceView() as RoomView);
        if (roomView == null) {
            // can't do it, not in a room
            return "e.avatar_only_cmd";
        }

        var actions :Array = _states ? roomView.getMyStates() : roomView.getMyActions();
        var match :String = findBestMatch(args, actions);
        if (match == null) {
            var choices :String = "";
            if (actions.length > 0) {
                choices = " " + Msgs.CHAT.get("m.choices", "\"" + actions.join("\", \"") + "\"");
            }

            return MessageBundle.tcompose(
                _states ? "e.no_matching_states" : "e.no_matching_actions", choices);
        }

        if (_states) {
            roomView.getRoomController().doAvatarState(match);
        } else {
            roomView.getRoomController().doAvatarAction(match);
        }
        history[0] = cmd + " " + match;
        _wctx.getChatDirector().displayFeedback(MsoyCodes.CHAT_MSGS,
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

    protected var _wctx :WorldContext;

    /** Are we working with states rather than actions? */
    protected var _states :Boolean;
}
}
