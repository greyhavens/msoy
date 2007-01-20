//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.text.TextField;

import flash.utils.Timer;
import flash.utils.getTimer; // function import

public class ChatGlyph extends Sprite
{
    /** The index of the ChatMessage corresponding to this glyph in the
     * HistoryList. */
    public var histIndex :int;

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
