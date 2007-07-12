//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.Shape;

import flash.events.TimerEvent;

import flash.geom.Rectangle;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Name;

import com.threerings.flash.TextFieldUtil;

public class BubbleGlyph extends ChatGlyph
{
    public function BubbleGlyph (
        overlay :ComicOverlay, type :int, lifetime :int,
        speaker :Name, defaultFmt :TextFormat, texts :Array)
    {
        super(overlay, type, lifetime);
        _speaker = speaker;

        var txt :TextField = createTextField();
        txt.width = overlay.getTargetTextWidth();
        txt.autoSize = TextFieldAutoSize.CENTER;

        setText(txt, defaultFmt, texts);
        makeGolden(txt);
        sizeFieldToText(txt);

        _outline = new Shape();
        addChild(_outline);

        addChild(txt);
        _txt = txt;
        var offset :int = overlay.drawBubbleShape(
            //this,
            _outline.graphics, type, txt.width, txt.height);
        txt.x = offset;
        txt.y = offset;
    }

    /**
     * Get the dimensions of the text field.
     */
    public function getTextSize () :Rectangle
    {
        return new Rectangle(0, 0, _txt.width, _txt.height);
    }

    /**
     * Get the bounding box of the bubble in the overlay.
     */
    public function getBubbleBounds () :Rectangle
    {
        return new Rectangle(int(x), int(y), int(_outline.width), int(_outline.height));
    }

    /**
     * Get the bounding box of the bubble, minus any tail.
     */
    public function getBubbleTerritory () :Rectangle
    {
        // TODO
        return getBubbleBounds();
    }

    public function isSpeaker (player :Name) :Boolean
    {
        return (_speaker != null) && _speaker.equals(player);
    }

    public function removeTail () :void
    {
        // the tail is added to our own graphics area
        graphics.clear();
    }

    public function setAgeLevel (overlay :ComicOverlay, ageLevel :int) :void
    {
        // TODO: if we keep, 7 is magic number from ComicOverlay.
        alpha = .5 + (.5 * ((7 - ageLevel) / 7));
//        // re-draw the bubble with the new age level
//        // TODO: ?
//        overlay.drawBubbleShape(
//            graphics, _type, _txt.width, _txt.height, ageLevel);
    }

    /**
     * Make the specified text field have a near-golden ratio.
     */
    protected function makeGolden (txt :TextField) :void
    {
        if (txt.textWidth < MINIMUM_SPLIT_WIDTH) {
            return;
        }

// Commented out: too slow.
//        // This method incrementally decreases the width of the field, 
//        // causing it to re-layout and checking the results.
//        const PAD :int = ChatOverlay.PAD * 2;
//        var w :Number = txt.textWidth;
//        var h :Number = txt.textHeight;
//        var lastRatio :Number = (w + PAD) / (h + PAD);
//        for (var hw :Number = w - 10; hw > 10; hw -= 10) {
//            txt.width = hw + TextFieldUtil.WIDTH_PAD;
//            var ratio :Number = (txt.textWidth + PAD) / (txt.textHeight + PAD);
//            if (Math.abs(ratio - GOLDEN) <= Math.abs(lastRatio - GOLDEN)) {
//                // we're getting closer
//                lastRatio = ratio;
//
//            } else {
//                txt.width = (hw + 10) + TextFieldUtil.WIDTH_PAD;
//                break;
//            }
//        }

        // This method checks the current size and makes guesses as how smaller
        // widths will lay out, and picks the width that puts the overall bubble
        // dimensions in the golden ratio.
        var w :Number = txt.textWidth;
        var h :Number = txt.textHeight;
        const W_PAD :int = ChatOverlay.PAD * 2 + TextFieldUtil.WIDTH_PAD;
        const H_PAD :int = ChatOverlay.PAD * 2 + TextFieldUtil.HEIGHT_PAD;

        var linesToUse :int = txt.numLines;
        var lastRatio :Number = (w + W_PAD) / (h + H_PAD);
        for (var newW :Number = w - 1; newW > 1; newW -= 1) {
            var calcH :Number = Math.ceil(w / newW) * h;
            var ratio :Number = (newW + W_PAD) / (calcH + H_PAD);
            if (Math.abs(ratio - GOLDEN) < Math.abs(lastRatio - GOLDEN)) {
                // we're getting closer
                lastRatio = ratio;

            } else {
                // we're getting further away, the last one was it
                w = newW + 1;
                break;
            }
        }

        txt.width = w + TextFieldUtil.WIDTH_PAD;
    }

    override protected function handleStartExpire (evt :TimerEvent) :void
    {
        removeTail();
        super.handleStartExpire(evt);
    }

    /** The name of the speaker. */
    protected var _speaker :Name;

    /** A reference to our textfield. */
    protected var _txt :TextField;

    protected var _outline :Shape;

    /** The minimum width of a bubble's label before we consider splitting
     * lines. */
    protected static const MINIMUM_SPLIT_WIDTH :int = 90;

    /** The golden ratio. */
    protected static const GOLDEN :Number = (1.0 + Math.sqrt(5.0)) / 2.0;
}
}
