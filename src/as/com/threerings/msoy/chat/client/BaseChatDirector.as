//
// $Id$

package com.threerings.msoy.chat.client {

import flash.utils.getTimer; // function

import com.threerings.util.MessageBundle;
import com.threerings.util.Throttle;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.client.MsoyContext;

public class BaseChatDirector extends ChatDirector
{
    /** The maximum size of any utterance. */
    public static const MAX_CHAT_LENGTH :int = 200;

    public function BaseChatDirector (ctx :CrowdContext, mctx :MsoyContext)
    {
        super(ctx, mctx.getMessageManager(), MsoyCodes.CHAT_MSGS);
        _mctx = mctx;

        var msg :MessageBundle = _msgmgr.getBundle(_bundle);

        registerCommandHandler(msg, "away", new AwayHandler());
        registerCommandHandler(msg, "bleepall", new BleepAllHandler());

        // override the broadcast command from ChatDirector
        registerCommandHandler(msg, "broadcast", new MsoyBroadcastHandler());

        // Ye Olde Easter Eggs
        registerCommandHandler(msg, "~egg", new HackHandler(function (args :String) :void {
            _handlers.remove("~egg");
            _mctx.getControlBar().setFullOn();
            SubtitleGlyph.thumbsEnabled = true;
            displayFeedback(null, MessageBundle.taint("Easter eggs enabled:\n" +
                " * Full-screen button (no chat, due to flash security).\n" +
                " * Chat link hover pics.\n" +
                "\n" +
                "These experimental features may be removed in the future. Let us know if you " +
                "find them incredibly useful."));
        }));
    }

    // from ChatDirector
    override protected function suppressTooManyCaps () :Boolean
    {
        return false;
    }

    // from ChatDirector
    override protected function clearChatOnClientExit () :Boolean
    {
        return false; // TODO: we need this because on msoy we "exit" when change servers
    }

    // from ChatDirector
    override protected function checkCanChat (
        speakSvc :SpeakService, message :String, mode :int) :String
    {
        var now :int = getTimer();
        if (_throttle.throttleOpAt(now)) {
            return "e.chat_throttled";
        }
        // if we allow it, we might also count this message as more than one "op"
        if (message.length > 8) {
            _throttle.noteOp(now);
        }
        if (message.length > (MAX_CHAT_LENGTH / 2)) {
            _throttle.noteOp(now);
        }
        return null;
    }

    protected var _mctx :MsoyContext;

    /** You may utter 8 things per 5 seconds, but large things count as two. */
    protected var _throttle :Throttle = new Throttle(8, 5000);
}
}
