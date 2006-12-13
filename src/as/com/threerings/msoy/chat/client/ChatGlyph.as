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
    public function ChatGlyph (
        overlay :ChatOverlay, type :int, expireDuration :int)
    {
        _overlay = overlay;
        _type = type;

        // set up an expire timer, if needed
        if (expireDuration != int.MAX_VALUE) {
            var timer :Timer = new Timer(expireDuration, 1);
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
        _deathStamp = getTimer() + FADE_DURATION;
        addEventListener(Event.ENTER_FRAME, handleFadeStep, false, 0, true);
    }

    protected function handleFadeStep (evt :Event) :void
    {
        var left :int = _deathStamp - getTimer();
        if (left > 0) {
            alpha = (left / FADE_DURATION);

        } else {
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
