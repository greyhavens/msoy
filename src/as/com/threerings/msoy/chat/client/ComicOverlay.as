//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextFormat;
import flash.text.TextFormatAlign;

import mx.core.Container;
import mx.core.IFlexDisplayObject;

import com.threerings.util.ArrayUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.flash.ColorUtil;
import com.threerings.flash.DisplayUtil;

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
        for (var ii :int = _bubbles.length - 1; ii >= 0; ii--) {
            var rec :ChatGlyph = (_bubbles[ii] as ChatGlyph);
            if (all || isPlaceOrientedType(rec.getType())) {
                _bubbles.splice(ii, 1);
                removeGlyph(rec);
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
        return ((index >= _newPlacePoint) || (! isPlaceOrientedType(getType(msg, false))));
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
                return true; // EXIT;
            }
            // if the bubble didn't fit (unlikely), make it a subtitle
            break;

        case PLACE: {
            var umsg :UserMessage = (msg as UserMessage);
            var speaker :Name = umsg.getSpeakerDisplayName();
            var speakerInfo :Array = _provider.getSpeakerInfo(speaker);
            if (speakerInfo == null) {
                log.warning("ChatOverlay.InfoProvider doesn't know the speaker! " +
                    "[speaker=" + speaker + ", type=" + type + "].");
                return false;
            }

//TODO: bubble continuations
//            // try to add all the text as a bubble, but if it doesn't
//            // fit, add some of it and 'continue' the rest in a subtitle.
//            var leftover :String = text;
//            for (var ii :int = 1; ii < 7; ii++) {
//                var bubtext :String = splitNear(text, text.length() / ii);
//                if (createBubble(type, umsg.timestamp,
//                                 bubtext + ((ii > 1) ? "..." : ""),
//                                 umsg.speaker, speakerloc)) {
//
//                    leftover = text.substring(bubtext.length());
//                    break;
//                }
//            }
//
//            if (leftover.length() > 0 && !isHistoryMode()) {
//                var ltext :String = MessageBundle.tcompose(
//                    "m.continue_format", umsg.speaker);
//                ltext = xlate(ltext) + " \"" + leftover + "\"";
//                addSubtitle(createSubtitle(CONTINUATION,
//                    msg.timestamp, null, 0, ltext, true));
//            }

            // TODO: adapt above code, with formats
            if (createBubble(msg, type, speaker, speakerInfo)) {
                return true; // EXIT
            }
            // else: turn into subtitle
            // TODO: continuations...
            }
            break;
        }

        // show the message as a subtitle instead
        return super.displayTypedMessageNow(msg, type);
    }

//    /**
//     * Split the text at the space nearest the specified location.
//     */
//    protected function splitNear (text :String, pos :int) :String
//    {
//        if (pos >= text.length()) {
//            return text;
//        }
//
//        var forward :int = text.indexOf(" ", pos);
//        var backward :int = text.lastIndexOf(" ", pos);
//
//        int newpos = (Math.abs(pos - forward) < Math.abs(pos - backward))
//            ? forward : backward;
//
//        // if we couldn't find a decent place to split, just do it wherever
//        if (newpos == -1) {
//            newpos = pos;
//
//        } else {
//            // actually split the space onto the first part
//            newpos++;
//        }
//        return text.substring(0, newpos);
//    }

    /**
     * Create a chat bubble with the specified type and text.
     *
     * @param speakerInfo if non-null, contains the speaker info described in
     * ChatInfoProvider.getSpeakerInfo().
     *
     * @return true if we successfully laid out the bubble
     */
    protected function createBubble (
        msg :ChatMessage, type :int, speaker :Name, speakerInfo :Array) :Boolean
    {
        var ii :int;
        var texts :Array = formatMessage(msg, type, false, _userBubbleFmt);
        var lifetime :int = getLifetime(msg, true);
        var bubble :BubbleGlyph =
            new BubbleGlyph(this, type, lifetime, speaker, _defaultBubbleFmt, texts);

        // get the size of the new bubble
        var r :Rectangle = getBubbleSize(type, bubble.getTextSize());

        // get the user's old bubbles.
        var oldbubs :Array = getAndExpireBubbles(speaker);
        var numold :int = oldbubs.length;

        var speakerBounds :Rectangle = (speakerInfo == null) ? null : (speakerInfo[0] as Rectangle);
        var placer :Rectangle;
        var bigR :Rectangle = null;
        if (numold == 0) {
            placer = r.clone();
            positionRectIdeally(placer, type, speakerBounds);

        } else {
            // get a big rectangle encompassing the old and new
            bigR = getRectWithOlds(r, oldbubs);
            placer = bigR.clone();

            positionRectIdeally(placer, type, speakerBounds);
            // we actually try to place midway between ideal and old and adjust up half the height
            // of the new boy
            placer.x = (placer.x + bigR.x) / 2;
            placer.y = (placer.y + (bigR.y - (r.height / 2))) / 2;
        }

        // then look for a place nearby where it will fit (making sure we only put it in the area
        // above the subtitles)
        var vbounds :Rectangle = new Rectangle(0, 0, _target.width, _targetBounds.y);
        if (!DisplayUtil.positionRect(placer, vbounds, getAvoidList(speaker))) {
            // we couldn't fit the bubble!
            return false;
        }

        // now 'placer' is positioned reasonably.
        if (0 == numold) {
            bubble.x = placer.x;
            bubble.y = placer.y;

        } else {
            var dx :int = placer.x - bigR.x;
            var dy :int = placer.y - bigR.y;
            for (ii = 0; ii < numold; ii++) {
                var bub :BubbleGlyph = (oldbubs[ii] as BubbleGlyph);
                bub.removeTail();
                var ob :Rectangle = bub.getBubbleBounds();
                // recenter the translated bub within placer's width..
                var xadjust :int = dx - (ob.x - bigR.x) + (placer.width - ob.width) / 2;
                bub.x += xadjust;
                bub.y += dy;
            }

            // and position 'r' in the right place relative to 'placer'
            bubble.x = placer.x + (placer.width - r.width) / 2,
            bubble.y = placer.y + placer.height - r.height;
        }

        // now add it
        _bubbles.push(bubble);
        _overlay.addChild(bubble);

        // and we need to dirty all the bubbles because they'll all be painted in slightly
        // different colors
        var numbubs :int = _bubbles.length;
        for (ii = 0; ii < numbubs; ii++) {
            (_bubbles[ii] as BubbleGlyph).setAgeLevel(this, numbubs - ii - 1);
        }

        return true; // success!
    }

    /**
     * Calculate the size of the chat bubble based on the dimensions of the label and the type of
     * chat. It will be turned into a shape later, but we manipulate it for a while as just a
     * rectangle (which are easier to move about and do intersection tests with, and besides the
     * Shape interface has no way to translate).
     */
    protected function getBubbleSize (type :int, r :Rectangle) :Rectangle
    {
        switch (modeOf(type)) {
        case SHOUT:
        case THINK:
        case EMOTE:
            // extra room for these two monsters
            r.inflate(PAD * 2, PAD * 2);
            break;

        default:
            r.inflate(PAD, PAD);
            break;
        }

        return r;
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
        switch (placeOf(type)) {
        case INFO:
        case ATTENTION:
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

    /**
     * Position the rectangle in its ideal location given the type and speaker positon (which may
     * be null).
     */
    protected function positionRectIdeally (
        r :Rectangle, type :int, speaker :Rectangle) :void
    {
        if (speaker != null) {
            // center horizontally at the top of the rectangle (it'll be moved)
            r.y = speaker.y;
            r.x = speaker.x + ((speaker.width - r.width) / 2);
            return;
        }

        // otherwise we have different areas for different types
        switch (placeOf(type)) {
        case INFO:
        case FEEDBACK:
        case ATTENTION:
        case BROADCAST:
            // upper left
            r.x = BUBBLE_SPACING;
            r.y = BUBBLE_SPACING;
            return;

        case PLACE:
            log.warning("Got to a place where I shouldn't get!");
            break; // fall through
        }

        // put it in the center..
        log.debug("Unhandled chat type in getLocation() [type=" + type + "].");
        r.x = (_target.width - r.width) / 2;
        r.y = (_target.height - r.height) / 2;
    }

    /**
     * Get a rectangle based on the old bubbles, but with room for the new one.
     */
    protected function getRectWithOlds (r :Rectangle, oldbubs :Array) :Rectangle
    {
        var n :int = oldbubs.length;
        // if no old bubs, just return the new one.
        if (n == 0) {
            return r;
        }

        // otherwise, encompass all the oldies
        var bigR :Rectangle = null;
        for (var ii :int = 0; ii < n; ii++) {
            var bub :BubbleGlyph = (oldbubs[ii] as BubbleGlyph);
            if (ii == 0) {
                bigR = bub.getBubbleBounds();
            } else {
                bigR = bigR.union(bub.getBubbleBounds());
            }
        }

        // and add space for the new boy
        bigR.width = Math.max(bigR.width, r.width);
        bigR.height += r.height;

        return bigR;
    }

    /**
     * Expire a bubble, if necessary, and return the old bubbles for the specified speaker.
     */
    protected function getAndExpireBubbles (speaker :Name) :Array
    {
        var num :int = _bubbles.length;
        var bub :BubbleGlyph;

        // first, get all the old bubbles belonging to the user
        var oldbubs :Array = [];
        if (speaker != null) {
            for (var ii :int = 0; ii < num; ii++) {
                bub = (_bubbles[ii] as BubbleGlyph);
                if (bub.isSpeaker(speaker)) {
                    oldbubs.push(bub);
                }
            }
        }

        // see if we need to expire this user's oldest bubble
        if (oldbubs.length >= MAX_BUBBLES_PER_USER) {
            bub = (oldbubs.shift() as BubbleGlyph);
            ArrayUtil.removeFirst(_bubbles, bub);
            removeGlyph(bub);

            // or some other old bubble
        } else if (num >= MAX_BUBBLES) {
            removeGlyph(_bubbles.shift() as BubbleGlyph);
        }

        // return the speaker's old bubbles
        return oldbubs;
    }

    override internal function glyphExpired (glyph :ChatGlyph) :void
    {
        ArrayUtil.removeFirst(_bubbles, glyph);
        super.glyphExpired(glyph);
    }

    /**
     * Return a list of rectangular areas that we should avoid while laying out a bubble for the
     * specified speaker.
     */
    protected function getAvoidList (speaker :Name) :Array
    {
        var avoid :Array = [];
        if (_provider == null) {
            return avoid;
        }
        var r :Rectangle;

        // for now we don't accept low-priority avoids
        _provider.getAvoidables(speaker, avoid, null);

        // shift those by any offset on our overlay
        var offset :Point = _overlay.localToGlobal(new Point(0, 0));
        offset.x *= -1;
        offset.y *= -1;
        for each (r in avoid) {
            r.offsetPoint(offset);
        }

        // add the existing chatbub non-tail areas from other speakers
        for each (var bub :BubbleGlyph in _bubbles) {
            if (!bub.isSpeaker(speaker)) {
                avoid.push(bub.getBubbleTerritory());
            }
        }

//        if (_avoids == null) {
//            _avoids = new Sprite();
//            _overlay.addChild(_avoids);
//        }
//
//        while (_avoids.numChildren > 0) {
//            _avoids.removeChildAt(0);
//        }
//
//        for each (r in avoid) {
//            var s :Sprite = new Sprite();
//            s.graphics.lineStyle(1, 0xFF0000);
//            s.graphics.drawRect(0, 0, r.width, r.height);
//            s.x = r.x;
//            s.y = r.y;
//            _avoids.addChild(s);
//        }
//
        return avoid;
    }

// DEBUGGING: drawing red boxes around the avoid areas
//    protected var _avoids :Sprite;

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

    /** The currently displayed bubble areas. */
    protected var _bubbles :Array = [];

    /** The space we force between adjacent bubbles. */
    protected static const BUBBLE_SPACING :int = 15;

    /** The distance to stay from the speaker. */
    protected static const SPEAKER_DISTANCE :int = 20;

    /** The width of the end of the tail. */
    protected static const TAIL_WIDTH :int = 12;

    /** The maximum number of bubbles to show. */
    protected static const MAX_BUBBLES :int = 8;

    /** The maximum number of bubbles to show per user. */
    protected static const MAX_BUBBLES_PER_USER :int = 3;

    /** The background colors to use when drawing bubbles. */
    protected static const BACKGROUNDS :Array = new Array(MAX_BUBBLES);

    private static function staticInit () :void
    {
        var yellowy :uint = 0xdddd6a;
        var blackish :uint = 0xcccccc;

        var steps :Number = (MAX_BUBBLES - 1) / 2;
        var ii :int;
        for (ii = 0; ii < MAX_BUBBLES / 2; ii++) {
            BACKGROUNDS[ii] = ColorUtil.blend(0xFFFFFF, yellowy,
                (steps - ii) / steps);
        }
        for (ii = MAX_BUBBLES / 2; ii < MAX_BUBBLES; ii++) {
            BACKGROUNDS[ii] = ColorUtil.blend(blackish, yellowy,
                (ii - steps) / steps);
        }
    }
    staticInit();

//    [Embed(source="../../../../../../../rsrc/media/skins/bubble.png",
//        scaleGridTop="7", scaleGridBottom="32", scaleGridLeft="9", scaleGridRight="73")]
//    protected static const BUB :Class;
//    [Embed(source="../../../../../../../rsrc/media/skins/bubble.swf")]
//    protected static const BUB :Class;
}
}
