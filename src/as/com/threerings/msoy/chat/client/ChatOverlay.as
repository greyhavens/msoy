//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.Sprite;

//import flash.display.Bitmap;
//import flash.display.BitmapData;

//import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.core.IRawChildrenContainer;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ColorUtil;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.whirled.spot.data.SpotCodes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.MsoyContext;

public class ChatOverlay
    implements ChatDisplay
{
    public var log :Log = Log.getLog(this);

    public function ChatOverlay (ctx :MsoyContext)
    {
        _ctx = ctx;

        if (_history == null) {
            _history = new HistoryList();
            _ctx.getChatDirector().addChatDisplay(_history);
        }
    }

    public function setTarget (disp :DisplayObjectContainer) :void
    {
        if (_target != null) {
            _ctx.getChatDirector().removeChatDisplay(this);

            if (_target is IRawChildrenContainer) {
                (_target as IRawChildrenContainer).rawChildren.removeChild(_overlay);
            } else {
                _target.removeChild(_overlay);
            }
            _overlay = null;
        }

        _target = disp;

        if (_target != null) {
            _overlay = new Sprite();
            _overlay.x = PAD;
            _overlay.y = PAD + ControlBar.HEIGHT; // TEMP:hack
            if (_target is IRawChildrenContainer) {
                (_target as IRawChildrenContainer).rawChildren.addChild(_overlay);
            } else {
                _target.addChild(_overlay);
            }

            _ctx.getChatDirector().addChatDisplay(this);
        }
    }

    // from ChatDisplay
    public function clear () :void
    {
        if (_overlay != null) {
            // remove all children
            for (var dex :int = _overlay.numChildren - 1; dex >= 0; dex--) {
                _overlay.removeChildAt(dex);
            }
        }

        // clear the list of showing sprites
        _subtitles.length = 0;
    }

    // from ChatDisplay
    public function displayMessage (
        msg :ChatMessage, alreadyDisp :Boolean) :Boolean
    {
        if (_target == null) {
            return false;
        }

        return displayMessageNow(msg);
    }

    /**
     * Display the specified message now, unless we are to ignore it.
     *
     * @return true if the message was displayed.
     */
    protected function displayMessageNow (msg :ChatMessage) :Boolean
    {
        var type :int =  getType(msg, false);
        if (type == IGNORECHAT) {
            return false;
        }

        return displayTypedMessageNow(msg, type);
    }

    protected function displayTypedMessageNow (
        msg :ChatMessage, type :int) :Boolean
    {
        // TODO: history mode check here

        addSubtitle(createSubtitle(msg, type, true));
        return true;
    }

    protected function addSubtitle (glyph :ChatGlyph) :void
    {
        var height :int = glyph.height;

        glyph.x = 0;
        if (false) {
            glyph.y = getTargetHeight() - height;
            scrollUpSubtitles(height + getSubtitleSpacing(glyph.getType()));
        } else {
            glyph.y = 0;
            scrollDownSubtitles(height + getSubtitleSpacing(glyph.getType()));
        }
        _subtitles.push(glyph);
        _overlay.addChild(glyph);
    }

    protected function createSubtitle (
        msg :ChatMessage, type :int, expires :Boolean) :ChatGlyph
    {
        var text :String = msg.message;

        var format :String = formatOf(type);
        if (format != null) {
            var umsg :UserMessage = (msg as UserMessage);
            text = _ctx.xlate(null, format,
                umsg.getSpeakerDisplayName(), text);
        }
        var expireDuration :int;
        if (expires) {
            expireDuration = getChatExpire(msg.timestamp, text);
        } else {
            expireDuration = int.MAX_VALUE;
        }

        return new SubtitleGlyph(this, type, text, expireDuration);
    }

    protected function getChatExpire (stamp :int, text :String) :int
    {
        // TODO
        return 15000;
    }

    /**
     * Return the translation key we should use for formatting a message
     * of the specified type.
     */
    protected function formatOf (type :int) :String
    {
        switch (placeOf(type)) {
        case TELL: return "m.tell_format";
        case BROADCAST: return "m.broadcast_format";
        case PLACE: case GAME: 
            switch (modeOf(type)) {
            case SPEAK: return "m.speak_format";
            case SHOUT: return "m.shout_format";
            case EMOTE: return "m.emote_format";
            case THINK: return "m.think_format";
            }
        }

        return null; // no formatting
    }

    public function getTargetHeight () :int
    {
        var h :int = _target.height;
        h -= (PAD * 2);
        // TODO: temp: since we overwrite the control bar now
        h -= ControlBar.HEIGHT;
        return h;
    }

    public function getTargetWidth () :int
    {
        var w :int = _target.width;
        w -= (PAD * 2);
        return w;
    }

    protected function getOutlineColor (type :int) :uint
    {
        switch (type) {
        case BROADCAST: return BROADCAST_COLOR;
        case TELL: return TELL_COLOR;
        case TELLFEEDBACK: return TELLFEEDBACK_COLOR;
        case INFO: return INFO_COLOR;
        case FEEDBACK: return FEEDBACK_COLOR;
        case ATTENTION: return ATTENTION_COLOR;
        default:
            switch (placeOf(type)) {
            case GAME: return GAME_COLOR;
            default: return BLACK;
            }
        }
    }

    internal function drawSubtitleShape (
        g :Graphics, type :int, width :int, height :int) :void
    {
        var outline :uint = getOutlineColor(type);
        var background :uint;
        if (BLACK == outline) {
            background = WHITE;
        } else {
            background = ColorUtil.blend(WHITE, outline, .8);
        }
        width += PAD;
        height += 2;
        var xx :int = PAD/-2;

        // TODO (right now they all get the same sausage)
        g.clear();
        g.beginFill(background);
        g.drawRoundRect(xx, 0, width, height, 10, 10);
        g.endFill();

        g.lineStyle(1, outline);
        g.drawRoundRect(xx, 0, width, height, 10, 10);
    }

    /**
     * Called from the HistoryList to notify us that messages were added
     * to the history.
     *
     * @param adjustment if non-zero, the number of old history entries
     * that were pruned.
     */
    internal function historyUpdated (adjustment :int) :void
    {
        // TODO
    }

    internal function glyphExpired (glyph :ChatGlyph) :void
    {
        ArrayUtil.removeFirst(_subtitles, glyph);
        _overlay.removeChild(glyph);
    }

    /**
     * Convert the message class/localtype/mode into our internal type code.
     */
    protected function getType (msg :ChatMessage, history :Boolean) :int
    {
        var localtype :String = msg.localtype;

        if (msg is UserMessage) {
            var type :int = 0;

            if (ChatCodes.USER_CHAT_TYPE == localtype) {
                type = TELL;

            } else if (ChatCodes.PLACE_CHAT_TYPE == localtype ||
                    SpotCodes.CLUSTER_CHAT_TYPE == localtype) {
                type = PLACE;
            }
            // TODO: more types

            // factor in the mode
            if (type != 0) {
                switch ((msg as UserMessage).mode) {
                case ChatCodes.DEFAULT_MODE:
                    return type | SPEAK;

                case ChatCodes.EMOTE_MODE:
                    return type | EMOTE;

                case ChatCodes.THINK_MODE:
                    return type | THINK;

                case ChatCodes.SHOUT_MODE:
                    return type | SHOUT;

                case ChatCodes.BROADCAST_MODE:
                    return BROADCAST; // broadcast always looks like broadcast
                }
            }

        } else if (msg is TellFeedbackMessage) {
            return (history || isApprovedLocalType(localtype)) ? TELLFEEDBACK
                                                               : IGNORECHAT;

        } else if (msg is SystemMessage) {
            if (history || isApprovedLocalType(localtype)) {
                switch ((msg as SystemMessage).attentionLevel) {
                case SystemMessage.INFO:
                    return INFO;

                case SystemMessage.FEEDBACK:
                    return FEEDBACK;

                case SystemMessage.ATTENTION:
                    return ATTENTION;

                default:
                    log.warning("Unknown attention level for system message " +
                        "[msg=" + msg + "].");;
                    break;
                }
            }

            // otherwise
            return IGNORECHAT;
        }

        log.warning("Skipping received message of unknown type " +
            "[msg=" + msg + "].");
        return IGNORECHAT;
    }

    /**
     * Check to see if we want ti display the specified localtype.
     */
    protected function isApprovedLocalType (localtype :String) :Boolean
    {
        // we show everything
        return true;
    }

    protected function getSubtitleSpacing (type :int) :int
    {
        switch (placeOf(type)) {
        default:
            return 1;
        }
    }

    protected function scrollUpSubtitles (dy :int) :void
    {
        for (var ii :int = 0; ii < _subtitles.length; ii++) {
            var glyph :ChatGlyph = (_subtitles[ii] as ChatGlyph);
            var newY :int = glyph.y - dy;
            if (newY + glyph.height < 0) {
                _overlay.removeChild(glyph);
                _subtitles.splice(ii, 1);
                ii--;

            } else {
                glyph.y = newY;
            }
        }
    }

    protected function scrollDownSubtitles (dy :int) :void
    {
        var maxH :int = getTargetHeight();

        for (var ii :int = 0; ii < _subtitles.length; ii++) {
            var glyph :ChatGlyph = (_subtitles[ii] as ChatGlyph);
            var newY :int = glyph.y + dy;
            if (newY > maxH) {
                _overlay.removeChild(glyph);
                _subtitles.splice(ii, 1);
                ii--;

            } else {
                glyph.y = newY;
            }
        }
    }

    /**
     * Extract the mode constant from the type value.
     */
    protected function modeOf (type :int) :int
    {
        return (type & 0xF);
    }

    /**
     * Extract the place constant from the type value. 
     */
    protected function placeOf (type :int) :int
    {
        return (type & ~0xF);
    }

    /** The light of our life. */
    protected var _ctx :MsoyContext;

    /** The overlay we place on top of our target that contains
     * all the chat glyphs. */
    protected var _overlay :Sprite;

    /** The target container over which we're overlaying chat. TODO. */
    protected var _target :DisplayObjectContainer;

    /** The currently displayed list of subtitles. */
    protected var _subtitles :Array = [];

    /* The shared history used by all overlays. */
    protected static var _history :HistoryList;

    /** Type mode code for default chat type (speaking). */
    protected static const SPEAK :int = 0;

    /** Type mode code for shout chat type. */
    protected static const SHOUT :int = 1;

    /** Type mode code for emote chat type. */
    protected static const EMOTE :int = 2;

    /** Type mode code for think chat type. */
    protected static const THINK :int = 3;

    /** Type place code for default place chat (cluster, scene). */
    protected static const PLACE :int = 1 << 4;

    /** Our internal code for tell chat. */
    protected static const TELL :int = 2 << 4;
    
    /** Our internal code for tell feedback chat. */
    protected static const TELLFEEDBACK :int = 3 << 4;
    
    /** Our internal code for info system messges. */
    protected static const INFO :int = 4 << 4;
    
    /** Our internal code for feedback system messages. */
    protected static const FEEDBACK :int = 5 << 4;

    /** Our internal code for attention system messages. */
    protected static const ATTENTION :int = 6 << 4;

    /** Type place code for broadcast chat type. */
    protected static const BROADCAST :int = 7 << 4;

    /** Type code for a chat type that was used in some special context,
     * like in a negotiation. */
    protected static const SPECIALIZED :int = 8 << 4;

    /** Our internal code for any type of chat that is continued in a
     * subtitle. */
    protected static const CONTINUATION :int = 9 << 4;

    /** Type code for game chat. */
    protected static const GAME :int = 10 << 4;

    /** Our internal code for a chat type we will ignore. */
    protected static const IGNORECHAT :int = -1;

    /** Pixel padding surrounding most things. */
    protected static const PAD :int = 10;

    // used to color chat bubbles
    protected static const BROADCAST_COLOR :uint = 0x990000;
    protected static const FEEDBACK_COLOR :uint = 0x00AA00;
    protected static const TELL_COLOR :uint = 0x0000AA;
    protected static const TELLFEEDBACK_COLOR :uint = 0x00AAAA;
    protected static const INFO_COLOR :uint = 0xAAAA00;
    protected static const ATTENTION_COLOR :uint = 0xFF5000;
    protected static const GAME_COLOR :uint = 0x777777;
    protected static const BLACK :uint = 0x000000;
    protected static const WHITE :uint = 0xFFFFFF;
}
}
