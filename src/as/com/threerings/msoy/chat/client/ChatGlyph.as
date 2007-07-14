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

import com.threerings.flash.TextFieldUtil;

public class ChatGlyph extends Sprite
{
    public function ChatGlyph (
        overlay :ChatOverlay, type :int, lifetime :int)
    {
        mouseEnabled = false;
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
        TextFieldUtil.trackSingleSelectable(txt);
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
        var hasLinks :Boolean = false;
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
                    if (fmt.url != null) {
                        hasLinks = true;
                    }
                }
                fmt = null;
                length = newLength;
            }
        }

        txt.mouseEnabled = hasLinks;
    }

    /**
     * After the text is set and positioned, and any desired
     * adjustments have been made, this "bakes-in" the text size
     * into the TextField's width/height.
     */
    protected function sizeFieldToText (txt :TextField) :void
    {
        txt.autoSize = TextFieldAutoSize.NONE;
        // --> No shit, this is how you do it. These values are entirely
        // missing from the public API anywhere. They are available
        // in mx.controls.UITextField::mx_internal.TEXT_WIDTH_PADDING,
        // but I'd like to not depend on that.
        // This is so typical of the total lack of respect Adobe has
        // for their developers: everyone is left to guess these on their own.
        txt.width = txt.textWidth + TextFieldUtil.WIDTH_PAD;
        txt.height = txt.textHeight + TextFieldUtil.HEIGHT_PAD;
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
