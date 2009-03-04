//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.geom.Point;

import flash.text.AntiAliasType;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.utils.Timer;
import flash.utils.getTimer; // function import

import com.threerings.util.StringUtil;

import com.threerings.flash.TextFieldUtil;

public class ChatGlyph extends Sprite
{
    public function ChatGlyph (
        overlay :ChatOverlay, type :int, lifetime :int)
    {
        mouseEnabled = false;
        _overlay = overlay;
        _type = type;
        setLifetime(lifetime);
    }

    public function getType () :int
    {
        return _type;
    }

    public function setLifetime (lifetime :int) :void
    {
        var exists :Boolean = _lifetimeTimer != null;
        if (exists) {
            _lifetimeTimer.stop();
        }
        // set up an expire timer, if needed
        if (lifetime != int.MAX_VALUE) {
            _lifetimeTimer = new Timer(lifetime, 1);
            if (!exists) {
                _lifetimeTimer.addEventListener(
                    TimerEvent.TIMER, handleStartExpire, false, 0, true);
            }
            _lifetimeTimer.start();
        }
    }

    /**
     * Force the clickability of the glyph.
     */
    public function setClickable (clickable :Boolean) :void
    {
        _txt.mouseEnabled = clickable;
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
        TextFieldUtil.trackOnlyLinksMouseable(txt);
        txt.multiline = true;
        txt.wordWrap = true;
        txt.selectable = true; // enable copy/paste
        txt.alwaysShowSelection = true; // show selection even when not focused
        txt.antiAliasType = AntiAliasType.ADVANCED;
        TextFieldUtil.trackSingleSelectable(txt);
        return txt;
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

    protected static const FADE_DURATION :int = 600;

    protected var _overlay :ChatOverlay;

    protected var _type :int;

    /** The time to die, fully. Used during fade. */
    protected var _deathStamp :int;

    /** A reference to our textfield. Subclasses are responsible for adding it. */
    protected var _txt :TextField;

    protected var _lifetimeTimer :Timer;
}
}
