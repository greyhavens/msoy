//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextFormat;

import flash.utils.getTimer; // function import

import mx.events.ResizeEvent;
import mx.events.ScrollEvent;

import mx.core.Container;
import mx.core.IRawChildrenContainer;

import mx.controls.scrollClasses.ScrollBar;
import mx.controls.VScrollBar;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ColorUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.StringUtil;

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

        // NOTE: Any null values in the override formats will use the
        // value from the default, so if a property is added to the default
        // then it should be explicitely negated if not desired in an override.
        _defaultFmt = new TextFormat();
        _defaultFmt.size = 14;
        _defaultFmt.color = 0x006666;
        _defaultFmt.bold = true;
        _defaultFmt.underline = false;

        _userSpeakFmt = new TextFormat();
        _userSpeakFmt.size = 16;
        _userSpeakFmt.color = 0x000000;
        _userSpeakFmt.bold = false;
        _userSpeakFmt.underline = false;

        if (_history == null) {
            _history = new HistoryList();
            _ctx.getChatDirector().addChatDisplay(_history);
        }
    }

    /**
     * Set the target container where this chat should add its overlay.
     */
    public function setTarget (target :Container) :void
    {
        if (_target != null) {
            _ctx.getChatDirector().removeChatDisplay(this);
            _target.removeEventListener("childrenChanged", handleContainerPopulate);
            _target.removeEventListener(ResizeEvent.RESIZE, handleContainerResize);
            _target.rawChildren.removeChild(_overlay);

            _history.removeChatOverlay(this);

            // clear all subtitles, blow away the overlay
            clearGlyphs(_subtitles);
            clearGlyphs(_showingHistory);
            setHistoryEnabled(false);
            _overlay = null;
        }

        _target = target;

        if (_target != null) {
            _overlay = new Sprite();
            _overlay.mouseChildren = false;
            _overlay.mouseEnabled = false;
            _overlay.alpha = ALPHA;
            _overlay.x = 0;
            _overlay.y = 0;
            _target.rawChildren.addChildAt(_overlay,
                Math.max(0, _target.rawChildren.numChildren - 1));
            _target.addEventListener(ResizeEvent.RESIZE, handleContainerResize);
            _target.addEventListener("childrenChanged", handleContainerPopulate);
            _history.addChatOverlay(this);

            _ctx.getChatDirector().addChatDisplay(this);

            setHistoryEnabled(true);

            layout();
        }
    }

    /**
     * Are we currently showing chat history?
     */
    public function isHistoryMode () :Boolean
    {
        return (_historyBar != null);
    }

    // from ChatDisplay
    public function clear () :void
    {
        clearGlyphs(_subtitles);
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
     * Scroll the history up or down the specified number of lines.
     */
    public function scrollHistory (dy :int) :void
    {
        if (_historyBar != null) {
            _historyBar.scrollPosition += dy;
        }
    }

    /**
     * Sets whether or not the glyphs are clickable.
     */
    public function setClickableGlyphs (clickable :Boolean) :void
    {
        _overlay.alpha = clickable ? 1 : ALPHA;
        _overlay.mouseChildren = clickable;
    }

    /**
     * Set the percentage of the bottom of the screen to use for subtitles.
     * TODO: by pixel?
     */
    public function setSubtitlePercentage (perc :Number) :void
    {
        _subtitlePercentage = perc;
    }

    /**
     * Set whether history is enabled or not.
     */
    public function setHistoryEnabled (historyEnabled :Boolean) :void
    {
        if (historyEnabled == (_historyBar != null)) {
            return; // no change
        }

        if (historyEnabled) {
            _historyBar = new VScrollBar();
            _historyBar.addEventListener(ScrollEvent.SCROLL, handleHistoryScroll);
            _historyBar.includeInLayout = false;
            configureHistoryBarSize();
            _target.addChild(_historyBar);
            resetHistoryOffset();

            // out with the subtitles
            clearGlyphs(_subtitles);

            // "scroll" down to the latest history entry
            updateHistBar(_history.size() - 1);

            // figure our history
            figureCurrentHistory();

        } else {
            _target.removeChild(_historyBar);
            _historyBar.removeEventListener(ScrollEvent.SCROLL, handleHistoryScroll);
            _historyBar = null;

            clearGlyphs(_showingHistory);
        }
    }

    /**
     * Layout.
     */
    protected function layout () :void
    {
        clearGlyphs(_subtitles);

        // figure out the height of the subtitles
        _subtitleHeight = (_target.height * _subtitlePercentage);

        // make a guess as to the extent of the history (how many avg
        // sized subtitles will fit in the subtitle area
        _historyExtent = (_subtitleHeight - PAD) / SUBTITLE_HEIGHT_GUESS;

        var msg :ChatMessage;
        var now :int = getTimer();
        var histSize :int = _history.size();
        var index :int = histSize - 1;
        for ( ; index >= 0; index--) {
            msg = _history.get(index);
            _lastExpire = 0;
            if (now > getChatExpire(msg.timestamp, msg.message)) {
                break;
            }
        }

        // now that we've found the message that's one too old, increment
        // the index so that it points to the first message we should display
        index++;
        _lastExpire = 0;

        // now dispatch from that point
        for ( ; index < histSize; index++) {
            msg = _history.get(index);
            if (shouldShowFromHistory(msg, index)) {
                displayMessage(msg, false);
            }
        }

        // reset the history offset
        resetHistoryOffset();

        // finally, if we're in history mode, we should figure that out too
        if (isHistoryMode()) {
            configureHistoryBarSize();
            updateHistBar(histSize - 1);
            figureCurrentHistory();
        }
    }

    /**
     * We're looking through history to figure out which messages we should
     * be showing, should we show the following?
     */
    protected function shouldShowFromHistory (msg :ChatMessage, index :int) :Boolean
    {
        return true; // all for subtitles
    }

    /**
     * Update the history scrollbar with the specified value.
     */
    protected function updateHistBar (val :int) :void
    {
        // we may need to figure out the new history offset amount...
        if (!_histOffsetFinal && (_history.size() > _histOffset)) {
            figureHistoryOffset();
        }

        // then figure out the new value and range
        var oldVal :int = Math.max(_histOffset, val);
        var newMaxVal :int = Math.max(0, _history.size() - 1);
        var newVal :int = (oldVal >= newMaxVal - 1) ? newMaxVal :oldVal;

        // _settingBar protects us from reacting to our own change
        _settingBar = true;
        try {
            _historyBar.setScrollProperties(_historyExtent, _histOffset,
                newMaxVal);
            _historyBar.scrollPosition = newVal;

        } finally {
            _settingBar = false;
        }
    }

    /**
     * Reset the history offset so that it will be recalculated next time
     * it is needed.
     */
    protected function resetHistoryOffset () :void
    {
        _histOffsetFinal = false;
        _histOffset = 0;
    }

    /**
     * Display the specified message now, unless we are to ignore it.
     *
     * @return true if the message was displayed.
     */
    protected function displayMessageNow (msg :ChatMessage) :Boolean
    {
        var type :int = getType(msg, false);
        if (type == IGNORECHAT) {
            return false;
        }

        return displayTypedMessageNow(msg, type);
    }

    /**
     * Display a non-history message now.
     */
    protected function displayTypedMessageNow (
        msg :ChatMessage, type :int) :Boolean
    {
        // if we're in history mode, this will show up in the history
        // and we'll rebuild our subtitle list if and when history goes away
        if (isHistoryMode()) {
            return false;
        }

        addSubtitle(createSubtitle(msg, type, true));
        return true;
    }

    /**
     * Add the specified subtitle glyph for immediate display.
     */
    protected function addSubtitle (glyph :SubtitleGlyph) :void
    {
        var height :int = int(glyph.height);

        glyph.x = PAD;
        glyph.y = getTargetHeight() - height - PAD;
        scrollUpSubtitles(height + getSubtitleSpacing(glyph.getType()));
        _subtitles.push(glyph);
        _overlay.addChild(glyph);
    }

    /**
     * Create a subtitle glyph.
     */
    protected function createSubtitle (
        msg :ChatMessage, type :int, expires :Boolean) :SubtitleGlyph
    {
        var texts :Array = formatMessage(msg, type, true);
        var lifetime :int = getLifetime(msg, expires);
        return new SubtitleGlyph(this, type, lifetime, _defaultFmt, texts);
    }

    /**
     * Return an array of Strings and TextFormats for creating a ChatGlyph.
     */
    protected function formatMessage (
        msg :ChatMessage, type :int, forceSpeaker :Boolean) :Array
    {
        // first parse the message text into plain and links
        var texts :Array = parseLinks(msg.message);

        // possibly insert the formatting
        if (forceSpeaker || alwaysUseSpeaker(type)) {
            var format :String = msg.getFormat();
            if (format != null) {
                var umsg :UserMessage = (msg as UserMessage);
                var prefix :String = _ctx.xlate(null, format,
                    umsg.getSpeakerDisplayName()) + " ";

                if (useQuotes(type)) {
                    prefix += "\"";
                    texts.push("\"");
                }
                texts.unshift(prefix);
            }
        }

        return texts;
    }

    /**
     * Return an array of text strings, with any string needing
     * special formatting preceeded by that format.
     */
    protected function parseLinks (text :String) :Array
    {
        // parse the text into an array with urls at odd elements
        var array :Array = StringUtil.parseURLs(text);

        // insert the appropriate format before each element
        for (var ii :int = array.length - 1; ii >= 0; ii--) {
            if (ii % 2 == 0) {
                // normal text at even-numbered elements...
                array.splice(ii, 0, _userSpeakFmt);
            } else {
                // links at the odd indexes
                array.splice(ii, 0, createLinkFormat(String(array[ii])));
            }
        }
        return array;
    }

    /**
     * Create a link format for the specified link text.
     */
    protected function createLinkFormat (url :String) :TextFormat
    {
        var fmt :TextFormat = new TextFormat();
        fmt.size = 18;
        fmt.underline = true;
        fmt.color = 0xFF0000;
        fmt.bold = false;
        fmt.url = "event:" + url;
        return fmt;
    }

    /**
     * Get the lifetime, in milliseconds, of the specified chat message.
     */
    protected function getLifetime (msg :ChatMessage, expires :Boolean) :int
    {
        if (expires) {
            return getChatExpire(msg.timestamp, msg.message) - msg.timestamp;
        }
        return int.MAX_VALUE;
    }

    /**
     * Get the expire time for the specified chat.
     */
    protected function getChatExpire (stamp :int, text :String) :int
    {
        var durations :Array =
            (DISPLAY_DURATION_PARAMS[getDisplayDurationIndex()] as Array);

        // start the computation from the maximum of the timestamp
        // or our last expire time.
        var start :int = Math.max(stamp, _lastExpire);

        // set the next expire to a time proportional to the text length.
        _lastExpire = start + Math.min(text.length * int(durations[0]),
                                       int(durations[2]));

        // but don't let it be longer than the maximum display time.
        _lastExpire = Math.min(stamp + int(durations[2]), _lastExpire);

        // and be sure to pop up the returned time so that it is above the min.
        return Math.max(stamp + int(durations[1]), _lastExpire);
    }

    /**
     * Should we be using quotes with the specified format?
     */
    protected function useQuotes (type :int) :Boolean
    {
        return (modeOf(type) != EMOTE);
    }

    /**
     * Should we force the use of the speaker in the formatting of
     * the message?
     */
    protected function alwaysUseSpeaker (type :int) :Boolean
    {
        return (modeOf(type) == EMOTE);
    }

    /**
     * Get the outline color for the specified chat type.
     */
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

    /**
     * Used by ChatGlyphs to draw the shape on their Graphics.
     */
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
        if (adjustment != 0) {
            for each (var glyph :SubtitleGlyph in _showingHistory) {
                glyph.histIndex -= adjustment;
            }
            // some history entries were deleted, we need to re-figure the
            // history scrollbar action
            resetHistoryOffset();
        }

        if (_target != null && isHistoryMode()) {
            var val :int = _historyBar.scrollPosition;
            updateHistBar(val - adjustment);

            // only refigure if needed
            if ((val != _historyBar.scrollPosition) || (adjustment != 0) ||
                    !_histOffsetFinal) {
                figureCurrentHistory();
            }
        }
    }

    /**
     * Callback from a ChatGlyph when it wants to be removed.
     */
    internal function glyphExpired (glyph :ChatGlyph) :void
    {
        ArrayUtil.removeFirst(_subtitles, glyph);
        // the glyph may have already been removed, but still expire
        // TODO: possibly fix that, so that a removed glyph is 
        if (_overlay != null && glyph.parent == _overlay) {
            removeGlyph(glyph);
        }
    }

    /**
     * Remove a glyph from the overlay.
     */
    protected function removeGlyph (glyph :ChatGlyph) :void
    {
        _overlay.removeChild(glyph);
        glyph.wasRemoved();
    }

    /**
     * Convert the message class/localtype/mode into our internal type code.
     */
    protected function getType (msg :ChatMessage, history :Boolean) :int
    {
        var localtype :String = msg.localtype;

        if (msg is TellFeedbackMessage) {
            return (history || isApprovedLocalType(localtype)) ? TELLFEEDBACK
                                                               : IGNORECHAT;
        } else if (msg is UserMessage) {
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

    /**
     * Get the spacing above the specified subtitle type.
     */
    protected function getSubtitleSpacing (type :int) :int
    {
        switch (placeOf(type)) {
        default:
            return 1;
        }
    }

    /**
     * Get the spacing for the specified type in history.
     */
    protected function getHistorySubtitleSpacing (index :int) :int
    {
        var msg :ChatMessage = _history.get(index);
        return getSubtitleSpacing(getType(msg, true));
    }

    /**
     * Scroll up all the subtitles by the specified amount.
     */
    protected function scrollUpSubtitles (dy :int) :void
    {
        var minY :int = getTargetHeight() - _subtitleHeight;
        for (var ii :int = 0; ii < _subtitles.length; ii++) {
            var glyph :ChatGlyph = (_subtitles[ii] as ChatGlyph);
            var newY :int = int(glyph.y) - dy;
            if (newY <= minY) {
                _subtitles.splice(ii, 1);
                ii--;
                removeGlyph(glyph);

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

    /**
     * Get the display duration parameters.
     */
    protected function getDisplayDurationIndex () :int
    {
        // by default we add one, because it's assumed that we're in
        // subtitle-only view.
        // TODO
        // return Prefs.getChatDecay() + 1;
        return 1;
    }

    /**
     * Remove all the glyphs in the specified list.
     */
    protected function clearGlyphs (glyphs :Array) :void
    {
        if (_overlay != null) {
            for each (var glyph :ChatGlyph in glyphs) {
                removeGlyph(glyph);
            }
        }

        glyphs.length = 0; // array truncation
    }

    /**
     * React to the scrollbar being changed.
     */
    protected function handleHistoryScroll (event :ScrollEvent) :void
    {
        if (!_settingBar) {
            figureCurrentHistory();
        }
    }

    /**
     * Handle a resize on the container hosting the overlay.
     */
    protected function handleContainerResize (event :ResizeEvent) :void
    {
        layout();
    }

    /**
     * React to child changes in the container, ensure the overlay
     * is the last thing visible.
     */
    protected function handleContainerPopulate (event :Event) :void
    {
        if (!_popping) {
            // goddamn flash can't keep a child at a location anymore
            popOverlayToFront();
        }
    }

    /**
     * Configure the history scrollbar size.
     */
    protected function configureHistoryBarSize () :void
    {
        _historyBar.height = _subtitleHeight;
        _historyBar.move(
            _target.width - ScrollBar.THICKNESS, //_historyBar.width;
            _target.height - _subtitleHeight);
    }

    /**
     * Ensure that the overlay is the top-level component in the container.
     */
    protected function popOverlayToFront () :void
    {
        _popping = true;
        try {
            _target.rawChildren.setChildIndex(
                _overlay, _target.rawChildren.numChildren - 1);

        } finally {
            _popping = false;
        }
    }

    /**
     * Figure out how many of the first history elements fit in our bounds
     * such that we can set the bounds on the scrollbar correctly such
     * that the scrolling to the smallest value just barely puts the first
     * element onscreen.
     */
    protected function figureHistoryOffset () :void
    {
        if (_target == null) {
            return;
        }

        var hsize :int = _history.size();
        var targHeight :int = getTargetHeight();
        var ypos :int = targHeight - PAD;
        var min :int = (targHeight - _subtitleHeight);
        for (var ii :int = 0; ii < hsize; ii++) {
            var glyph :ChatGlyph = getHistorySubtitle(ii);
            ypos -= int(glyph.height);

            // oop, we passed it, it was the last one
            if (ypos <= min) {
                _histOffset = Math.max(0, ii - 1);
                _histOffsetFinal = true;
                return;
            }

            ypos -= getHistorySubtitleSpacing(ii);
        }

        // basically, this means there isn't yet enough history to fill
        // the first 'page' of the history scrollback
        // so we set the offset to the max value but do not set
        // histOffsetFinal to be true so that this will be recalculated
        _histOffset = hsize - 1;
    }

    /**
     * Figure out which ChatMessages in the history should currently appear
     * in the showing history.
     */
    protected function figureCurrentHistory () :void
    {
        var first :int = _historyBar.scrollPosition;
        var count :int = 0;
        var glyph :SubtitleGlyph;
        var ii :int;

        if (_history.size() > 0) {
            // start from the bottom...
            var targHeight :int = getTargetHeight();
            var ypos :int = targHeight - PAD;
            var min :int = (targHeight - _subtitleHeight);
            for (ii = first; ii >= 0; ii--, count++) {
                glyph = getHistorySubtitle(ii);

                // see if it will fit
                ypos -= int(glyph.height);
                if ((count != 0) && ypos <= min) {
                    break; // don't add that one
                }

                // position it
                glyph.x = PAD;
                glyph.y = ypos;
                ypos -= getHistorySubtitleSpacing(ii);
            }
        }

        // finally, because we've been adding to the _showingHistory here
        // we need to prune out the ChatGlyphs that aren't actually needed
        // and make sure the ones that are are positioned on the screen correctly
        for (ii = _showingHistory.length - 1; ii >= 0; ii--) {
            glyph = (_showingHistory[ii] as SubtitleGlyph);
            var managed :Boolean = (_overlay != null) && _overlay.contains(glyph);
            if (glyph.histIndex <= first && glyph.histIndex > (first - count)) {
                // it should be showing
                if (!managed) {
                    _overlay.addChild(glyph);
                }
            } else {
                // it shouldn't be showing
                if (managed) {
                    removeGlyph(glyph);
                }
                _showingHistory.splice(ii, 1);
            }
        }
    }

    /**
     * Get the subtitle for the specified history index, creating if
     * necessary.
     */
    protected function getHistorySubtitle (index :int) :SubtitleGlyph
    {
        var glyph :SubtitleGlyph;

        // do a brute search (over a small set) for an already-created glyph
        for each (glyph in _showingHistory) {
            if (glyph.histIndex == index) {
                return glyph;
            }
        }

        // it looks like we've got to create a new one
        glyph = createHistorySubtitle(index);
        glyph.histIndex = index;
        _showingHistory.push(glyph);
        return glyph;
    }

    /**
     * Create a new subtitle for use in history.
     */
    protected function createHistorySubtitle (index :int) :SubtitleGlyph
    {
        var msg :ChatMessage = _history.get(index);
        var type :int = getType(msg, true);
        return createSubtitle(msg, type, false);
    }

    internal function getTargetHeight () :int
    {
        return _target.height;
    }

    internal function getTargetWidth () :int
    {
        var w :int = _target.width;
        if (_historyBar != null) {
            w -= _historyBar.width;
        }
        w -= (PAD * 2);
        return w;
    }

    /** The light of our life. */
    protected var _ctx :MsoyContext;

    /** The overlay we place on top of our target that contains
     * all the chat glyphs. */
    protected var _overlay :Sprite;

    /** The target container over which we're overlaying chat. */
    protected var _target :Container;

    /** The currently displayed list of subtitles. */
    protected var _subtitles :Array = [];

    /** The currently displayed subtitles in history mode. */
    protected var _showingHistory :Array = [];

    /** The height of the subtitle area, without any padding. */
    protected var _subtitleHeight :int = SUBTITLE_HEIGHT_GUESS * 5;

    /** The percent of the bottom of the screen to use for subtitles. */
    protected var _subtitlePercentage :Number = .4;

    /** The history offset (from 0) such that the history lines
     * (0, _histOffset - 1) will all fit onscreen if the lowest scrollbar
     * positon is _histOffset. */
    protected var _histOffset :int = 0;

    /** True if the histOffset does need to be recalculated. */
    protected var _histOffsetFinal :Boolean = false;

    /** A guess of how many history lines fit onscreen at a time. */
    protected var _historyExtent :int;

    /** The unbounded expire time of the last chat glyph displayed. */
    protected var _lastExpire :int;

    /** The default text format to be applied to subtitles. */
    protected var _defaultFmt :TextFormat;

    /** The format for user-entered text. */
    protected var _userSpeakFmt :TextFormat;

    /** The history scrollbar. */
    protected var _historyBar :VScrollBar;

    /** True while we're setting the position on the scrollbar, so that we
     * know to ignore the event. */
    protected var _settingBar :Boolean = false;

    /** True while popping the overlay to the front. */
    protected var _popping :Boolean = false;

    /** Used to guess at the 'page size' for the scrollbar. */
    protected static const SUBTITLE_HEIGHT_GUESS :int = 26;

    /* The shared history used by all overlays. */
    protected static var _history :HistoryList;

    /**
     * Times to display chat.
     * { (time per character), (min time), (max time) }
     *
     * Groups 0/1/2 are short/medium/long for chat bubbles,
     * and groups 1/2/3 are short/medium/long for subtitles.
     */
    protected static const DISPLAY_DURATION_PARAMS :Array = [
        [ 125, 10000, 30000 ],
        [ 200, 15000, 40000 ],
        [ 275, 20000, 50000 ],
        [ 350, 25000, 60000 ]
    ];

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
    public static const PAD :int = 10;

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

    /** The normal alpha value for bubbles on the overlay. */
    protected static const ALPHA :Number = .8;
}
}
