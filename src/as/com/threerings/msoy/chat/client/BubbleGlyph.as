package com.threerings.msoy.chat.client {

import flash.geom.Rectangle;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Name;

public class BubbleGlyph extends ChatGlyph
{
    public function BubbleGlyph (
        overlay :ComicOverlay, type :int, lifetime :int,
        speaker :Name, defaultFmt :TextFormat, texts :Array)
    {
        super(overlay, type, lifetime);
        _speaker = speaker;

        var txt :TextField = createTextField();
        txt.width = 400; // TODO
        txt.autoSize = TextFieldAutoSize.CENTER;

        setText(txt, defaultFmt, texts);
        makeGolden(txt);
        sizeFieldToText(txt);

        addChild(txt);
        _txt = txt;
        var offset :int = overlay.drawBubbleShape(
            graphics, type, txt.width, txt.height);
        txt.x = offset;
        txt.y = offset;
    }

    /**
     * Get the dimensions of the text field.
     */
    public function getTextSize () :Rectangle
    {
        return new Rectangle(0, 0, _txt.textWidth, _txt.textHeight);
    }

    /**
     * Get the bounding box of the bubble in the overlay.
     */
    public function getBubbleBounds () :Rectangle
    {
        return new Rectangle(int(x), int(y), int(width), int(height));
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
        // TODO
    }

    public function setAgeLevel (ageLevel :int) :void
    {
        // TODO
    }

    /**
     * Make the specified text field have a near-golden ratio.
     */
    protected function makeGolden (txt :TextField) :void
    {
        if (txt.textWidth < MINIMUM_SPLIT_WIDTH) {
            return;
        }

//        var linesToUse :int = 1;
//        var lastRatio = (txt.textWidth + ChatOverlay.PAD * 2) /
//                        (txt.textHeight + ChatOverlay.PAD * 2);
//        for (var lines :int = 2; true; lines++) {
//            var ratio : ((txt.textWidth / lines) + (ChatOverlay.PAD * 2)) /
//                ((txt.textHeight * lines) + (ChatOverlay.PAD * 2));
//            if (Math.abs(ratio - GOLDEN) < Math.abs(lastRatio - GOLDEN)) {
//                // we're getting closer
//                lastRatio = ratio;
//
//            } else {
//                // we're getting further away, the last one was it
//                linesToUse = (lines - 1);
//                break;
//            }
//        }

        // TODO: god, do we really want to try tweaking the textfield's
        // width and trying to make it golden?

        // for right now, if text all fits on one line, make the width 200
        if (txt.numLines == 1) {
            txt.width = 200;
        }
    }

    /** The name of the speaker. */
    protected var _speaker :Name;

    /** A reference to our textfield. */
    protected var _txt :TextField;

    /** The minimum width of a bubble's label before we consider splitting
     * lines. */
    protected static const MINIMUM_SPLIT_WIDTH :int = 90;

    /** The golden ratio. */
    protected static const GOLDEN :Number = (1.0 + Math.sqrt(5.0)) / 2.0;
}
}
