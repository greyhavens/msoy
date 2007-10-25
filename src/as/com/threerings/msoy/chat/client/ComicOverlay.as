//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.Graphics;

import flash.geom.Rectangle;

import flash.text.TextFormat;
import flash.text.TextFormatAlign;

import mx.core.Container;

import com.threerings.util.HashMap;
import com.threerings.util.Name;

import com.threerings.flash.ColorUtil;

import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

/**
 * Implements comic chat in the metasoy client.
 */
public class ComicOverlay extends ChatOverlay
{
    /**
     * Construct a comic chat overlay.
     */
    public function ComicOverlay (ctx :WorldContext)
    {
        super(ctx.getMessageManager());
        _ctx = ctx;
    }

    /**
     * Called by our target when we've entered a new place.
     */
    public function newPlaceEntered (provider :ChatInfoProvider) :void
    {
        _provider = provider;
        _newPlacePoint = _history.size();

        // and clear place-oriented bubbles
        clearBubbles(false);
    }

    override protected function layout (bounds :Rectangle, targetWidth :int) :void
    {
        clearBubbles(true); // these will get repopulated from the history
        super.layout(bounds, targetWidth);
    }

    override public function setTarget (target :Container, targetWidth :int = -1) :void
    {
        if (_target != null) {
            clearBubbles(true);
        }
        super.setTarget(target, targetWidth);
    }

    override public function clear () :void
    {
        super.clear();
        clearBubbles(true);
    }

    public function speakerMoved (speaker :Name, bounds :Rectangle) :void
    {
        var cloud :BubbleCloud = _bubbles.get(speaker);
        if (cloud != null) {
            cloud.setSpeakerLocation(bounds);
        }
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

    /**
     * Clear chat bubbles, either all of them or just the place-oriented ones.
     */
    protected function clearBubbles (all :Boolean) :void
    {
        for each (var cloud :BubbleCloud in _bubbles.values()) {
            for each (var bubble :BubbleGlyph in cloud.bubbles) {
                if (all || isPlaceOrientedType(bubble.getType())) {
                    cloud.removeBubble(bubble);
                }
            }
        }
    }

    override internal function historyUpdated (adjustment :int) :void
    {
        _newPlacePoint -= adjustment;
        super.historyUpdated(adjustment);
    }

    override protected function shouldShowFromHistory (msg :ChatMessage, index :int) :Boolean
    {
        // if we're minimized, show nothing
        if ((_ctx.getClient() as WorldClient).isMinimized()) {
            return false;
        }
        // only show if the message was received since we last entered a new place, or if it's
        // place-less chat.
        return ((index >= _newPlacePoint) || (!isPlaceOrientedType(getType(msg, false))));
    }

    override protected function isApprovedLocalType (localtype :String) :Boolean
    {
        if (ChatCodes.PLACE_CHAT_TYPE == localtype || ChatCodes.USER_CHAT_TYPE == localtype) {
            return true;
        }
        log.debug("Ignoring non-standard system/feedback chat [localtype=" + localtype + "].");
        return false;
    }

    /**
     * Is the type of chat place-oriented.
     */
    protected function isPlaceOrientedType (type :int) :Boolean
    {
        return (placeOf(type)) == PLACE;
    }

    override public function displayMessage (msg :ChatMessage, alreadyDisp :Boolean) :Boolean
    {
        if ((_ctx.getClient() as WorldClient).isMinimized()) {
            return false; // no comic messages while minimized
        }
        return super.displayMessage(msg, alreadyDisp);
    }

    override protected function displayTypedMessageNow (msg :ChatMessage, type :int) :Boolean
    {
        switch (placeOf(type)) {
        case INFO:
        case FEEDBACK:
        case ATTENTION:
        case BROADCAST:
            if (createBubble(msg, type, null, null)) {
                return true;
            }
            // if the bubble didn't fit (unlikely), make it a subtitle
            break;

        case PLACE: 
            var umsg :UserMessage = (msg as UserMessage);
            var speaker :Name = umsg.getSpeakerDisplayName();
            var speakerBounds :Rectangle = _provider.getSpeakerBounds(speaker);
            if (speakerBounds == null) {
                log.warning("ChatOverlay.InfoProvider doesn't know the speaker! " +
                    "[speaker=" + speaker + ", type=" + type + "].");
                return false;
            }

            if (createBubble(msg, type, speaker, speakerBounds)) {
                return true;
            }
            // if the bubble didn't fit (unlikely), make it a subtitle
            break;
        }

        // show the message as a subtitle instead
        return super.displayTypedMessageNow(msg, type);
    }

    /**
     * Create a chat bubble with the specified type and text.
     *
     * @param speakerBounds if non-null, contains the bounds of the speaker in screen coordinates
     *
     * @return true if we successfully laid out the bubble
     */
    protected function createBubble (
        msg :ChatMessage, type :int, speaker :Name, speakerBounds :Rectangle) :Boolean
    {
        var ii :int;
        var texts :Array = formatMessage(msg, type, false, _userBubbleFmt);
        var lifetime :int = getLifetime(msg, true);
        var bubble :BubbleGlyph =
            new BubbleGlyph(this, type, lifetime, speaker, _defaultBubbleFmt, texts);

        var cloud :BubbleCloud = _bubbles.get(speaker);
        if (cloud == null) {
            cloud = new BubbleCloud(this, MAX_BUBBLES_PER_USER, speakerBounds);
            _bubbles.put(speaker, cloud);
        }
        cloud.addBubble(bubble);
        _overlay.addChild(bubble);

        // TODO: dirty the old bubbles
//        var numbubs :int = _bubbles.length;
//        for (ii = 0; ii < numbubs; ii++) {
//            (_bubbles[ii] as BubbleGlyph).setAgeLevel(this, numbubs - ii - 1);
//        }

        return true;
    }

    /**
     * Draw the specified bubble shape.
     *
     * @return the padding that should be applied to the bubble's label.
     */
    internal function drawBubbleShape (g :Graphics, type :int, txtWidth :int, txtHeight :int,
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
        // first fill the shape we want
        g.lineStyle(1, fill);
        g.beginFill(fill);
        g.drawRect(w - PAD, h - PAD, PAD, PAD);
        g.moveTo(w - PAD * 3 / 4, h);
        g.curveTo(w - PAD / 4, h + PAD / 4, w - PAD / 2, h + PAD / 2);
        g.curveTo(w, h + PAD * 3 / 8, w, h);
        g.endFill();

        // now draw the border
        g.lineStyle(1, outline);
        g.moveTo(w - PAD - 2, h);
        g.lineTo(w - PAD * 3 / 4, h);
        g.curveTo(w - PAD / 4, h + PAD / 4, w - PAD / 2, h + PAD / 2);
        g.curveTo(w, h + PAD * 3 / 8, w, h);
        g.lineTo(w, h - PAD - 2);
    }

    protected function drawThinkTail (g :Graphics, w :int, h :int, outline :int, fill :int) :void
    {
        g.lineStyle(1, outline);
        g.beginFill(fill);
        // the think bubble doesn't really utilize the w and h that are sent to this method very 
        // well... we can get a little closer than may seem wise.
        g.drawCircle(w - 9, h - 1, 4);
        g.drawCircle(w - 13, h + 8, 3);
        g.endFill();
    }

    /** Bubble draw function. See getBubbleShape() */
    protected function drawRoundedBubble (g :Graphics, w :int, h :int) :void
    {
        g.drawRoundRect(0, 0, w, h, PAD * 2, PAD * 2);
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
        const V_DIA :int = 16;
        const H_DIA :int = 12;

        g.moveTo(PAD, PAD);

        var yy :int;
        var ty :int;
        var xx :int;
        var tx :int;
        for (xx = PAD; xx < (w - PAD); xx += H_DIA) {
            tx = Math.min(w - PAD, xx + H_DIA);
            g.curveTo((xx + tx)/2, 0, tx, PAD);
        }

        for (yy = PAD; yy < (h - PAD); yy += V_DIA) {
            ty = Math.min(h - PAD, yy + V_DIA);
            g.curveTo(w, (yy + ty)/2, w - PAD, ty);
        }

        for (xx = (w - PAD); xx > 0; xx -= H_DIA) {
            tx = Math.max(PAD, xx - H_DIA);
            g.curveTo((xx + tx)/2, h, tx, h - PAD);
        }

        for (yy = (h - PAD); yy > 0; yy -= V_DIA) {
            ty = Math.max(PAD, yy - V_DIA);
            g.curveTo(0, (yy + ty)/2, PAD, ty);
        }
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

    override internal function glyphExpired (glyph :ChatGlyph) :void
    {
        if (glyph is BubbleGlyph) {
            var bubble :BubbleGlyph = glyph as BubbleGlyph;
            var cloud :BubbleCloud = _bubbles.get(bubble.getSpeaker());
            cloud.removeBubble(bubble);
        }
        super.glyphExpired(glyph);
    }

    // documentation inherited
    override protected function getDisplayDurationIndex () :int
    {
        // normalize the duration returned by super. Annoying.
        return super.getDisplayDurationIndex() - 1;
    }

    /** Giver of life, context. */
    protected var _ctx :WorldContext;

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

    /** The maximum number of bubbles to show per user. */
    protected static const MAX_BUBBLES_PER_USER :int = 3;
}
}

import flash.geom.Rectangle;

import com.threerings.msoy.chat.client.BubbleGlyph;
import com.threerings.msoy.chat.client.ComicOverlay;

/**
 * A class to keep track of the bubbles spoken by a speaker.  When the speaker moves, this class
 * is told the new location so that it can layout its bubbles correctly.  This may get nixed or 
 * fancied up on the next pass of bubble layout...
 */
class BubbleCloud 
{
    public function BubbleCloud (overlay :ComicOverlay, maxBubbles :int, bounds :Rectangle) 
    {
        _overlay = overlay;
        _maxBubbles = maxBubbles;
        _location = bounds;
    }

    public function get bubbles () :Array 
    {
        return _bubbles;
    }

    public function setSpeakerLocation (bounds :Rectangle) :void
    {
        _location = bounds;
        if (bounds == null) {
            // TODO: bring back in the nifty DisplayUtil stuff for laying out the bubbles in the 
            // upper left.  BubbleClouds with null speaker bounds are those not being shown in 
            // PLACE (non speak, think, emote, etc), and aren't being placed over an ActorSprite.
            var yOffset :int = BUBBLE_SPACING;
            for each (var bubble :BubbleGlyph in _bubbles) {
                bubble.x = BUBBLE_SPACING;
                bubble.y = yOffset;
                var bubBounds :Rectangle = bubble.getBubbleBounds();
                yOffset += bubBounds.height;
            }
        } else {
            var centerX :Number = bounds.x + bounds.width / 2;
            yOffset = bounds.y - BUBBLE_SPACING; 
            for each (bubble in _bubbles) {
                bubBounds = bubble.getBubbleBounds();
                yOffset -= bubBounds.height;
                bubble.x = centerX - bubBounds.width / 2;
                bubble.y = yOffset;
            }
        }
    }

    public function addBubble (bubble :BubbleGlyph) :void
    {
        _bubbles.unshift(bubble);
        while (_bubbles.length > _maxBubbles) {
            _overlay.removeGlyph(_bubbles.pop() as BubbleGlyph);
        }
        for (var ii :int = 1; ii < _bubbles.length; ii++) {
            (_bubbles[ii] as BubbleGlyph).removeTail();
        }
        // refresh the bubble display
        setSpeakerLocation(_location);
    }

    public function removeBubble (bubble :BubbleGlyph) :void
    {
        for (var ii :int = 0; ii < _bubbles.length; ii++) {
            if (_bubbles[ii] == bubble) {
                _bubbles.splice(ii, 1);
                // refresh the bubble display
                setSpeakerLocation(_location);
                break;
            }
        }
        // make sure the bubble gets removed from the overlay, whether we found it here or not.
        _overlay.removeGlyph(bubble);
    }

    /** The space we force between adjacent bubbles. */
    protected static const BUBBLE_SPACING :int = 15;

    protected var _bubbles :Array = [];
    protected var _location :Rectangle;
    protected var _overlay :ComicOverlay;
    protected var _maxBubbles :int;
}
