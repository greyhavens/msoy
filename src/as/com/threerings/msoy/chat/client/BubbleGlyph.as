//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.BlendMode;

import flash.filters.BevelFilter;
import flash.filters.BitmapFilterQuality;

import flash.geom.Rectangle;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Name;

import com.threerings.flash.TextFieldUtil;

import com.threerings.msoy.utils.TextUtil;

public class BubbleGlyph extends ChatGlyph
{
    public function BubbleGlyph (
        overlay :ComicOverlay, type :int, lifetime :int,
        speaker :Name, defaultFmt :TextFormat, texts :Array)
    {
        super(overlay, type, lifetime);
        _speaker = speaker;
        blendMode = BlendMode.LAYER;

        if (speaker != null) {
            filters = [new BevelFilter(5, 100, 0xFFFFFF, 1.0, 0, 1.0, 50, 50, 1,
                                       BitmapFilterQuality.MEDIUM)];
        }

        var txt :TextField = createTextField();
        txt.width = overlay.getTargetTextWidth();
        txt.autoSize = TextFieldAutoSize.CENTER;

        TextUtil.setText(txt, texts, defaultFmt);
        // mouse enabled will get turned on when/if the mouse is hovering over an actual URL.
        txt.mouseEnabled = false;
        makeGolden(txt);
        sizeFieldToText(txt);

        addChild(_txt = txt);
        var offset :int = overlay.drawBubbleShape(
            graphics, type, txt.width, txt.height, true);
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
        return new Rectangle(int(x), int(y), int(width), int(height));
    }

    public function isSpeaker (player :Name) :Boolean
    {
        return (_speaker != null) && _speaker.equals(player);
    }

    public function getSpeaker () :Name
    {
        return _speaker;
    }

    public function removeTail () :void
    {
        (_overlay as ComicOverlay).drawBubbleShape(
            graphics, _type, _txt.width, _txt.height, false);
    }

    public function setAgeLevel (ageLevel :int) :void
    {
        ageLevel = Math.min(ageLevel, 7);
        // TODO: if we keep, 7 is magic number from ComicOverlay.
        alpha = .5 + (.5 * ((7 - ageLevel) / 7));
    }

    /**
     * Make the specified text field have a near-golden ratio.
     */
    protected function makeGolden (txt :TextField) :void
    {
        if (txt.textWidth < MINIMUM_SPLIT_WIDTH) {
            return;
        }

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

    /** The name of the speaker. */
    protected var _speaker :Name;

    /** The minimum width of a bubble's label before we consider splitting
     * lines. */
    protected static const MINIMUM_SPLIT_WIDTH :int = 90;

    /** The golden ratio. */
    protected static const GOLDEN :Number = (1.0 + Math.sqrt(5.0)) / 2.0;
}
}
