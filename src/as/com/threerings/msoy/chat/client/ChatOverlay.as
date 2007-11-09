//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.InteractiveObject;
import flash.display.Sprite;
import flash.display.Stage;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextFormat;

import flash.utils.getTimer; // function import

import mx.events.FlexEvent;
import mx.events.ResizeEvent;
import mx.events.ScrollEvent;

import mx.core.Container;
import mx.core.IRawChildrenContainer;

import mx.controls.scrollClasses.ScrollBar;
import mx.controls.VScrollBar;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.MessageManager;
import com.threerings.util.StringUtil;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.flash.ColorUtil;

import com.threerings.whirled.spot.data.SpotCodes;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.TimedMessageDisplay;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.notify.data.NotifyMessage;

public class ChatOverlay
    implements ChatDisplay
{
    public static const SCROLL_BAR_LEFT :int = 1;
    public static const SCROLL_BAR_RIGHT :int = 2;

    public var log :Log = Log.getLog(this);

    public function ChatOverlay (msgMan :MessageManager, scrollBarSide :int = SCROLL_BAR_LEFT)
    {
        _msgMan = msgMan;

        _scrollBarSide = scrollBarSide;
        _scrollOverlay = new Sprite();
        _scrollOverlay.mouseEnabled = false;
        _scrollOverlay.blendMode = BlendMode.LAYER;

        _staticOverlay = new Sprite();
        _staticOverlay.mouseEnabled = false;
        _staticOverlay.blendMode = BlendMode.LAYER;

        createStandardFormats();

        // listen for preferences changes, update history mode
        Prefs.config.addEventListener(ConfigValueSetEvent.TYPE, handlePrefsUpdated, false, 0, true);
    }

    /**
     * @return true if there are clickable glyphs under the specified point.
     */
    public function hasClickableGlyphsAtPoint (stageX :Number, stageY :Number) :Boolean
    {
        // NOTE: The docs swear up and down that the point needs to be in stage coords,
        // but only local coords seem to work. Bug?
        var overlays :Array = [_scrollOverlay, _staticOverlay];
        var stagePoint :Point = new Point(stageX, stageY);
        for each (var overlay :Sprite in overlays) {
            var p :Point = overlay.globalToLocal(stagePoint);
            var objs :Array = overlay.getObjectsUnderPoint(p);
            for each (var obj :DisplayObject in objs) {
                // the obj returned when hovering over text is the TextField, not the Chat Glyph
                if (obj.parent is ChatGlyph) {
                    if (_glyphsClickableAlways) {
                        (obj.parent as ChatGlyph).setClickable(true);
                        return true;
                    } else if ((obj.parent as ChatGlyph).isClickableAtPoint(stagePoint)) {
                        return true;
                    }
                } else if (obj is InteractiveObject && InteractiveObject(obj).mouseEnabled) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create the standard chat TextFormat. This is exposed so that other things can
     * show something in the current "chat font".
     */
    public static function createChatFormat () :TextFormat
    {
        var fmt :TextFormat = new TextFormat();
        fmt.font = FONT;
        fmt.size = Prefs.getChatFontSize();
        fmt.color = 0x000000;
        fmt.bold = false;
        fmt.underline = false;
        return fmt;
    }

    /**
     * Are we active with a target?
     */
    public function isActive () :Boolean
    {
        return (_target != null);
    }

    /**
     * Configures this chat overlay with its chat history list.
     */
    public function setHistory (history :HistoryList) :void
    {
        if (history == _history) {
            return;
        }

        if (_history != null) {
            _history.removeChatOverlay(this);
            clearGlyphs(_showingHistory);
            clearGlyphs(_subtitles);
        }

        _history = history;

        if (_history != null) {
            _history.addChatOverlay(this);

            if (isHistoryMode()) {
                configureHistoryBarSize();
                resetHistoryOffset();

                // "scroll" down to the latest history entry
                if (_historyBar != null) {
                    updateHistBar(_history.size() - 1);
    
                    // figure our history
                    figureCurrentHistory();
                }

            } else {
                showCurrentSubtitles();
            }
        }
    }

    /**
     * History list accessor.
     */
    public function getHistory () :HistoryList
    {
        return _history;
    }

    /**
     * Set the target container where this chat should add its overlay. 
     *
     * @param target the container to which a chat overlay should be added; or null to release
     * references and internal resources associated with the previous target.
     * @param targetWidth an optional parameter forcing the target width to the specified value so
     * that message layout will work properly even if the target has not yet been laid out and does
     * not yet have its proper width.
     */
    public function setTarget (target :LayeredContainer, targetBounds :Rectangle = null) :void
    {
        if (_target != null) {
            // removing from the old
            _target.removeOverlay(_scrollOverlay);
            _target.removeOverlay(_staticOverlay);
            _target.removeEventListener(ResizeEvent.RESIZE, handleContainerResize);

            // stop listening to our chat history
            _history.removeChatOverlay(this);

            // clear all subtitles, blow away the overlay
            clearGlyphs(_subtitles);
            clearGlyphs(_showingHistory);
            setHistoryEnabled(false);
        }

        _target = target;

        if (_target != null) {
            // adding to the new
            _scrollOverlay.x = 0;
            _scrollOverlay.y = 0;
            _target.addOverlay(_scrollOverlay, PlaceBox.LAYER_CHAT_SCROLL);

            _staticOverlay.x = 0;
            _staticOverlay.y = 0;
            _target.addOverlay(_staticOverlay, PlaceBox.LAYER_CHAT_STATIC);
            _target.addEventListener(ResizeEvent.RESIZE, handleContainerResize);

            // resume listening to our chat history
            _history.addChatOverlay(this);

            _targetBounds = targetBounds;
            layout();
            setHistoryEnabled(Prefs.getShowingChatHistory());
        }
    }

    /**
     * Scrolls the scrollable glyphs by applying a scroll rect to the sprite that they are on.
     */
    public function setScrollRect (rect :Rectangle) :void
    {
        _scrollOverlay.scrollRect = rect;
    }

    /**
     * Set whether history is enabled or not.
     * TODO: Prefs install defaults.
     */
    public function setHistoryEnabled (historyEnabled :Boolean) :void
    {
        if (historyEnabled == (_historyBar != null)) {
            return; // no change
        }

        if (historyEnabled) {
            _historyBar = new VScrollBar();
            _historyBar.addEventListener(FlexEvent.UPDATE_COMPLETE, configureHistoryBarSize);
            _historyBar.addEventListener(ScrollEvent.SCROLL, handleHistoryScroll);
            _historyBar.includeInLayout = false;
            _target.addChild(_historyBar);
            configureHistoryBarSize();
            _target.addEventListener(Event.ADDED_TO_STAGE, handleTargetAdded);
            _target.addEventListener(Event.REMOVED_FROM_STAGE, handleTargetRemoved);
            handleTargetAdded();
            resetHistoryOffset();

            // out with the subtitles
            clearGlyphs(_subtitles);

            // "scroll" down to the latest history entry
            updateHistBar(_history.size() - 1);

            // figure our history
            figureCurrentHistory();

        } else {
            _target.removeEventListener(Event.ADDED_TO_STAGE, handleTargetAdded);
            _target.removeEventListener(Event.REMOVED_FROM_STAGE, handleTargetRemoved);
            handleTargetRemoved();
            _target.removeChild(_historyBar);
            _historyBar.removeEventListener(ScrollEvent.SCROLL, handleHistoryScroll);
            _historyBar.removeEventListener(FlexEvent.UPDATE_COMPLETE, configureHistoryBarSize);
            _historyBar = null;

            clearGlyphs(_showingHistory);

            showCurrentSubtitles();
        }
    }

    /**
     * Are we currently showing chat history?
     */
    public function isHistoryMode () :Boolean
    {
        return (_historyBar != null);
    }

    /**
     * Sets whether or not the glyphs are clickable.
     */
    public function setClickableGlyphs (clickable :Boolean) :void
    {
        _glyphsClickableAlways = clickable;
    }

    /**
     * Set the percentage of the bottom of the screen to use for subtitles.
     * TODO: by pixel?
     */
    public function setSubtitlePercentage (perc :Number) :void
    {
        if (_subtitlePercentage != perc) {
            _subtitlePercentage = perc;
            if (_target) {
                layout();
            }
        }
    }

    // from ChatDisplay
    public function clear () :void
    {
        clearGlyphs(_subtitles);
    }

    // from ChatDisplay
    public function displayMessage (msg :ChatMessage, alreadyDisp :Boolean) :Boolean
    {
        if (_target == null) {
            return false;
        }

        return displayMessageNow(msg);
    }

    /**
     * Remove a glyph from the overlay.
     */
    public function removeGlyph (glyph :ChatGlyph) :void
    {
        if (glyph.parent == _scrollOverlay) {
            _scrollOverlay.removeChild(glyph);
        } else if (glyph.parent == _staticOverlay) {
            _staticOverlay.removeChild(glyph);
        }
        glyph.wasRemoved();
    }

    protected function showCurrentSubtitles () :void
    {
        if (_target == null) {
            return;
        }

        clearGlyphs(_subtitles);
        _subtitles = [];
        if (_history.size() == 0) {
            return;
        }

        var ii :int = _history.size() - 1;
        for (; ii >= 0 && shouldShowSubtitleNow(_history.get(ii)); ii--);
        ii++;

        var timed :TimedMessageDisplay;
        var expire :int;
        var type :int;
        var texts :Array;
        _lastExpire = 0;
        for (; ii < _history.size(); ii++) {
            timed = _history.get(ii);
            if (timed.displayedAt == -1) {
                timed.showingNow();
            }
            expire = getChatExpire(timed.displayedAt, timed.msg.message);
            type = getType(timed.msg, true);
            texts = formatMessage(timed.msg, type, true, _userSpeakFmt);
            addSubtitle(new SubtitleGlyph(this, type, expire, _defaultFmt, texts));
        }
    }

    protected function shouldShowSubtitleNow (timed :TimedMessageDisplay) :Boolean
    {
        if (timed.displayedAt == -1) {
            return true;
        } else {
            return getChatExpire(timed.displayedAt, timed.msg.message) > getTimer();
        }
    }

    protected function handlePrefsUpdated (event :ConfigValueSetEvent) :void
    {
        switch (event.name) {
        case Prefs.CHAT_HISTORY:
            if (_target != null) {
                setHistoryEnabled(Boolean(event.value));
            }
            break;

        case Prefs.CHAT_FONT_SIZE:
            createStandardFormats();
            if (isHistoryMode()) {
                clearGlyphs(_showingHistory);
                figureCurrentHistory();
            }
            if (_target != null) {
                layout();
            }
            break;
        }
    }

    /**
     * Layout.
     */
    protected function layout () :void
    {
        clearGlyphs(_subtitles);

        if (_targetBounds == null) {
            var height :int = _target.height * _subtitlePercentage;
            _targetBounds = new Rectangle(0, 0, DEFAULT_WIDTH + ScrollBar.THICKNESS, height);
        } 
        // make a guess as to the extent of the history (how many avg sized subtitles will fit in
        // the subtitle area
        _historyExtent = (_targetBounds.height - PAD) / SUBTITLE_HEIGHT_GUESS;

        var msg :ChatMessage;
        var now :int = getTimer();
        var histSize :int = _history.size();
        var index :int = histSize - 1;
        for ( ; index >= 0; index--) {
            msg = _history.get(index).msg;
            _lastExpire = 0;
            if (now > getChatExpire(msg.timestamp, msg.message)) {
                break;
            }
        }

        // now that we've found the message that's one too old, increment the index so that it
        // points to the first message we should display
        index++;
        _lastExpire = 0;

        // now dispatch from that point
        var timed :TimedMessageDisplay;
        for ( ; index < histSize; index++) {
            timed = _history.get(index);
            if (shouldShowFromHistory(timed.msg, index)) {
                displayMessage(timed.msg, false);
                timed.showingNow();
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
     * We're looking through history to figure out which messages we should be showing, should we
     * show the following?
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
            _historyBar.setScrollProperties(_historyExtent, _histOffset, newMaxVal);
            _historyBar.scrollPosition = newVal;
        } finally {
            _settingBar = false;
        }
    }

    /**
     * Reset the history offset so that it will be recalculated next time it is needed.
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
    protected function displayTypedMessageNow (msg :ChatMessage, type :int) :Boolean
    {
        // This implementation never displays messages via this method.  All messages are either 
        // shown in the history or in subtitles via HistoryList's historyUpdated() call on this
        // class.
        return false;
    }

    /**
     * Add the specified subtitle glyph for immediate display.
     */
    protected function addSubtitle (glyph :SubtitleGlyph) :void
    {
        var height :int = int(glyph.height);
        glyph.x = _targetBounds.x + PAD;
        glyph.y = _targetBounds.bottom - height - PAD;
        scrollUpSubtitles(height + getSubtitleSpacing(glyph.getType()));
        _subtitles.push(glyph);
        _staticOverlay.addChild(glyph);
    }

    /**
     * Create a subtitle glyph.
     */
    protected function createSubtitle (msg :ChatMessage, type :int, expires :Boolean) :SubtitleGlyph
    {
        var texts :Array = formatMessage(msg, type, true, _userSpeakFmt);
        var lifetime :int = getLifetime(msg, expires);
        return new SubtitleGlyph(this, type, lifetime, _defaultFmt, texts);
    }

    /**
     * Return an array of Strings and TextFormats for creating a ChatGlyph.
     */
    protected function formatMessage (
        msg :ChatMessage, type :int, forceSpeaker :Boolean, userSpeakFmt :TextFormat) :Array
    {
        // first parse the message text into plain and links
        var texts :Array = parseLinks(msg.message, userSpeakFmt, shouldParseSpecialLinks(type));

        // possibly insert the formatting
        if (forceSpeaker || alwaysUseSpeaker(type)) {
            var format :String = msg.getFormat();
            if (format != null) {
                var umsg :UserMessage = (msg as UserMessage);
                var prefix :String = _msgMan.getBundle(MsoyCodes.CHAT_MSGS).get(
                    format, umsg.getSpeakerDisplayName()) + " ";

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
     * Return an array of text strings, with any string needing special formatting preceeded by
     * that format.
     */
    protected function parseLinks (
        text :String, userSpeakFmt :TextFormat, parseSpecial :Boolean) :Array
    {
        // parse the text into an array with urls at odd elements
        var array :Array = StringUtil.parseURLs(text);

        // insert the appropriate format before each element
        for (var ii :int = array.length - 1; ii >= 0; ii--) {
            if (ii % 2 == 0) {
                // normal text at even-numbered elements...
                if (parseSpecial) {
                    var specialBits :Array = parseSpecialLinks(String(array[ii]), userSpeakFmt);
                    specialBits.unshift(ii, 1);
                    array.splice.apply(array, specialBits);

                } else {
                    // just insert the speak format before the text
                    array.splice(ii, 0, userSpeakFmt);
                }

            } else {
                // links at the odd indexes
                array.splice(ii, 0, createLinkFormat(String(array[ii]), userSpeakFmt));
            }
        }

        return array;
    }

    /**
     * Parse any "special links" (in the format "[text|url]") in the specified text.
     *
     * @return an array containing [ format, text, format, text, ... ].
     */
    protected function parseSpecialLinks (text :String, userSpeakFmt :TextFormat) :Array
    {
        var array :Array = [];

        var result :Object;
        do {
            result = _specialLinkRegExp.exec(text);
            if (result != null) {
                var index :int = int(result.index);
                array.push(userSpeakFmt, text.substring(0, index));
                array.push(createLinkFormat(String(result[2]), userSpeakFmt), String(result[1]));

                // and advance the text
                var match :String = String(result[0]);
                text = text.substring(index + match.length);

            } else {
                // it's just left-over text
                array.push(userSpeakFmt, text);
            }

        } while (result != null);

        return array;
    }

    /**
     * (Re)create the standard formats.
     */
    protected function createStandardFormats () :void
    {
        // NOTE: Any null values in the override formats will use the value from the default, so if
        // a property is added to the default then it should be explicitely negated if not desired
        // in an override.
        _defaultFmt = new TextFormat();
        _defaultFmt.font = FONT;
        _defaultFmt.size = Prefs.getChatFontSize();
        _defaultFmt.color = 0x000070;
        _defaultFmt.bold = false;
        _defaultFmt.underline = false;

        _userSpeakFmt = createChatFormat();
    }

    /**
     * Create a link format for the specified link text.
     */
    protected function createLinkFormat (url :String, userSpeakFmt :TextFormat) :TextFormat
    {
        var fmt :TextFormat = new TextFormat();
        fmt.align = userSpeakFmt.align;
        fmt.font = FONT;
        fmt.size = Prefs.getChatFontSize();
        fmt.underline = true;
        fmt.color = 0x0093dd;
        fmt.bold = true;
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
        // load the configured durations
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
        return (modeOf(type) == EMOTE) || (placeOf(type) == BROADCAST);
    }

    /**
     * Should we parse "special links" (in the format "[text|url]") for chat messages
     * of the specified type.
     */
    protected function shouldParseSpecialLinks (type :int) :Boolean
    {
        switch (type) {
        case FEEDBACK:
        case INFO:
        case ATTENTION:
        case NOTIFICATION:
            return true;

        default:
            return false;
        }
    }

    /**
     * Get the outline color for the specified chat type.
     */
    protected function getOutlineColor (type :int) :uint
    {
        // mask out the bits we don't need for determining outline color
        switch (placeOf(type)) {
        case BROADCAST: return BROADCAST_COLOR;
        case TELL: return TELL_COLOR;
        case TELLFEEDBACK: return TELLFEEDBACK_COLOR;
        case INFO: return INFO_COLOR;
        case FEEDBACK: return FEEDBACK_COLOR;
        case ATTENTION: return ATTENTION_COLOR;
        case NOTIFICATION: return NOTIFY_COLOR;
        case CHANNEL: return CHANNEL_COLOR;
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
    internal function drawSubtitleShape (g :Graphics, type :int, width :int, height :int) :int
    {
        var outline :uint = getOutlineColor(type);
        var background :uint;
        if (BLACK == outline) {
            background = WHITE;
        } else {
            background = ColorUtil.blend(WHITE, outline, .8);
        }
        width += PAD * 2;

        var shapeFunction :Function = getSubtitleShape(type);

        // clear any old graphics
        g.clear();

        // fill and outline in the same step
        g.lineStyle(1, outline);
        g.beginFill(background);
        shapeFunction(g, width, height);
        g.endFill();

        return PAD;
    }

    /**
     * Get the function that draws the subtitle shape for the
     * specified type of subtitle.
     */
    protected function getSubtitleShape (type :int) :Function
    {
        switch (placeOf(type)) {
        case PLACE: {
            switch (modeOf(type)) {
            case SPEAK:
            default:
                return drawRoundedSubtitle;

            case EMOTE:
                return drawEmoteSubtitle;

            case THINK:
                return drawThinkSubtitle;
            }
        }

        case FEEDBACK:
            return drawFeedbackSubtitle;

        case BROADCAST:
        case CONTINUATION:
        case INFO:
        case ATTENTION:
        default:
            return drawRectangle;
        }
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawRectangle (g :Graphics, w :int, h :int) :void
    {
        g.drawRect(0, 0, w, h);
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawRoundedSubtitle (g :Graphics, w :int, h :int) :void
    {
        g.drawRoundRect(0, 0, w, h, PAD * 2, PAD * 2);
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawEmoteSubtitle (g :Graphics, w :int, h :int) :void
    {
        g.moveTo(0, 0);
        g.lineTo(w, 0);
        g.curveTo(w - PAD, h / 2, w, h);
        g.lineTo(0, h);
        g.curveTo(PAD, h / 2, 0, 0);
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawThinkSubtitle (g :Graphics, w :int, h :int) :void
    {
        // thinky bubbles on the left and right
        const DIA :int = 8;
        g.moveTo(PAD/2, 0);
        g.lineTo(w - PAD/2, 0);

        var yy :int;
        var ty :int;
        for (yy = 0; yy < h; yy += DIA) {
            ty = Math.min(h, yy + DIA);
            g.curveTo(w, (yy + ty)/2, w - PAD/2, ty);
        }

        g.lineTo(PAD/2, h);
        for (yy = h; yy > 0; yy -= DIA) {
            ty = Math.max(0, yy - DIA);
            g.curveTo(0, (yy + ty)/2, PAD/2, ty);
        }
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawFeedbackSubtitle (g :Graphics, w :int, h :int) :void
    {
        g.moveTo(PAD / 2, 0);
        g.lineTo(w, 0);
        g.lineTo(w - PAD / 2, h);
        g.lineTo(0, h);
        g.lineTo(PAD / 2, 0);
    }

    /**
     * Called from the HistoryList to notify us that messages were added to the history.
     *
     * @param adjustment if non-zero, the number of old history entries that were pruned.
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

        if (_target != null) {
            if (isHistoryMode()) {
                var val :int = _historyBar.scrollPosition;
                updateHistBar(val - adjustment);

                // only refigure if needed
                if ((val != _historyBar.scrollPosition) || (adjustment != 0) || !_histOffsetFinal) {
                    figureCurrentHistory();
                }
            } else {
                var timed :TimedMessageDisplay = _history.get(_history.size() - 1);
                var newGlyph :SubtitleGlyph = 
                    createSubtitle(timed.msg, getType(timed.msg, true), true);
                timed.showingNow();
                addSubtitle(newGlyph);
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
        if (glyph.parent == _scrollOverlay || glyph.parent == _staticOverlay) {
            removeGlyph(glyph);
        }
    }

    /**
     * Convert the message class/localtype/mode into our internal type code.
     */
    protected function getType (msg :ChatMessage, history :Boolean) :int
    {
        var localtype :String = msg.localtype;

        if (msg is TellFeedbackMessage) {
            if (history || isApprovedLocalType(localtype)) {
                return (msg as TellFeedbackMessage).isFailure() ? FEEDBACK : TELLFEEDBACK;
            }
            return IGNORECHAT;

        } else if (msg is UserMessage) {
            var type :int;
            if (msg is ChannelMessage) {
                type = CHANNEL;
            } else if (ChatCodes.USER_CHAT_TYPE == localtype) {
                type = TELL;
            } else {
                type = PLACE;
            }
            // factor in the mode
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

        } else if (msg is NotifyMessage) {
            return NOTIFICATION;
        }

        log.warning("Skipping received message of unknown type [msg=" + msg + "].");
        return IGNORECHAT;
    }

    /**
     * Check to see if we want to display the specified localtype.
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
//        switch (placeOf(type)) {
//        default:
            return 1;
//        }
    }

    /**
     * Get the spacing for the specified type in history.
     */
    protected function getHistorySubtitleSpacing (index :int) :int
    {
        var msg :ChatMessage = _history.get(index).msg;
        return getSubtitleSpacing(getType(msg, true));
    }

    /**
     * Scroll up all the subtitles by the specified amount.
     */
    protected function scrollUpSubtitles (dy :int) :void
    {
        var minY :int = _targetBounds.y;
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
        return Prefs.getChatDecay() + 1;
    }

    /**
     * Remove all the glyphs in the specified list.
     */
    protected function clearGlyphs (glyphs :Array) :void
    {
        if (_scrollOverlay != null && _staticOverlay != null) {
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
     * When we're in history mode, listen for our target being added
     * to the hierarchy.
     */
    protected function handleTargetAdded (... ignored) :void
    {
        if (_target.stage) {
            // we need to listen to the stage for mouse wheel events,
            // otherwise we don't get them over many targets
            _stage = _target.stage;
            _stage.addEventListener(MouseEvent.MOUSE_WHEEL, handleMouseWheel);
        }
    }

    /**
     * When in history mode, listen for our target being removed
     * from the hierarchy.
     */
    protected function handleTargetRemoved (... ignored) :void
    {
        if (_stage) {
            _stage.removeEventListener(MouseEvent.MOUSE_WHEEL, handleMouseWheel);
            _stage = null;
        }
    }

    /**
     * Handle mouse wheel events detected in our target container.
     */
    protected function handleMouseWheel (event :MouseEvent) :void
    {
        var p :Point = new Point(event.stageX, event.stageY);
        p = _target.globalToLocal(p);

        var subtitleY :Number = p.y - _targetBounds.y;
        if (subtitleY >= 0 && subtitleY < _targetBounds.height) {
            // The delta factor is configurable per OS, and so may range from 1-3 or even
            // higher. We normalize this based on observed values so that a single click of the
            // mouse wheel always scrolls one line.
            if (_wheelFactor > Math.abs(event.delta)) {
                _wheelFactor = Math.abs(event.delta);
            }
            var newPos :int = _historyBar.scrollPosition - int(event.delta / _wheelFactor);
            // Note: the scrollPosition setter function will ensure the value is bounded by min/max
            // for setting the position of the thumb, but it does NOT bound the actual underlying
            // value. Thus, the scrollPosition can "go negative" and must climb back out again
            // before the thumb starts to move from 0.  It's retarded, and it means we have to
            // bound the value ourselves.
            newPos = Math.min(_historyBar.maxScrollPosition,
                              Math.max(_historyBar.minScrollPosition, newPos));

            // only update if changed
            if (newPos != int(_historyBar.scrollPosition)) {
                _historyBar.scrollPosition = newPos;
                // Retardedly, as of Flex v2.0.1, setting the scroll position does not dispatch a
                // scroll event, so we must fake it.
                figureCurrentHistory();
            }
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
     * Configure the history scrollbar size and location.
     */
    protected function configureHistoryBarSize (... ignored) :void
    {
        if (_targetBounds != null && _historyBar != null) {
            _historyBar.height = _targetBounds.height;
            if (_scrollBarSide == SCROLL_BAR_LEFT) {
                _historyBar.move(_targetBounds.x, _targetBounds.y);
            } else {
                _historyBar.move(
                    _targetBounds.x + _targetBounds.width - ScrollBar.THICKNESS, _targetBounds.y);
            }
        }
    }

    /**
     * Figure out how many of the first history elements fit in our bounds such that we can set the
     * bounds on the scrollbar correctly such that the scrolling to the smallest value just barely
     * puts the first element onscreen.
     */
    protected function figureHistoryOffset () :void
    {
        if (_target == null || _targetBounds == null) {
            return;
        }

        var hsize :int = _history.size();
        var ypos :int = _targetBounds.bottom - PAD;
        var min :int = _targetBounds.y;
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

        // basically, this means there isn't yet enough history to fill the first 'page' of the
        // history scrollback, so we set the offset to the max value but do not set histOffsetFinal
        // to be true so that this will be recalculated
        _histOffset = hsize - 1;
    }

    /**
     * Figure out which ChatMessages in the history should currently appear in the showing history.
     */
    protected function figureCurrentHistory () :void
    {
        var first :int = _historyBar.scrollPosition;
        var count :int = 0;
        var glyph :SubtitleGlyph;
        var ii :int;

        if (_history.size() > 0) {
            // start from the bottom...
            var ypos :int = _targetBounds.bottom - PAD;
            var min :int = _targetBounds.y;
            for (ii = first; ii >= 0; ii--, count++) {
                glyph = getHistorySubtitle(ii);

                // see if it will fit
                ypos -= int(glyph.height);
                if ((count != 0) && ypos <= min) {
                    break; // don't add that one
                }

                // position it
                glyph.x = _targetBounds.x + PAD + 
                    (_scrollBarSide == SCROLL_BAR_LEFT ? ScrollBar.THICKNESS : 0);
                glyph.y = ypos;
                ypos -= getHistorySubtitleSpacing(ii);
            }
        }

        // finally, because we've been adding to the _showingHistory here we need to prune out the
        // ChatGlyphs that aren't actually needed and make sure the ones that are are positioned on
        // the screen correctly
        for (ii = _showingHistory.length - 1; ii >= 0; ii--) {
            glyph = (_showingHistory[ii] as SubtitleGlyph);
            // only the static overlay contains subtitles
            var managed :Boolean = _staticOverlay.contains(glyph);
            if (glyph.histIndex <= first && glyph.histIndex > (first - count)) {
                // it should be showing
                if (!managed) {
                    _staticOverlay.addChild(glyph);
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
     * Get the subtitle for the specified history index, creating if necessary.
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
        var msg :ChatMessage = _history.get(index).msg;
        return createSubtitle(msg, getType(msg, true), false);
    }

    internal function getTargetTextWidth () :int
    {
        var w :int = _targetBounds.width - ScrollBar.THICKNESS;
        // there is PAD between the text and the edges of the bubble, and another PAD between the
        // bubble and the container edges, on each side for a total of 4 pads.
        w -= (PAD * 4);
        return w;
    }

    /** Used to translate messages. */
    protected var _msgMan :MessageManager;

    /** The overlay we place on top of our target that contains all the chat glyphs that can 
     * scroll. */
    protected var _scrollOverlay :Sprite;

    /** The overlay we place on top of our target that contains all the chat glyphs that should
     * not scroll. */
    protected var _staticOverlay :Sprite;

    /** The target container over which we're overlaying chat. */
    protected var _target :LayeredContainer;

    /** The region of our target over which we render. */
    protected var _targetBounds :Rectangle;

    /** The stage of our target, while tracking mouseWheel in history mode. */
    protected var _stage :Stage;

    /** The currently displayed list of subtitles. */
    protected var _subtitles :Array = [];

    /** The currently displayed subtitles in history mode. */
    protected var _showingHistory :Array = [];

    /** The percent of the bottom of the screen to use for subtitles. */
    protected var _subtitlePercentage :Number = 1;

    /** The history offset (from 0) such that the history lines (0, _histOffset - 1) will all fit
     * onscreen if the lowest scrollbar positon is _histOffset. */
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

    /** Matches "special links", which are in the format "[text|url]". */
    protected var _specialLinkRegExp :RegExp = new RegExp("\\[(.+?)\\|(.+?)\\]");

    /** The history scrollbar. */
    protected var _historyBar :VScrollBar;

    /** The smallest absolute value seen for delta in a mouse wheel event. */
    protected var _wheelFactor :int = int.MAX_VALUE;

    /** True while we're setting the position on the scrollbar, so that we
     * know to ignore the event. */
    protected var _settingBar :Boolean = false;

    /* The history used by this overlay. */
    protected var _history :HistoryList;

    /** The side to keep the scroll bar for this overlay on. */
    protected var _scrollBarSide :int;

    /** Whether we should always allow the chat glyphs to capture the mouse (for text selection) */
    protected var _glyphsClickableAlways :Boolean = false;

    /** Used to guess at the 'page size' for the scrollbar. */
    protected static const SUBTITLE_HEIGHT_GUESS :int = 26;

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

    /** Type code for notifications. */
    protected static const NOTIFICATION :int = 11 << 4;

    /** Our internal code for channel chat. */
    protected static const CHANNEL :int = 12 << 4;

    /** Our internal code for a chat type we will ignore. */
    protected static const IGNORECHAT :int = -1;

    /** Pixel padding surrounding most things. */
    public static const PAD :int = 10;

    /** The default width to use for the chat history */
    protected static const DEFAULT_WIDTH :int = 300;

    // used to color chat bubbles
    protected static const BROADCAST_COLOR :uint = 0x990000;
    protected static const FEEDBACK_COLOR :uint = 0x00AA00;
    protected static const TELL_COLOR :uint = 0x0000AA;
    protected static const TELLFEEDBACK_COLOR :uint = 0x00AAAA;
    protected static const INFO_COLOR :uint = 0xAAAA00;
    protected static const ATTENTION_COLOR :uint = 0xFF5000;
    protected static const GAME_COLOR :uint = 0x777777;
    protected static const NOTIFY_COLOR :uint = 0x008A83;
    protected static const CHANNEL_COLOR :uint = 0x5500AA;
    protected static const BLACK :uint = 0x222222; // same black as other Whirled UI bits
    protected static const WHITE :uint = 0xFFFFFF;

    /** The font for all chat. */
    protected static const FONT :String = "Arial";

    /** The normal alpha value for bubbles on the overlay. */
    protected static const ALPHA :Number = .8;
}
}
