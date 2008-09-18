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
import mx.core.UIComponent;

import mx.controls.scrollClasses.ScrollBar;
import mx.controls.VScrollBar;

import com.whirled.game.data.WhirledGameCodes;

import com.whirled.ui.PlayerList;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.ExpiringSet;
import com.threerings.util.HashMap;
import com.threerings.util.Log;
import com.threerings.util.MessageManager;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.flash.ColorUtil;

import com.threerings.whirled.spot.data.SpotCodes;

import com.threerings.msoy.utils.TextUtil;

import com.threerings.msoy.chat.data.MsoyChatChannel;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.game.client.MsoyGamePanel;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.world.client.WorldContext;

public class ChatOverlay
    implements TabbedChatDisplay
{
    public static const SCROLL_BAR_LEFT :int = 1;
    public static const SCROLL_BAR_RIGHT :int = 2;

    public static const DEFAULT_WIDTH :int = 300;
    public static const DEFAULT_HEIGHT :int = 500;

    /** Pixel padding surrounding most things. */
    public static const PAD :int = 10;

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

    public function ChatOverlay (ctx :MsoyContext, target :LayeredContainer, 
        scrollBarSide :int = SCROLL_BAR_LEFT, includeOccupantList :Boolean = true)
    {
        _ctx = ctx;
        _msgMan = _ctx.getMessageManager();
        _includeOccList = includeOccupantList;
        _scrollBarSide = scrollBarSide;

        createStandardFormats();

        // overlay for history chat that may get pulled out and put on the side in slide chat mode
        _historyOverlay.mouseEnabled = false;
        _historyOverlay.blendMode = BlendMode.LAYER;

        _target = target;
        layout();
        displayChat(true);

        _closedTabs = new ExpiringSet(LOCALTYPE_EXPIRE_TIME);
        _closedTabs.addEventListener(ExpiringSet.ELEMENT_EXPIRED, localtypeExpired);

        // listen for preferences changes, update history mode
        Prefs.config.addEventListener(ConfigValueSetEvent.CONFIG_VALUE_SET,
            handlePrefsUpdated, false, 0, true);
    }

    /**
     * Sets whether or not we're suppressing the any normal desire to jump to sidebar mode.
     */
    public function setSuppressSidebar (suppress :Boolean) :void
    {
        _suppressSidebar = suppress;
        putInSidebar(shouldUseSidebar());
    }

    // from ChatDisplay
    public function clear () :void
    {
        clearGlyphs(_subtitles);
        clearGlyphs(_showingHistory);
        _lastExpire = 0;
        _filteredMessages = [];
    }

    // from ChatDisplay
    public function displayMessage (msg :ChatMessage, alreadyDisp :Boolean) :Boolean
    {
        if (shouldDisplayMessage(msg)) {
            if (isHistoryMode()) { 
                _filteredMessages.push(msg);
                var val :int = _historyBar.scrollPosition;
                updateHistoryBar();
                if (val != _historyBar.scrollPosition || !_histOffsetFinal) {
                    showCurrentHistory();
                }
            } else {
                addSubtitle(createSubtitle(msg, getType(msg, false), true));
            }
            return true;
        }

        // if a member/jabber tell tab gets popped open behind the scenes, pretend that we just
        // saw it so that the messages will get displayed in subtitle mode when we switch to it.
        var channelType :int = MsoyChatChannel.typeOf(msg.localtype);
        if ((channelType == MsoyChatChannel.MEMBER_CHANNEL || 
             channelType == MsoyChatChannel.JABBER_CHANNEL) &&
            !_localtypeDisplayTimes.containsKey(msg.localtype)) {
            _localtypeDisplayTimes.put(msg.localtype, getTimer());
        }

        return false;
    }

    // from TabbedChatDisplay
    public function tabClosed (localtype :String) :void
    {
        _closedTabs.add(localtype);
    }

    public function displayChat (display :Boolean) :void
    {
        if (_ctx.getTopPanel() == null) {
            // we're not really ready yet...
            return;
        }

        if (display) {
            if (!_target.containsOverlay(_historyOverlay)) {
                _target.addOverlay(_historyOverlay, PlaceBox.LAYER_CHAT_HISTORY);
            }
            if (_historyBar != null && !_target.contains(_historyBar)) {
                _target.addChild(_historyBar);
            }

            putInSidebar(shouldUseSidebar());
            setOccupantListShowing(Prefs.getShowingOccupantList());

        } else {
            putInSidebar(false);
            setOccupantListShowing(false);

            if (_target.containsOverlay(_historyOverlay)) {
                _target.removeOverlay(_historyOverlay);
            } 
            if (_historyBar != null && _target.contains(_historyBar)) {
                _target.removeChild(_historyBar);
            }
        }
    }

    /**
     * @return true if there are clickable glyphs under the specified point.
     */
    public function hasClickableGlyphsAtPoint (stageX :Number, stageY :Number) :Boolean
    {
        var stagePoint :Point = new Point(stageX, stageY);
        for each (var overlay :Sprite in getOverlays()) {
            if (overlay == null) {
                continue;
            }

            // NOTE: The docs swear up and down that the point needs to be in stage coords,
            // but only local coords seem to work. Bug?
            var objs :Array = overlay.getObjectsUnderPoint(overlay.globalToLocal(stagePoint));
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

    public function setLocalType (localtype :String) :void
    {
        // avoid fooling around if we didn't change our localtype, except for place chat where we
        // need to refresh things when we switch rooms which doesn't change the localtype
        if (_localtype == localtype && localtype != ChatCodes.PLACE_CHAT_TYPE) {
            return;
        }

        if (_localtype != null) {
            // note when we stopped looking at the given localtype
            _localtypeDisplayTimes.put(_localtype, getTimer());
        }

        if (_closedTabs.contains(localtype)) {
            _closedTabs.remove(localtype);
        }

        _localtype = localtype;

        // remove old occ list.
        var occListShowing :Boolean = occupantListShowing();
        if (occListShowing) {
            if (_target.containsOverlay(_occupantList)) {
                _target.removeOverlay(_occupantList);
            } else if (_chatContainer != null) {
                _chatContainer.clearOccupantList();
            }
        }
        occListShowing = _occupantList != null ? occListShowing : Prefs.getShowingOccupantList();
        _occupantList = _ctx.getMsoyChatDirector().getPlayerList(localtype);
        setOccupantListShowing(occListShowing);

        if (isHistoryMode()) {
            clearGlyphs(_showingHistory);
            createFilteredMessages();
            resetHistoryOffset();
            updateHistoryBar();
            showCurrentHistory();
        } else {
            _lastExpire = 0;
            clearGlyphs(_subtitles);
            showCurrentSubtitles();
        }
    }

    /**
     * Set the target bounds to the given rectangle.  If targetBounds is null, this ChatOverlay will
     * use the default bounds.
     */
    public function setTargetBounds (targetBounds :Rectangle) :void
    {
        _targetBounds = targetBounds;
        layout();
    }

    /**
     * Sets whether or not the glyphs are clickable.
     */
    public function setClickableGlyphs (clickable :Boolean) :void
    {
        _glyphsClickableAlways = clickable;
    }

    /**
     * Remove a glyph from the overlay.
     */
    public function removeGlyph (glyph :ChatGlyph) :void
    {
        if (glyph.parent != null) {
            glyph.parent.removeChild(glyph);
        }
        glyph.wasRemoved();
    }

    /**
     * Callback from a ChatGlyph when it wants to be removed.
     */
    public function glyphExpired (glyph :ChatGlyph) :void
    {
        ArrayUtil.removeFirst(_subtitles, glyph);
        if (getOverlays().indexOf(glyph.parent) != -1) {
            removeGlyph(glyph);
        }
    }

    public function getTargetTextWidth () :int
    {
        var w :int = _targetBounds.width - ScrollBar.THICKNESS;
        // there is PAD between the text and the edges of the bubble, and another PAD between the
        // bubble and the container edges, on each side for a total of 4 pads.
        w -= (PAD * 4);
        return w;
    }

    /**
     * Used by ChatGlyphs to draw the shape on their Graphics.
     */
    public function drawSubtitleShape (g :Graphics, type :int, width :int, height :int) :int
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
     * Should we put the chat in a sidebar?
     */
    protected function shouldUseSidebar () :Boolean
    {
        return !_suppressSidebar && Prefs.getSidebarChat();
    }

    protected function createFilteredMessages () :void
    {
        var history :HistoryList = _ctx.getMsoyChatDirector().getHistoryList();
        _filteredMessages = [];
        for (var ii :int = 0; ii < history.size(); ii++) {
            var msg :ChatMessage = history.get(ii);
            if (shouldDisplayMessage(msg)) {
                _filteredMessages.push(msg);
            }
        }
    }

    protected function showCurrentHistory () :void
    {
        if (_filteredMessages.length == 0 || _targetBounds == null) {
            return;
        }

        var first :int = _historyBar.scrollPosition;
        var ypos :int = _targetBounds.bottom - PAD;
        var count :int = 0;
        for (var ii :int = first; ii >= 0; ii--, count++) {
            var glyph :SubtitleGlyph = getHistorySubtitle(ii);
            ypos -= int(glyph.height);
            if ((count != 0) && ypos <= getMinHistY()) {
                break;
            }

            glyph.x = _targetBounds.x + PAD +
                (_scrollBarSide == SCROLL_BAR_LEFT || _chatContainer != null ?
                ScrollBar.THICKNESS : 0);
            glyph.y = ypos;
            ypos -= 1;
            glyph.setTransparent(_chatContainer == null && _target is PlaceBox);
        }

        for (ii = _showingHistory.length - 1; ii >= 0; ii--) {
            glyph = _showingHistory[ii] as SubtitleGlyph;
            var managed :Boolean = _historyOverlay.contains(glyph);
            if (glyph.histIndex <= first && glyph.histIndex > (first - count)) {
                if (!managed) {
                    _historyOverlay.addChild(glyph);
                }
            } else {
                if (managed) {
                    removeGlyph(glyph);
                }
                _showingHistory.splice(ii, 1);
            }
        }
    }

    protected function showCurrentSubtitles () :void
    {
        var lastDisplayed :int = int(_localtypeDisplayTimes.get(_localtype));
        if (lastDisplayed == 0) {
            // nothing to show here
            return;
        }

        var history :HistoryList = _ctx.getMsoyChatDirector().getHistoryList();
        var messages :Array = [];
        var glyphs :Array = [];
        var totalHeight :int = 0;
        for (var ii :int = history.size() - 1; ii >= 0; ii--) {
            var msg :ChatMessage = history.get(ii) as ChatMessage;
            if (shouldDisplayMessage(msg)) {
                // let it calculate the expire date from the timestamp
                _lastExpire = 0;
                var expire :int = getChatExpire(msg.timestamp, msg.message);
                if (expire < lastDisplayed) { 
                    break;
                }

                var glyph :SubtitleGlyph = createSubtitle(msg, getType(msg, false), false);
                totalHeight += glyph.height;
                if (totalHeight > _targetBounds.height) {
                    break;
                }
                messages.unshift(msg);
                glyphs.unshift(glyph);
            }
        }

        if (messages.length == 0) {
            return;
        }

        _lastExpire = 0;
        var time :int = getTimer();
        for (ii = 0; ii < messages.length; ii++) {
            msg = messages[ii] as ChatMessage;
            glyph = glyphs[ii] as SubtitleGlyph;
            glyph.setLifetime(getChatExpire(time, msg.message) - time);
            addSubtitle(glyph);
        }
    }

    protected function localtypeExpired (event :ValueEvent) :void
    {
        _localtypeDisplayTimes.remove(event.value as String);
    }

    protected function getOverlays () :Array
    {
        return [ _historyOverlay, _occupantList ];
    }

    protected function handlePrefsUpdated (event :ConfigValueSetEvent) :void
    {
        switch (event.name) {
        case Prefs.CHAT_HISTORY:
            setHistoryEnabled(Boolean(event.value) || shouldUseSidebar());
            break;

        case Prefs.CHAT_SIDEBAR:
            putInSidebar(shouldUseSidebar());
            break;

        case Prefs.OCCUPANT_LIST:
            setOccupantListShowing(Boolean(event.value));
            break;

        case Prefs.CHAT_FONT_SIZE:
            createStandardFormats();
            layout();
            break;
        }
    }

    protected function setHistoryEnabled (historyEnabled :Boolean, 
        forceClear :Boolean = false) :void
    {
        if (!(_target is PlaceBox)) {
            // always show history on a non-PlaceBox
            historyEnabled = true;
        }

        if (!forceClear && historyEnabled == (_historyBar != null)) {
            return;
        }

        layout(false);
        clearGlyphs(_subtitles);
        clearGlyphs(_showingHistory);
        if (historyEnabled) {
            createFilteredMessages();
            resetHistoryOffset();
            updateHistoryBar();
            showCurrentHistory();
        } else {
            _lastExpire = 0;
            _localtypeDisplayTimes.put(_localtype, getTimer());
            showCurrentSubtitles();
        }
    }

    protected function putInSidebar (sidebar :Boolean) :void
    {
        if (!(_target is PlaceBox)) {
            return; // never slide on a non-PlaceBox
        }

        if (sidebar == (_chatContainer != null)) {
            return; // no change
        }

        // ensure the history bar has been set
        layout(false);

        if (sidebar) {
            _target.removeOverlay(_historyOverlay);
            _target.removeChild(_historyBar);
            _ctx.getTopPanel().setLeftPanel(
                _chatContainer = new ChatContainer(_historyBar, _historyOverlay));
            if (_occupantList != null && _target.containsOverlay(_occupantList)) {
                _target.removeOverlay(_occupantList);
                _chatContainer.displayOccupantList(_occupantList);
            }
            setHistoryEnabled(true, true);
            for each (var glyph :ChatGlyph in _showingHistory) {
                glyph.setClickable(true);
            }
        } else {
            for each (glyph in _showingHistory) {
                glyph.setClickable(false);
            }
            _ctx.getTopPanel().clearLeftPanel(_chatContainer);
            if (_chatContainer.containsOccupantList()) {
                _target.addOverlay(_occupantList, PlaceBox.LAYER_CHAT_LIST);
            }
            _chatContainer = null;
            _target.addOverlay(_historyOverlay, PlaceBox.LAYER_CHAT_HISTORY);
            var showingHistory :Boolean = Prefs.getShowingChatHistory() || (!(_target is PlaceBox));
            if (showingHistory) {
                _target.addChild(_historyBar);
            }
            setHistoryEnabled(showingHistory, true);
        }
    }

    protected function setOccupantListShowing (showing :Boolean) :void
    {
        // if we need to ship it off to a game chat container, do that.
        if (!_includeOccList) {
            var rightPanel :UIComponent = _ctx.getTopPanel().getLeftPanel();
            if (rightPanel is GameChatContainer) {
                if (_occupantList != null) {
                    _occupantList.scrollBarOnLeft = (_scrollBarSide == SCROLL_BAR_LEFT);
                }
                (rightPanel as GameChatContainer).displayOccupantList(_occupantList);
            }
            return;
        }

        if (showing == occupantListShowing()) {
            return; // no change
        }

        if (showing && _occupantList == null) {
            // no list to show, and we need to make sure chat history takes up the full height.
            layout(true);
            return;
        }

        _occupantList.scrollBarOnLeft = true;

        if (showing) {
            if (_chatContainer != null) {
                _chatContainer.displayOccupantList(_occupantList);
            } else {
                _target.addOverlay(_occupantList, PlaceBox.LAYER_CHAT_LIST);
            }

        } else {
            if (_chatContainer != null) {
                _chatContainer.clearOccupantList();
            } else if (_target.containsOverlay(_occupantList)) {
                _target.removeOverlay(_occupantList);
            }
        }

        layout(true);
    }

    protected function isHistoryMode () :Boolean
    {
        return (_historyBar != null);
    }

    protected function occupantListShowing () :Boolean
    {
        return _occupantList != null && 
            (_target.containsOverlay(_occupantList) ||
            (_chatContainer != null && _chatContainer.containsOccupantList()));
    }

    protected function getDefaultTargetBounds () :Rectangle
    {
        return new Rectangle(0, 0, DEFAULT_WIDTH + ScrollBar.THICKNESS, DEFAULT_HEIGHT);
    }

    protected function layout (redraw :Boolean = true) :void
    {
        if (_targetBounds == null) {
            _targetBounds = getDefaultTargetBounds();
        } 

        _historyExtent = (_targetBounds.height - PAD) / SUBTITLE_HEIGHT_GUESS;

        if (Prefs.getShowingChatHistory() || shouldUseSidebar() || !(_target is PlaceBox)) {
            if (_historyBar == null) {
                _historyBar = new VScrollBar();
                _historyBar.addEventListener(FlexEvent.UPDATE_COMPLETE, configureHistoryBarSize);
                _historyBar.addEventListener(ScrollEvent.SCROLL, handleHistoryScroll);
                _historyBar.includeInLayout = false;
                _target.addChild(_historyBar);
            }
            configureHistoryBarSize();
            resetHistoryOffset();
            updateHistoryBar();
        } else {
            if (_historyBar != null && _target.contains(_historyBar)) {
                _target.removeChild(_historyBar);
            }
            _historyBar = null;
        }

        if (redraw) {
            clearGlyphs(_subtitles);
            clearGlyphs(_showingHistory);
            _lastExpire = 0;
            _localtypeDisplayTimes.put(_localtype, getTimer());
            if (isHistoryMode()) {
                showCurrentHistory();
            } else {
                showCurrentSubtitles();
            }
        }
    }

    protected function shouldDisplayMessage (msg :ChatMessage) :Boolean
    {
        var type :int = getType(msg, false);
        if (type == IGNORECHAT) {
            return false;
        }

        // if _localtype is null, we're still starting up - anything that shows up at this stage
        // should go ahead and get displayed (startup notifications and the like)
        if (msg.localtype == _localtype || _localtype == null) {
            return true;
        }

        // If we're displaying PLACE_CHAT_TYPE messages, then we need to let through
        // USERGAME_CHAT_TYPE messages as well - that localtype is used as a formatting indicator
        // TODO: this would be cleaner if game user chat and usercode generated info were using
        // the same localtype - this special case would be unneccessary then, as game chat would
        // go to a tab with that localtype specified instead of PLACE_CHAT_TYPE
        if (_localtype == ChatCodes.PLACE_CHAT_TYPE && 
                (msg.localtype == WhirledGameCodes.USERGAME_CHAT_TYPE ||
                 msg.localtype == ChatCodes.USER_CHAT_TYPE)) {
            return true;
        }

        // If we're on the room tab, display any System message that do not have a custom localtype
        if (type == BROADCAST ||
            (msg is SystemMessage && msg.localtype == ChatCodes.PLACE_CHAT_TYPE)) {
            // in WorldContext we pull out the scene and check the id against the current localtype
            if (_ctx is WorldContext) {
                var currentScene :MsoyScene = 
                    (_ctx as WorldContext).getSceneDirector().getScene() as MsoyScene;
                if (currentScene != null && 
                    MsoyChatChannel.typeIsForRoom(_localtype, currentScene.getId())) {
                    return true;
                }

            // in non-WorldContext we just watch for PLACE_CHAT_TYPE
            } else {
                if (_localtype == ChatCodes.PLACE_CHAT_TYPE) {
                    return true;
                }
            }
        }

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
        scrollUpSubtitles(height + 1);
        _subtitles.push(glyph);
        _historyOverlay.addChild(glyph);
    }

    /**
     * Create a subtitle glyph.
     */
    protected function createSubtitle (msg :ChatMessage, type :int, expires :Boolean) :SubtitleGlyph
    {
        var texts :Array = formatMessage(msg, type, true, _userSpeakFmt);
        var lifetime :int = int.MAX_VALUE;
        if (expires) {
            lifetime = getChatExpire(msg.timestamp, msg.message) - msg.timestamp;
        }
        return new SubtitleGlyph(this, type, lifetime, _defaultFmt, texts);
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
        var msg :ChatMessage = _filteredMessages[index] as ChatMessage;
        glyph = createSubtitle(msg, getType(msg, true), false);
        glyph.histIndex = index;
        if (_chatContainer != null) {
            glyph.setClickable(true);
        }
        _showingHistory.push(glyph);
        return glyph;
    }

    /**
     * Return an array of Strings and TextFormats for creating a ChatGlyph.
     */
    protected function formatMessage (
        msg :ChatMessage, type :int, forceSpeaker :Boolean, userSpeakFmt :TextFormat) :Array
    {
        // first parse the message text into plain and links
        var texts :Array = 
            TextUtil.parseLinks(msg.message, userSpeakFmt, shouldParseSpecialLinks(type));

        // possibly insert the formatting
        if (forceSpeaker || alwaysUseSpeaker(type)) {
            var format :String = msg.getFormat();
            if (format != null) {
                var umsg :UserMessage = (msg as UserMessage);
                var prefix :String = _msgMan.getBundle(MsoyCodes.CHAT_MSGS).get(
                    format, umsg.getSpeakerDisplayName()) + " ";

                if (useQuotes(msg, type)) {
                    prefix += "\"";
                    texts.push("\"");
                }
                texts.unshift(prefix);
            }
        }

        return texts;
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
        _lastExpire = Math.min(start + int(durations[2]), _lastExpire);

        // and be sure to pop up the returned time so that it is above the min.  
        return Math.max(stamp + int(durations[1]), _lastExpire);
    }

    /**
     * Should we be using quotes with the specified format?
     */
    protected function useQuotes (msg: ChatMessage, type :int) :Boolean
    {
        return (modeOf(type) != EMOTE) /* TODO: && !(msg is ChannelMessage) */;
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
        case CHANNEL: return CHANNEL_COLOR;
        case GAME: return GAME_COLOR;
        default: return BLACK;
        }
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
        case GAME:
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
     * Convert the message class/localtype/mode into our internal type code.
     */
    protected function getType (msg :ChatMessage, history :Boolean) :int
    {
        var localtype :String = msg.localtype;

        if (msg is TellFeedbackMessage) {
            return (msg as TellFeedbackMessage).isFailure() ? FEEDBACK : TELLFEEDBACK;

        } else if (msg is UserMessage) {
            var type :int;
            var channelType :int = MsoyChatChannel.typeOf(localtype);
            // TODO: jabber messages should probably have their own format
            if (channelType == MsoyChatChannel.MEMBER_CHANNEL || 
                channelType == MsoyChatChannel.JABBER_CHANNEL) {
                type = TELL;
            } else if (channelType != MsoyChatChannel.ROOM_CHANNEL) {
                type = CHANNEL;
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
            switch ((msg as SystemMessage).attentionLevel) {
            case SystemMessage.INFO:
                return msg.localtype == WhirledGameCodes.USERGAME_CHAT_TYPE ? GAME : INFO;
            case SystemMessage.FEEDBACK:
                return FEEDBACK;
            case SystemMessage.ATTENTION:
                return ATTENTION;
            default:
                log.warning("Unknown attention level for system message " + "[msg=" + msg + "].");;
                break;
            }

            // otherwise
            return IGNORECHAT;
        }

        log.warning("Skipping received message of unknown type [msg=" + msg + "].");
        return IGNORECHAT;
    }

    /**
     * Scroll up all the subtitles by the specified amount.
     */
    protected function scrollUpSubtitles (dy :int) :void
    {
        for (var ii :int = 0; ii < _subtitles.length; ii++) {
            var glyph :ChatGlyph = (_subtitles[ii] as ChatGlyph);
            var newY :int = int(glyph.y) - dy;
            if (newY <= getMinHistY()) {
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
        for each (var glyph :ChatGlyph in glyphs) {
            removeGlyph(glyph);
        }

        glyphs.length = 0; // array truncation
    }

    protected function handleHistoryScroll (event :ScrollEvent) :void
    {
        if (!_settingBar) {
            showCurrentHistory();
        }
    }

    protected function configureHistoryBarSize (...ignored) :void
    {
        if (_targetBounds == null || _historyBar == null) {
            return;
        }

        _historyBar.height = _targetBounds.height -
            ((_occupantList != null && _includeOccList && Prefs.getShowingOccupantList()) ?
            _occupantList.height + _occupantList.y : 0);
        if (_scrollBarSide == SCROLL_BAR_LEFT || _chatContainer != null) {
            _historyBar.move(_targetBounds.x + (ScrollBar.THICKNESS / 2), getMinHistY());
        } else {
            _historyBar.move(
                _targetBounds.x + _targetBounds.width - (ScrollBar.THICKNESS / 2) + 1,
                getMinHistY());
        }
    }

    protected function resetHistoryOffset () :void
    {
        _histOffsetFinal = false;
        _histOffset = 0;
        // force scrollbar to the bottom when updateHistoryBar() is called.
        _historyBar.scrollPosition = int.MAX_VALUE;
    }

    protected function updateHistoryBar () :void
    {
        if (_historyBar == null) {
            return;
        }

        if (!_histOffsetFinal && (_filteredMessages.length > _histOffset)) {
            figureHistoryOffset();
        }

        var oldVal :int = Math.max(_historyBar.scrollPosition, _histOffset);
        var newMaxVal :int = Math.max(_filteredMessages.length - 1, 0);
        var newVal :int = (oldVal >= newMaxVal - 1) ? newMaxVal : oldVal;

        // _settingBar protects us from reacting to our own change
        _settingBar = true;
        try {
            _historyBar.setScrollProperties(_historyExtent, _histOffset, newMaxVal);
            _historyBar.scrollPosition = newVal;
        } finally {
            _settingBar = false;
        }
    }

    protected function getMinHistY () :int
    {
        return _targetBounds.y +
            ((_occupantList != null && _includeOccList && Prefs.getShowingOccupantList()) ? 
              _occupantList.y + _occupantList.height : 0);
    }

    /**
     * Figure out how many of hte first history elements fit in our bounds such that we can set the
     * bounds on the scrollbar correctly such that the scrolling to the smallest value just barely
     * puts the first element onscreen.
     */
    protected function figureHistoryOffset () :void
    {
        if (_targetBounds == null) {
            return;
        }

        var hsize :int = _filteredMessages.length;
        var ypos :int = _targetBounds.bottom - PAD;
        for (var ii :int = 0; ii < hsize; ii++) {
            var glyph :ChatGlyph = getHistorySubtitle(ii);
            ypos -= int(glyph.height);

            if (ypos <= getMinHistY()) {
                _histOffset = Math.max(0, ii - 1);
                _histOffsetFinal = true;
                return;
            }

            ypos -= 1; // spacing
        }

        // basically, this means there isn't yet enough history to fill the first 'page' of the
        // history scrollback, so we set hte offset to the max value but do not set histOffsetFinal
        // to be true so that this will be recalculated
        _histOffset = hsize - 1;
    }

    private static const log :Log = Log.getLog(ChatOverlay);

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

    /** Our internal code for channel chat. This is currently unused, as all channel chat is 
     * associated with a place.  If we have private, non-place channels in the future, this will
     * be used again. */
    protected static const CHANNEL :int = 12 << 4;

    /** Our internal code for a chat type we will ignore. */
    protected static const IGNORECHAT :int = -1;

    // used to color chat bubbles
    protected static const BROADCAST_COLOR :uint = 0x990000;
    protected static const FEEDBACK_COLOR :uint = 0x00AA00;
    protected static const TELL_COLOR :uint = 0x0000AA;
    protected static const TELLFEEDBACK_COLOR :uint = 0x00AAAA;
    protected static const INFO_COLOR :uint = 0xAAAA00;
    protected static const ATTENTION_COLOR :uint = 0xFF5000;
    protected static const GAME_COLOR :uint = 0x777777;
    protected static const CHANNEL_COLOR :uint = 0x5500AA;
    protected static const BLACK :uint = 0x000000;
    protected static const WHITE :uint = 0xFFFFFF;

    /** The font for all chat. */
    protected static const FONT :String = "Arial";

    /** Expire localtype display time rememberings after a minute. */
    protected static const LOCALTYPE_EXPIRE_TIME :Number = 60;

    protected var _ctx :MsoyContext;

    /** Contains chat when we're in sidebar mode. */
    protected var _chatContainer :ChatContainer;

    protected var _includeOccList :Boolean;
    protected var _localtype :String;
    protected var _localtypeDisplayTimes :HashMap = new HashMap();
    protected var _closedTabs :ExpiringSet;
    protected var _filteredMessages :Array = [];

    /** If true, the sidebar is being suppressed and we shouldn't show it. */
    protected var _suppressSidebar :Boolean;

    /** Used to translate messages. */
    protected var _msgMan :MessageManager;

    /** The overlay we place on top of our target that contains all the history subtitle chat
     * glyphs. */
    protected var _historyOverlay :Sprite = new Sprite();

    /** The list that contains names and headshots of everyone current subscribed to the currently
     * shown channel */
    protected var _occupantList :PlayerList;

    /** The target container over which we're overlaying chat. */
    protected var _target :LayeredContainer;

    /** The region of our target over which we render. */
    protected var _targetBounds :Rectangle;

    /** The currently displayed list of subtitles. */
    protected var _subtitles :Array = [];

    /** The currently displayed subtitles in history mode. */
    protected var _showingHistory :Array = [];

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

    /** The history scrollbar. */
    protected var _historyBar :VScrollBar;

    /** True while we're setting the position on the scrollbar, so that we
     * know to ignore the event. */
    protected var _settingBar :Boolean = false;

    /** The side to keep the scroll bar for this overlay on. */
    protected var _scrollBarSide :int;

    /** Whether we should always allow the chat glyphs to capture the mouse (for text selection) */
    protected var _glyphsClickableAlways :Boolean = false;

}
}

import flash.display.Sprite;

import mx.controls.scrollClasses.ScrollBar;

import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.flex.FlexWrapper;

import com.whirled.ui.PlayerList;

import com.threerings.util.Log;

import com.threerings.msoy.client.PlaceLayer;
import com.threerings.msoy.client.TopPanel;

import com.threerings.msoy.chat.client.ChatOverlay;

/**
 * Used when we put chat into sidebar mode.
 */
class ChatContainer extends Container
{
    public function ChatContainer (scrollBar :ScrollBar, chat :Sprite) 
    {
        styleName = "chatContainer";
        autoLayout = false;
        width = TopPanel.RIGHT_SIDEBAR_WIDTH;
        horizontalScrollPolicy = ScrollPolicy.OFF
        addChild(scrollBar);
        addChild(new FlexWrapper(chat));
    }

    public function containsOccupantList () :Boolean
    {
        return _occList != null;
    }

    public function displayOccupantList (occList :PlayerList) :void
    {
        if (_occList != null && _occList != occList) {
            removeChild(_occList);
        }

        addChild(_occList = occList);
    }

    public function clearOccupantList () :void
    {
        if (_occList != null && _occList.parent == this) {
            removeChild(_occList);
        }
        _occList = null;
    }

    private static const log :Log = Log.getLog(ChatContainer);

    protected var _occList :PlayerList;
}
