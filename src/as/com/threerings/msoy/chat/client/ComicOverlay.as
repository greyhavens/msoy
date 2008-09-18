//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.BlendMode;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextFormat;
import flash.text.TextFormatAlign;

import mx.core.Container;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.util.ArrayUtil;
import com.threerings.util.HashMap;
import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.flash.ColorUtil;

import com.threerings.msoy.chat.data.MsoyChatChannel;

import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceBox;

import com.threerings.msoy.data.all.RoomName;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.MsoyScene;

/**
 * Implements comic chat in the metasoy client.
 */
public class ComicOverlay extends ChatOverlay 
{
    /**
     * Construct a comic chat overlay.
     */
    public function ComicOverlay (ctx :MsoyContext, target :LayeredContainer)
    {
        super(ctx, target);

        // overlay for chat that stays in a given place in the scene, and is therefore scrolled
        // with it.
        _scrollOverlay.mouseEnabled = false;
        _scrollOverlay.blendMode = BlendMode.LAYER;

        // overlay for chat that stays in a given place on the screen.
        _staticOverlay.setComicOverlay(this);
        _staticOverlay.mouseEnabled = false;
        _staticOverlay.blendMode = BlendMode.LAYER;
    }

    override public function displayChat (display :Boolean) :void
    {
        super.displayChat(display);

        // this call signals a move to a new room - we want to clear out our glyphs
        for each (var bubble :BubbleGlyph in _allBubbles) {
            removeGlyph(bubble);
        }
        _bubbles = new HashMap();
        _allBubbles = [];

        var overlays :Array = [ _scrollOverlay, _staticOverlay ];
        var layers :Array = [ PlaceBox.LAYER_CHAT_SCROLL, PlaceBox.LAYER_CHAT_STATIC ];
        for (var ii :int = 0; ii < overlays.length; ii++) {
            var contains :Boolean = _target.containsOverlay(overlays[ii]);
            if (display && !contains) {
                _target.addOverlay(overlays[ii], layers[ii]);
            } else if (!display && contains) {
                _target.removeOverlay(overlays[ii]);
            }
        }
    }

    // from ChatDisplay
    override public function clear () :void
    {
        super.clear();
        for each (var bubble :BubbleGlyph in _allBubbles) {
            removeGlyph(bubble);
        }
        _bubbles = new HashMap();
        _allBubbles = [];
    }

    // from ChatDisplay
    override public function displayMessage (msg :ChatMessage, alreadyDisplayed :Boolean) :Boolean
    {
        var displayed :Boolean = false;
        var type :int = getType(msg, false);

        // display all system messages if they don't have a custom localtype.
        if (type == BROADCAST ||
            (msg is SystemMessage && (msg.localtype == ChatCodes.PLACE_CHAT_TYPE || 
                                      msg.localtype == ChatCodes.USER_CHAT_TYPE))) {
            displayed = displayBubble(msg, type);
        } else if (_ctx is WorldContext) {
            var scene :MsoyScene = 
                (_ctx as WorldContext).getSceneDirector().getScene() as MsoyScene;
            if (scene != null && MsoyChatChannel.typeIsForRoom(msg.localtype, scene.getId())) {
                if (type != IGNORECHAT) {
                    displayed = displayBubble(msg, type);
                }
            }
        } 

        return super.displayMessage(msg, alreadyDisplayed) || displayed;
    }

    public function willEnterPlace (provider :ChatInfoProvider) :void
    {
        _provider = provider;
    }

    public function didLeavePlace (provider :ChatInfoProvider) :void
    {
        if (_provider == provider) {
            _provider = null;
        }
        // else we've already received the ChatInfoProvider for the new place
    }

    public function speakerMoved (speaker :Name, pos :Point) :void
    {
        var cloud :BubbleCloud = _bubbles.get(speaker);
        if (cloud != null) {
            // the position is in stage coordinates
            cloud.setSpeakerLocation(_scrollOverlay.globalToLocal(pos));
        }
    }

    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        for each (var cloud :BubbleCloud in _bubbles.values()) {
            cloud.viewWidth = unscaledWidth;
        }
    }

    /**
     * Scrolls the scrollable glyphs by applying a scroll rect to the sprite that they are on.
     */
    public function setScrollRect (rect :Rectangle) :void
    {
        _scrollOverlay.scrollRect = rect;
    }

    override public function glyphExpired (glyph :ChatGlyph) :void
    {
        if (glyph is BubbleGlyph) {
            var bubble :BubbleGlyph = glyph as BubbleGlyph;
            var cloud :BubbleCloud = _bubbles.get(bubble.getSpeaker());
            if (cloud != null) {
                cloud.removeBubble(bubble);
                ArrayUtil.removeFirst(_allBubbles, bubble);
            }
        }
        super.glyphExpired(glyph);
    }

    /**
     * Draw the specified bubble shape.
     *
     * @return the padding that should be applied to the bubble's label.
     */
    public function drawBubbleShape (g :Graphics, type :int, txtWidth :int, txtHeight :int,
        tail :Boolean) :int
    {
        var outline :uint = getOutlineColor(type);
        var background :uint;
        if (BLACK == outline) {
            background = WHITE;
        } else {
            background = ColorUtil.blend(WHITE, outline, .8);
        }

        var padding :int = getBubbleLabelOffset(type);
        var width :int = txtWidth + padding * 2;
        var height :int = txtHeight + padding * 2;

        var shapeFunction :Function = getBubbleShape(type);

        // clear any old graphics
        g.clear();

        g.lineStyle(1, outline);
        g.beginFill(background);
        shapeFunction(g, width, height);
        g.endFill();

        if (tail) {
            var tailFunction :Function = getTailShape(type);
            if (tailFunction != null) {
                tailFunction(g, width, height, outline, background);
            }
        }

        return padding;
    }

    override protected function getOverlays () :Array
    {
        return super.getOverlays().concat(_scrollOverlay, _staticOverlay);
    }

    /**
     * Get the expire time for the specified chat.
     */
    protected function getBubbleExpire (stamp :int, text :String) :int
    {
        // load the configured durations
        var durations :Array =
            (DISPLAY_DURATION_PARAMS[getDisplayDurationIndex()] as Array);

        // start the computation from the maximum of the timestamp
        // or our last expire time.
        var start :int = Math.max(stamp, _lastBubbleExpire);

        // set the next expire to a time proportional to the text length.
        _lastBubbleExpire = start + Math.min(text.length * int(durations[0]),
                                       int(durations[2]));

        // but don't let it be longer than the maximum display time.
        _lastBubbleExpire = Math.min(stamp + int(durations[2]), _lastBubbleExpire);

        // and be sure to pop up the returned time so that it is above the min.
        return Math.max(stamp + int(durations[1]), _lastBubbleExpire);
    }

    override protected function createStandardFormats () :void
    {
        super.createStandardFormats();

        // Bubbles use copies of the standard subtitle formats, only with align = CENTER.
        _defaultBubbleFmt = new TextFormat(_defaultFmt.font, _defaultFmt.size,
            _defaultFmt.color, _defaultFmt.bold, _defaultFmt.italic, _defaultFmt.underline,
            _defaultFmt.url, _defaultFmt.target, TextFormatAlign.CENTER,
            _defaultFmt.leftMargin, _defaultFmt.rightMargin, _defaultFmt.indent,
            _defaultFmt.leading);
        _userBubbleFmt = new TextFormat(_userSpeakFmt.font, _userSpeakFmt.size,
            _userSpeakFmt.color, _userSpeakFmt.bold, _userSpeakFmt.italic, _userSpeakFmt.underline,
            _userSpeakFmt.url, _userSpeakFmt.target, TextFormatAlign.CENTER,
            _userSpeakFmt.leftMargin, _userSpeakFmt.rightMargin, _userSpeakFmt.indent,
            _userSpeakFmt.leading);
    }

    protected function displayBubble (msg :ChatMessage, type :int) :Boolean
    {
        switch (placeOf(type)) {
        case INFO:
        case FEEDBACK:
        case ATTENTION:
        case BROADCAST:
            return createAndAddBubble(msg, type, null, null);

        case PLACE: 
            var umsg :UserMessage = (msg as UserMessage);
            if (_provider == null) {
                log.warning(
                    "Asked to display user message with a null ChatInfoProvider [" + umsg + "]");
                return false;
            }

            var speaker :Name = umsg.getSpeakerDisplayName();
            var speakerBubblePos :Point = _provider.getBubblePosition(speaker);
            if (speakerBubblePos == null) {
                // this just means we have someone chatting in the channel that isn't an occupant.
                return false;
            }
            return createAndAddBubble(msg, type, speaker, speakerBubblePos);

        default:
            return false;
        }
    }

    /**
     * Create a chat bubble with the specified type and text.
     *
     * @param speakerBubblePos if non-null, contains the position of where to put the bubbles for 
     *                         this speaker in screen coordinates
     *
     * @return true if we successfully laid out the bubble
     */
    protected function createAndAddBubble (
        msg :ChatMessage, type :int, speaker :Name, speakerBubblePos :Point) :Boolean
    {
        var texts :Array = formatMessage(msg, type, false, _userBubbleFmt);
        var lifetime :int = getBubbleExpire(msg.timestamp, msg.message) - msg.timestamp;
        var bubble :BubbleGlyph =
            new BubbleGlyph(this, type, lifetime, speaker, _defaultBubbleFmt, texts);

        var cloud :BubbleCloud = _bubbles.get(speaker);
        if (cloud == null) {
            var local :Point = 
                speakerBubblePos == null ? null : _scrollOverlay.globalToLocal(speakerBubblePos);
            cloud = 
                new BubbleCloud(this, MAX_BUBBLES_PER_USER, local, _target.width, _target.height);
            _bubbles.put(speaker, cloud);
        }
        cloud.addBubble(bubble);
        if (placeOf(type) == PLACE) {
            _scrollOverlay.addChild(bubble);
        } else {
            _staticOverlay.addChild(bubble);
        }
        _allBubbles.unshift(bubble);

        for (var ii :int = 0; ii < _allBubbles.length; ii++) {
            (_allBubbles[ii] as BubbleGlyph).setAgeLevel(ii);
        }

        return true;
    }

    /**
     * Get the function that draws the bubble shape for the specified type of bubble.
     */
    protected function getBubbleShape (type :int) :Function
    {
        switch (placeOf(type)) {
        case INFO:
        case ATTENTION:
            return drawRectangle;
        }

        switch (modeOf(type)) {
        case SPEAK:
            return drawRoundedBubble;
        case EMOTE:
            return drawEmoteBubble;
        case THINK:
            return drawThinkBubble;
        }

        // fall back to subtitle shape
        return getSubtitleShape(type);
    }

    /**
     * Get the function that draws the tail shape for the specified type of bubble.
     */
    protected function getTailShape (type :int) :Function
    {
        if (placeOf(type) != PLACE) {
            return null;
        }

        switch(modeOf(type)) {
        case SPEAK:
            return drawSpeakTail;
        case THINK:
            return drawThinkTail;
        }

        return null;
    }

    protected function drawSpeakTail (g :Graphics, w :int, h :int, outline :int, fill :int) :void
    {
        // draw the tail 1/6 of the width from the right;
        var tailWidth :Number = PAD * 3 / 4;
        var tailX :Number = w - Math.max(getRoundedCornerSize(w, h) * 3 / 8, w / 6) - tailWidth;

        // first fill the shape we want
        g.lineStyle(1, fill);
        g.beginFill(fill);
        g.drawRect(tailX, h - PAD, tailWidth, PAD);
        g.moveTo(tailX, h);
        g.curveTo(
            tailX + tailWidth * 2 / 3, h + PAD / 2, tailX + tailWidth * 1 / 3, h + PAD * 3 / 4);
        g.curveTo(tailX + tailWidth, h + PAD * 3 / 8, tailX + tailWidth, h);
        g.endFill();

        // now draw the border
        g.lineStyle(1, outline);
        g.moveTo(tailX - 2, h);
        g.lineTo(tailX, h);
        g.curveTo(
            tailX + tailWidth * 2 / 3, h + PAD / 2, tailX + tailWidth * 1 / 3, h + PAD * 3 / 4);
        g.curveTo(tailX + tailWidth, h + PAD * 3 / 8, tailX + tailWidth, h);
    }

    protected function drawThinkTail (g :Graphics, w :int, h :int, outline :int, fill :int) :void
    {
        var tailX :Number = w - w / 6 - PAD * 3 / 4;

        g.lineStyle(1, outline);
        g.beginFill(fill);
        g.drawCircle(tailX, h + 6, 4);
        g.drawCircle(tailX - 4, h + 15, 3);
        g.endFill();
    }

    protected function getRoundedCornerSize (w :int, h :int) :Number
    {
        return Math.min(Math.max(Math.max(w, h) / 2, PAD * 2), 75);
    }

    /** Bubble draw function. See getBubbleShape() */
    protected function drawRoundedBubble (g :Graphics, w :int, h :int) :void
    {
        var cornerSize :Number = getRoundedCornerSize(w, h);
        g.drawRoundRect(0, 0, w, h, cornerSize, cornerSize);
    }

    /** Bubble draw function. See getBubbleShape() */
    protected function drawEmoteBubble (g :Graphics, w :int, h :int) :void
    {
        var hw :Number = w / 2;
        var hh :Number = h / 2;
        g.moveTo(0, 0);
        g.curveTo(hw, PAD * 2, w, 0);
        g.curveTo(w - (PAD * 2), hh, w, h);
        g.curveTo(hw, h - (PAD * 2), 0, h);
        g.curveTo(PAD * 2, hh, 0, 0);
    }

    /** Bubble draw function. See getBubbleShape() */
    protected function drawThinkBubble (g :Graphics, w :int, h :int) :void
    {
        var thinkPad :Number = PAD / 2;
        var targetDia :int = 16;
        // amount of space we need to leave at each end of the lines, so that the corner bubble 
        // is about the same size as the rest of the bubbles.
        var endBuf :int = Math.round(Math.sin(45 * Math.PI / 180) * targetDia);

        var bumpyWidth :Number = w - thinkPad * 2 - endBuf * 2;
        var hDia :int = distributeCloselyWithin(bumpyWidth, targetDia, 4);
        var hBumps :int = Math.round(bumpyWidth / hDia);

        var bumpyHeight :Number = h - thinkPad * 2 - endBuf * 2;
        var vDia :int = distributeCloselyWithin(bumpyHeight, targetDia, 4);
        var vBumps :int = Math.round(bumpyHeight / vDia);

        var yy :int;
        var xx :int;
        var ii :int;
        var control :Point;

        // top edge
        g.moveTo(xx = (thinkPad + endBuf), thinkPad);
        for (ii = 0; ii < hBumps; ii++, xx += hDia) {
            g.curveTo(xx + hDia / 2, -thinkPad, xx + hDia, thinkPad);
        }
        control = findControlPoint(new Point(xx, thinkPad), 
                                   new Point(w - thinkPad, yy = (thinkPad + endBuf)),
                                   thinkPad * 2);
        g.curveTo(control.x, control.y, w - thinkPad, yy);

        // right edge
        for (ii = 0; ii < vBumps; ii++, yy += vDia) {
            g.curveTo(w + thinkPad, yy + vDia / 2, w - thinkPad, yy + vDia);
        }
        control = findControlPoint(new Point(xx = (w - thinkPad - endBuf), h - thinkPad), 
                                   new Point(w - thinkPad, yy),
                                   thinkPad * 2);
        g.curveTo(control.x, control.y, xx, h - thinkPad);

        // bottom edge
        for (ii = 0; ii < hBumps; ii++, xx -= hDia) {
            g.curveTo(xx - hDia / 2, h + thinkPad, xx - hDia, h - thinkPad);
        }
        control = findControlPoint(new Point(xx, h - thinkPad),
                                   new Point(thinkPad, yy = (h - thinkPad - endBuf)),
                                   thinkPad * 2);
        g.curveTo(control.x, control.y, thinkPad, yy);

        // left edge
        for (ii = 0; ii < vBumps; ii++, yy -= vDia)  {
            g.curveTo(-thinkPad, yy - vDia / 2, thinkPad, yy - vDia);
        }
        control = findControlPoint(new Point(xx = (thinkPad + endBuf), thinkPad),
                                   new Point(thinkPad, yy),
                                   thinkPad * 2);
        g.curveTo(control.x, control.y, xx, thinkPad);
    }

    protected function findControlPoint (from :Point, to :Point, length :Number) :Point 
    {
        // find the control point that draws a perpendicular line from the center of the line
        // between the two points, and is length away from that line.
        var control :Point = Point.interpolate(from, to, 0.5);
        control = control.add(
            Point.polar(length, Math.atan2(from.y - control.y, to.x - control.x)));
        return control;
    }

    protected function distributeCloselyWithin (length :int, target :int, tolerance :int) :int
    {
        // find the number that divides the most closely into length, and is closest to target,
        // within the given tolerance.
        var best :int = target - tolerance;
        for (var ii :int = target - tolerance; ii <= target + 2; ii++) {
            var bestResult :int = length % best;
            bestResult = 
                bestResult > length - (length % best) ? length - (length % best) : bestResult;

            var iiResult :int = length % ii;
            iiResult = iiResult > length - (length % ii) ? length - (length % ii) : iiResult;

            if (bestResult > iiResult) {
                best = ii;
            } else if (bestResult == iiResult) {
                if (Math.abs(best - length) > Math.abs(ii - length)) {
                    best = ii;
                }
            }
        }
        return best;
    }

    /**
     * Position the label based on the type.
     */
    protected function getBubbleLabelOffset (type :int) :int
    {
        switch (modeOf(type)) {
        case SHOUT:
        case EMOTE:
        case THINK:
            return (PAD * 2);

        default:
            return PAD;
        }
    }

    // documentation inherited
    override protected function getDisplayDurationIndex () :int
    {
        // normalize the duration returned by super. Annoying.
        return super.getDisplayDurationIndex() - 1;
    }

    private static const log :Log = Log.getLog(ComicOverlay);

    /** The overlay we place on top of our target that contains all the chat glyphs that can 
     * scroll. */
    protected var _scrollOverlay :Sprite = new Sprite();

    /** The overlay we place on top of our target that contains all the chat glyphs that should
     * not scroll with the scene. */
    protected var _staticOverlay :StaticOverlay = new StaticOverlay();

    /** The provider of info about laying out bubbles. */ 
    protected var _provider :ChatInfoProvider;

    /** A copy of super's _defaultFmt, with a differnent alignment. */
    protected var _defaultBubbleFmt :TextFormat;

    /** A copy of super's _userSpeakFmt, with a different alignment. */
    protected var _userBubbleFmt :TextFormat;

    /** The place in our history at which we last entered a new place. */
    protected var _newPlacePoint :int = 0;

    /** Maps speaker name to BubbleCloud */
    protected var _bubbles :HashMap = new HashMap();

    protected var _allBubbles :Array = [];

    protected var _lastBubbleExpire :int = 0;

    /** The maximum number of bubbles to show per user. */
    protected static const MAX_BUBBLES_PER_USER :int = 3;
}
}

import flash.display.Sprite;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.Log;

import com.threerings.flash.DisplayUtil;

import com.threerings.msoy.client.PlaceLayer;

import com.threerings.msoy.chat.client.BubbleGlyph;
import com.threerings.msoy.chat.client.ComicOverlay;

/**
 * A class to keep track of the bubbles spoken by a speaker.  When the speaker moves, this class
 * is told the new location so that it can layout its bubbles correctly.  This may get nixed or 
 * fancied up on the next pass of bubble layout...
 */
class BubbleCloud 
{
    public function BubbleCloud (overlay :ComicOverlay, maxBubbles :int, pos :Point,
        viewWidth :Number, viewHeight :Number) 
    {
        _scrollOverlay = overlay;
        _maxBubbles = maxBubbles;
        _pos = pos;
        _viewWidth = viewWidth;
        _viewHeight = viewHeight;
    }

    public function get bubbles () :Array 
    {
        return _bubbles;
    }

    public function set viewWidth (w :Number) :void
    {
        _viewWidth = w;
        if (_pos == null) {
            for each (var bubble :BubbleGlyph in _bubbles) {
                // force the bubbles to be repositioned.
                bubble.x = 0;
                bubble.y = 0;
            }
            setSpeakerLocation(_pos);
        }
    }

    public function setSpeakerLocation (pos :Point) :void
    {
        _pos = pos;
        if (pos == null) {
            // BubbleClouds with null speaker pos are those not being shown in PLACE (non speak, 
            // think, emote, etc), and aren't being placed over an ActorSprite.
            var vpos :Rectangle = new Rectangle(BUBBLE_SPACING, BUBBLE_SPACING, 
                _viewWidth - BUBBLE_SPACING * 2, _viewHeight - BUBBLE_SPACING * 2);
            var avoidList :Array = [];
            var placeList :Array = [];
            for (var ii :int = 0; ii < _bubbles.length; ii++) {
                var bubble :BubbleGlyph = _bubbles[ii] as BubbleGlyph;
                if (bubble.x != 0 || bubble.y != 0) {
                    avoidList.push(bubble.getBubbleBounds());
                } else {
                    placeList.push(bubble);
                }
            }
            for each (bubble in placeList) {
                var placer :Rectangle = bubble.getBubbleBounds();
                placer.x = _viewWidth - placer.width - BUBBLE_SPACING;
                placer.y = BUBBLE_SPACING;
                DisplayUtil.positionRect(placer, vpos, avoidList);
                bubble.x = placer.x;
                bubble.y = placer.y;
                avoidList.push(placer);
            }
        } else {
            var yOffset :Number = pos.y - BUBBLE_SPACING; 
            for each (bubble in _bubbles) {
                var bubBounds :Rectangle = bubble.getBubbleBounds();
                yOffset -= bubBounds.height;
                bubble.x = pos.x - bubBounds.width / 2;
                bubble.y = yOffset;
            }
        }
    }

    public function addBubble (bubble :BubbleGlyph) :void
    {
        _bubbles.unshift(bubble);
        while (_bubbles.length > _maxBubbles) {
            _scrollOverlay.removeGlyph(_bubbles.pop() as BubbleGlyph);
        }
        for (var ii :int = 1; ii < _bubbles.length; ii++) {
            (_bubbles[ii] as BubbleGlyph).removeTail();
        }
        // refresh the bubble display
        setSpeakerLocation(_pos);
    }

    public function removeBubble (bubble :BubbleGlyph) :void
    {
        for (var ii :int = 0; ii < _bubbles.length; ii++) {
            if (_bubbles[ii] == bubble) {
                _bubbles.splice(ii, 1);
                // refresh the bubble display
                setSpeakerLocation(_pos);
                break;
            }
        }
        // make sure the bubble gets removed from the overlay, whether we found it here or not.
        _scrollOverlay.removeGlyph(bubble);
    }

    private static const log :Log = Log.getLog(BubbleCloud);

    /** The space we force between adjacent bubbles. */
    protected static const BUBBLE_SPACING :int = 5;

    protected var _bubbles :Array = [];
    protected var _pos :Point;
    protected var _scrollOverlay :ComicOverlay;
    protected var _maxBubbles :int;
    protected var _viewWidth :Number;
    protected var _viewHeight :Number;
}

class StaticOverlay extends Sprite
    implements PlaceLayer
{
    public function setComicOverlay (comicOverlay :ComicOverlay) :void
    {
        _comicOverlay = comicOverlay;
    }

    // from PlaceLayer
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        if (_comicOverlay != null) {
            _comicOverlay.setPlaceSize(unscaledWidth, unscaledHeight);
        }
    }

    protected var _comicOverlay :ComicOverlay;
}
