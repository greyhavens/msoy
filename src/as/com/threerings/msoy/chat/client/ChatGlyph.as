//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.utils.Timer;
import flash.utils.getTimer; // function import

public class ChatGlyph extends Sprite
{
    public function ChatGlyph (
        overlay :ChatOverlay, type :int, lifetime :int)
    {
        _overlay = overlay;
        _type = type;

        // set up an expire timer, if needed
        if (lifetime != int.MAX_VALUE) {
            // TODO: possibly have the overlay manage all this with
            // just one Timer
            var timer :Timer = new Timer(lifetime, 1);
            timer.addEventListener(
                TimerEvent.TIMER, handleStartExpire, false, 0, true);
            timer.start();
        }
    }

    public function getType () :int
    {
        return _type;
    }

    /**
     * Called back from the overlay if we were removed before our time.
     */
    internal function wasRemoved () :void
    {
        // in case we've started listening to this, stop
        removeEventListener(Event.ENTER_FRAME, handleFadeStep);
    }

    /**
     * Create the text field for holding the text.
     */
    protected function createTextField () :TextField
    {
        var txt :TextField = new TextField();
        txt.multiline = true;
        txt.wordWrap = true;
        txt.selectable = true; // enable copy/paste
        txt.alwaysShowSelection = true; // show selection even when not focused
        return txt;
    }

    /**
     * Populate the TextField with the specified formatted strings.
     *
     * @param texts A mixed array of String and TextFormat objects, with
     * each String being rendered in the TextFormat preceding it, or the
     * default format if not preceded by a TextFormat.
     */
    protected function setText (
        txt :TextField, defaultFmt :TextFormat, texts :Array) :void
    {
        var fmt :TextFormat = null;
        var length :int = 0;
        for each (var o :Object in texts) {
            if (o is TextFormat) {
                fmt = (o as TextFormat);

            } else {
                // Note: we should just be able to set the defaultFormat
                // for the entire field and then format the different
                // stretches, but SURPRISE! It doesn't quite work right,
                // so we format every goddamn piece of the text by hand.
                var append :String = String(o);
                var newLength :int = length + append.length;
                txt.appendText(append);
                if (fmt == null) {
                    fmt = defaultFmt;
                }
                if (length != newLength) {
                    txt.setTextFormat(fmt, length, newLength);
                }
                fmt = null;
                length = newLength;
            }
        }

        // restrain the size to the used size
        // TODO: Research this more? As far as I can tell, everyone
        // in the world just adds a fudge factor to the supposedly
        // correct sizes. I tried figuring out where this value comes
        // from, but all the formatting properties I'm using with the
        // TextField have no indent/margin/etc.
/*
        o = txt.getTextFormat(); //txt.defaultTextFormat;
        trace("default: " + o.align + ", " + o.blockIndent +
            ", " + o.indent + ", " + o.kerning + ", " + o.leading + ", " +
            o.leftMargin + ", " + o.rightMargin + ", " + o.letterSpacing);
        txt.width = txt.textWidth;
        txt.height = txt.textHeight;
        */
        txt.autoSize = TextFieldAutoSize.NONE;
        const FUDGE :int = 5;
        txt.width = txt.textWidth + FUDGE;
        txt.height = txt.textHeight + FUDGE;
    }

    protected function handleStartExpire (evt :TimerEvent) :void
    {
        if (parent) {
            _deathStamp = getTimer() + FADE_DURATION;
            addEventListener(Event.ENTER_FRAME, handleFadeStep, false, 0, true);
        }
    }

    protected function handleFadeStep (evt :Event) :void
    {
        var left :int = _deathStamp - getTimer();
        if (left > 0) {
            alpha = (left / FADE_DURATION);

        } else {
            removeEventListener(Event.ENTER_FRAME, handleFadeStep);
            _overlay.glyphExpired(this);
        }
    }

    protected var _overlay :ChatOverlay;

    protected var _type :int;

    /** The time to die, fully. Used during fade. */
    protected var _deathStamp :int;

    protected static const FADE_DURATION :int = 600;
}
}
