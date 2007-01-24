//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.Graphics;

import flash.geom.Rectangle;

import mx.core.Container;

import com.threerings.util.ArrayUtil;
import com.threerings.util.DisplayUtil;
import com.threerings.util.ColorUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

/**
 * Implements comic chat in the yohoho client.
 */
public class ComicOverlay extends ChatOverlay
{
    /**
     * Construct a comic chat overlay.
     *
     * @param subtitleHeight the amount of vertical space to use for subtitles.
     */
    public function ComicOverlay (ctx :MsoyContext)
    {
        super(ctx);
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

    override protected function layout () :void
    {
        clearBubbles(true); // these will get repopulated from the history
        super.layout();
    }

    override public function setTarget (target :Container) :void
    {
        if (_target != null) {
            clearBubbles(true);
        }

        super.setTarget(target);
    }

    override public function clear () :void
    {
        super.clear();

        clearBubbles(true);
    }

    /**
     * Clear chat bubbles, either all of them or just the place-oriented ones.
     */
    protected function clearBubbles (all :Boolean) :void
    {
        for (var ii :int = _bubbles.length - 1; ii >= 0; ii--) {
            var rec :ChatGlyph = (_bubbles[ii] as ChatGlyph);
            if (all || isPlaceOrientedType(rec.getType())) {
                _overlay.removeChild(rec);
                _bubbles.splice(ii, 1);
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
        // only show if the message was received since we last entered
        // a new place, or if it's place-less chat.
        return ((index >= _newPlacePoint) ||
                (! isPlaceOrientedType(getType(msg, false))));
    }

    override protected function isApprovedLocalType (localtype :String) :Boolean
    {
        if (ChatCodes.PLACE_CHAT_TYPE == localtype ||
                ChatCodes.USER_CHAT_TYPE == localtype) {
            return true;
        }

        log.debug("Ignoring non-standard system/feedback chat " +
                  "[localtype=" + localtype + "].");
        return false;
    }

    /**
     * Is the type of chat place-oriented.
     */
    protected function isPlaceOrientedType (type :int) :Boolean
    {
        return (placeOf(type)) == PLACE;
    }

    override protected function displayTypedMessageNow (
        msg :ChatMessage, type :int) :Boolean
    {
        switch (placeOf(type)) {
        case INFO:
        case ATTENTION:
            if (createBubble(msg, type, null, null)) {
                return true; // EXIT;
            }
            // if the bubble didn't fit (unlikely), make it a subtitle
            break;

        case PLACE: {
            var umsg :UserMessage = (msg as UserMessage);
            var speaker :Name = umsg.getSpeakerDisplayName();
            var speakerLoc :Rectangle = _provider.getSpeaker(speaker);
            if (speakerLoc == null) {
                log.warning("ChatOverlay.InfoProvider doesn't know " +
                            "the speaker! [speaker=" + speaker + ", type=" +
                            type + "].");
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
            if (createBubble(msg, type, speaker, speakerLoc)) {
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
     * Create a chat bubble with the specified type
     * and text.
     *
     * @param speakerloc if non-null, specifies that a tail should be
     * added which points to that location.
     * @return true if we successfully laid out the bubble
     */
    protected function createBubble (
        msg :ChatMessage, type :int, 
        speaker :Name, speakerloc :Rectangle) :Boolean
    {
        var ii :int;
        var texts :Array = formatMessage(msg, type, false);
        var lifetime :int = getLifetime(msg, true);
        var bubble :BubbleGlyph = new BubbleGlyph(this, type, lifetime,
            _defaultFmt, texts);

        // get the size of the new bubble
        var r :Rectangle = getBubbleSize(type, bubble.getTextSize());

        // get the user's old bubbles.
        var oldbubs :Array = getAndExpireBubbles(speaker);
        var numold :int = oldbubs.length;

        var placer :Rectangle;
        var bigR :Rectangle = null;
        if (numold == 0) {
            placer = r.clone();
            positionRectIdeally(placer, type, speakerloc);

        } else {
            // get a big rectangle encompassing the old and new
            bigR = getRectWithOlds(r, oldbubs);
            placer = bigR.clone();

            positionRectIdeally(placer, type, speakerloc);
            // we actually try to place midway between ideal and old
            // and adjust up half the height of the new boy
            placer.x = (placer.x + bigR.x) / 2;
            placer.y = (placer.y + (bigR.y - (r.height / 2))) / 2;
        }

        // then look for a place nearby where it will fit
        // (making sure we only put it in the area above the subtitles)
        var vbounds :Rectangle = new Rectangle(
            0, 0, _target.width, _target.height);
        vbounds.height -= _subtitleHeight;
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
                var xadjust :int = dx - (ob.x - bigR.x) +
                    (placer.width - ob.width) / 2;
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

        // and we need to dirty all the bubbles because they'll all
        // be painted in slightly different colors
        var numbubs :int = _bubbles.length;
        for (ii = 0; ii < numbubs; ii++) {
            (_bubbles[ii] as BubbleGlyph).setAgeLevel(numbubs - ii - 1);
        }

        return true; // success!
    }

    /**
     * Calculate the size of the chat bubble based on the dimensions
     * of the label and the type of chat. It will be turned into a shape
     * later, but we manipulate it for a while as just a rectangle
     * (which are easier to move about and do intersection tests with,
     * and besides the Shape interface has no way to translate).
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
    internal function drawBubbleShape (
        g :Graphics, type :int, txtWidth :int, txtHeight :int) :int
    {
        // this little bit copied from superclass- if we keep: reuse
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

        // TODO: more
        g.clear();
        g.beginFill(background);
        g.drawRoundRect(0, 0, width, height, PAD, PAD);
        g.endFill();

        g.lineStyle(1, outline);
        g.drawRoundRect(0, 0, width, height, PAD, PAD);


//        // TODO: much
//        Shape shape = getBubbleShape(type, r);
//        Shape full = shape;
//
//        // if we have a tail, the full area should include that.
//        if (speakerloc != null) {
//            Area area = new Area(getTail(type, r, speakerloc));
//            area.add(new Area(shape));
//            full = area;
//        }
//
//        Color color;
//        switch (type) {
//        case INFO: color = INFO_COLOR; break;
//        case ATTENTION: color =  ATTENTION_COLOR; break;
//        default: color = Color.BLACK; break;
//        }

        return padding;
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
     * Position the rectangle in its ideal location given the type
     * and speaker positon (which may be null).
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
        case ATTENTION:
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
        r.x = (getTargetWidth() - r.width) / 2;
        r.y = (getTargetHeight() - r.height) / 2;
    }

    /**
     * Get a rectangle based on the old bubbles, but with room for the new one.
     */
    protected function getRectWithOlds (r :Rectangle, oldbubs :Array) :Rectangle
    {
        var n :int = oldbubs.size();
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
     * Get the appropriate shape for the specified type of chat.
     */
//    protected Shape getBubbleShape (int type, Rectangle r)
//    {
//        switch (placeOf(type)) {
//        case INFO:
//        case ATTENTION:
//            // boring rectangle wrapped in an Area for translation
//            return new Area(r);
//        }
//
//        switch (modeOf(type)) {
//        case SPEAK:
//            // a rounded rectangle balloon, put in an Area so that it's
//            // translatable
//            return new Area(new RoundRectangle2D.Float(
//                r.x, r.y, r.width, r.height, PAD * 4, PAD * 4));
//
//        case SHOUT: {
//            // spikey balloon
//            Polygon left = new Polygon(), right = new Polygon();
//            Polygon top = new Polygon(), bot = new Polygon();
//
//            int x = r.x + PAD;
//            int y = r.y + PAD;
//            int wid = r.width - PAD * 2;
//            int hei = r.height - PAD * 2;
//            Area a = new Area(new Rectangle(x, y, wid, hei));
//            int spikebase = 10;
//            int cornbase = spikebase*3/4;
//
//            // configure spikes to the left and right sides
//            left.addPoint(x, y);
//            left.addPoint(x - PAD, y + spikebase/2);
//            left.addPoint(x, y + spikebase);
//            right.addPoint(x + wid, y);
//            right.addPoint(x + wid + PAD, y + spikebase/2);
//            right.addPoint(x + wid, y + spikebase);
//
//            // add the left and right side spikes
//            int ypos = 0;
//            int ahei = hei - cornbase;
//            int maxpos = ahei - spikebase + 1;
//            int numvert = (int) Math.ceil(ahei / ((float) spikebase));
//            for (int ii=0; ii < numvert; ii++) {
//                int newpos = cornbase/2 +
//                    Math.min((ahei * ii) / numvert, maxpos);
//                left.translate(0, newpos - ypos);
//                right.translate(0, newpos - ypos);
//                a.add(new Area(left));
//                a.add(new Area(right));
//                ypos = newpos;
//            }
//
//            // configure spikes for the top and bottom
//            top.addPoint(x, y);
//            top.addPoint(x + spikebase/2, y - PAD);
//            top.addPoint(x + spikebase, y);
//            bot.addPoint(x, y + hei);
//            bot.addPoint(x + spikebase/2, y + hei + PAD);
//            bot.addPoint(x + spikebase, y + hei);
//
//            // add top and bottom spikes
//            int xpos = 0;
//            int awid = wid - cornbase;
//            maxpos = awid - spikebase + 1;
//            int numhorz = (int) Math.ceil(awid / ((float) spikebase));
//            for (int ii=0; ii < numhorz; ii++) {
//                int newpos = cornbase/2 +
//                    Math.min((awid * ii) / numhorz, maxpos);
//                top.translate(newpos - xpos, 0);
//                bot.translate(newpos - xpos, 0);
//                a.add(new Area(top));
//                a.add(new Area(bot));
//                xpos = newpos;
//            }
//
//            // and lets also add corner spikes
//            Polygon corner = new Polygon();
//            corner.addPoint(x, y + cornbase);
//            corner.addPoint(x - PAD + 2, y - PAD + 2);
//            corner.addPoint(x + cornbase, y);
//            a.add(new Area(corner));
//
//            corner.reset();
//            corner.addPoint(x + wid - cornbase, y);
//            corner.addPoint(x + wid + PAD - 2, y - PAD + 2);
//            corner.addPoint(x + wid, y + cornbase);
//            a.add(new Area(corner));
//
//            corner.reset();
//            corner.addPoint(x + wid, y + hei - cornbase);
//            corner.addPoint(x + wid + PAD - 2, y + hei + PAD - 2);
//            corner.addPoint(x + wid - cornbase, y + hei);
//            a.add(new Area(corner));
//
//            corner.reset();
//            corner.addPoint(x + cornbase, y + hei);
//            corner.addPoint(x - PAD + 2, y + hei + PAD - 2);
//            corner.addPoint(x, y + hei - cornbase);
//            a.add(new Area(corner));
//            // grunt work!
//
//            return a;
//        }
//
//        case EMOTE: {
//            // a box that curves inward on all sides
//            Area a = new Area(r);
//            a.subtract(new Area(new Ellipse2D.Float(
//                r.x, r.y - PAD, r.width, PAD * 2)));
//            a.subtract(new Area(new Ellipse2D.Float(
//                r.x, r.y + r.height - PAD, r.width, PAD * 2)));
//            a.subtract(new Area(new Ellipse2D.Float(
//                r.x - PAD, r.y, PAD * 2, r.height)));
//            a.subtract(new Area(new Ellipse2D.Float(
//                r.x + r.width - PAD, r.y, PAD * 2, r.height)));
//            return a;
//        }
//
//        case THINK: {
//            // cloudy balloon!
//            int x = r.x + PAD;
//            int y = r.y + PAD;
//            int wid = r.width - PAD * 2;
//            int hei = r.height - PAD * 2;
//            Area a = new Area(new Rectangle(x, y, wid, hei));
//
//            // small circles on the left and right
//            int dia = 12;
//            int numvert = (int) Math.ceil(hei / ((float) dia));
//            int leftside = x - dia/2;
//            int rightside =  x + wid - (dia/2) - 1;
//            int maxh = hei - dia;
//            for (int ii=0; ii < numvert; ii++) {
//                int ypos = y + Math.min((hei * ii) / numvert, maxh);
//                a.add(new Area(new Ellipse2D.Float(leftside, ypos,
//                    dia, dia)));
//                a.add(new Area(new Ellipse2D.Float(rightside, ypos,
//                    dia, dia)));
//            }
//
//            // larger ovals on the top and bottom
//            dia = 16;
//            int numhorz = (int) Math.ceil(wid / ((float) dia));
//            int topside = y - dia/3;
//            int botside = y + hei - (dia/3) - 1;
//            int maxw = wid - dia;
//            for (int ii=0; ii < numhorz; ii++) {
//                int xpos = x + Math.min((wid * ii) / numhorz, maxw);
//                a.add(new Area(new Ellipse2D.Float(xpos, topside,
//                    dia, dia*2/3)));
//                a.add(new Area(new Ellipse2D.Float(xpos, botside,
//                    dia, dia*2/3)));
//            }
//
//            return a;
//        }
//        }
//
//        // fall back to subtitle shape
//        return getSubtitleShape(type, r, r);
//    }

    /**
     * Create a tail to the specified rectanglular area from the
     * speaker point.
     */
//    protected Shape getTail (int type, Rectangle r, Point speaker)
//    {
//        // emotes don't actually have tails
//        if (modeOf(type) == EMOTE) {
//            return new Area(); // empty shape
//        }
//
//        int midx = r.x + (r.width / 2);
//        int midy = r.y + (r.height / 2);
//
//        // we actually want to start about SPEAKER_DISTANCE away from the
//        // speaker
//        int xx = speaker.x - midx;
//        int yy = speaker.y - midy;
//        float dist = (float) Math.sqrt(xx * xx + yy * yy);
//        float perc = (dist - SPEAKER_DISTANCE) / dist;
//
//        if (modeOf(type) == THINK) {
//            int steps = Math.max((int) (dist / SPEAKER_DISTANCE), 2);
//            float step = perc / steps;
//            Area a = new Area();
//            for (int ii=0; ii < steps; ii++, perc -= step) {
//                int radius = Math.min(SPEAKER_DISTANCE / 2 - 1, ii + 2);
//                a.add(new Area(new Ellipse2D.Float(
//                  (int) ((1 - perc) * midx + perc * speaker.x) + perc * radius,
//                  (int) ((1 - perc) * midy + perc * speaker.y) + perc * radius,
//                  radius * 2, radius * 2)));
//            }
//
//            return a;
//        }
//
//        // ELSE draw a triangular tail shape
//        Polygon p = new Polygon();
//        p.addPoint((int) ((1 - perc) * midx + perc * speaker.x),
//                   (int) ((1 - perc) * midy + perc * speaker.y));
//
//        if (Math.abs(speaker.x - midx) > Math.abs(speaker.y - midy)) {
//            int x;
//            if (midx > speaker.x) {
//                x = r.x + PAD;
//            } else {
//                x = r.x + r.width - PAD;
//            }
//            p.addPoint(x, midy - (TAIL_WIDTH / 2));
//            p.addPoint(x, midy + (TAIL_WIDTH / 2));
//
//        } else {
//            int y;
//            if (midy > speaker.y) {
//                y = r.y + PAD;
//            } else {
//                y = r.y + r.height - PAD;
//            }
//            p.addPoint(midx - (TAIL_WIDTH / 2), y);
//            p.addPoint(midx + (TAIL_WIDTH / 2), y);
//        }
//
//        return p;
//    }

    /**
     * Expire a bubble, if necessary, and return the old bubbles
     * for the specified speaker.
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
            _target.removeChild(bub);

            // or some other old bubble
        } else if (num >= MAX_BUBBLES) {
            _target.removeChild(_bubbles.shift() as BubbleGlyph);
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
     * Return a list of rectangular areas that we should avoid while
     * laying out a bubble for the specified speaker.
     */
    protected function getAvoidList (speaker :Name) :Array
    {
        var avoid :Array = [];
        if (_provider == null) {
            return avoid;
        }

        // for now we don't accept low-priority avoids
        _provider.getAvoidables(speaker, avoid, null);

        // add the existing chatbub non-tail areas from other speakers
        for each (var bub :BubbleGlyph in _bubbles) {
            if (!bub.isSpeaker(speaker)) {
                avoid.push(bub.getBubbleTerritory());
            }
        }

        return avoid;
    }

    // documentation inherited
    override protected function getDisplayDurationIndex () :int
    {
        // normalize the duration returned by super. Annoying.
        return super.getDisplayDurationIndex() - 1;
    }

    /** The provider of info about laying out bubbles. */ 
    protected var _provider :ChatInfoProvider;

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
}
}
