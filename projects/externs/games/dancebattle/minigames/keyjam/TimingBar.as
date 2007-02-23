package minigames.keyjam {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.utils.getTimer; // function import
import flash.utils.Timer;

public class TimingBar extends Sprite
{
    public function TimingBar (width :int, height :int, pixelsPerMs :Number)
    {
        _width = width;
        _pixelsPerMs = pixelsPerMs;

        // draw the bar
        with (graphics) {
            // the background
            beginFill(0xFFFFFF);
            drawRect(0, 0, width, height);
            endFill();

            // the border
            lineStyle(2);
            drawRect(0, 0, width, height);

            // the red zones!
            lineStyle(0, 0, 0);
            for (var ww :int = 5; ww >= 1; ww--) {
                beginFill((0xFF / ww) << 16); // redness
                var extent :Number = ww * 3;
                drawRect(width/2 - extent, 0, extent * 2, height);
                endFill();
            }
        }

        // create the needle
        _needle = new Sprite();
        with (_needle.graphics) {
            lineStyle(3, 0x00FF00);
            moveTo(0, 0);
            lineTo(0, height);
        }
        addChild(_needle);
        
        // set up a starting needle position
        _needle.x = Math.random() * width;
        trace("Picked starting position: " + _needle.x);
        _direction = 1;
        _lastStamp = getTimer();

        addEventListener(Event.ENTER_FRAME, repositionNeedle);
    }

    /**
     * Stop the needle, and return the nearness to the center,
     * expressed as a value from 0 - 1.
     */
    public function stopNeedle () :Number
    {
        removeEventListener(Event.ENTER_FRAME, repositionNeedle);
        repositionNeedle(); // one last update
        return 1 - (Math.abs(_width/2 - _needle.x) / (_width/2));
    }

    /**
     * Fade out after we're no longer important.
     */
    public function fadeOut () :void
    {
        _fadeTimer = new Timer(200);
        _fadeTimer.addEventListener(TimerEvent.TIMER, handleFadeTick);
        _fadeTimer.start();
    }

    /**
     * Repositon the needle, given the current timestmap.
     * This should always be done before querying the needle accuracy.
     */
    protected function repositionNeedle (event :Object = null) :void
    {
        var curStamp :Number = getTimer();
        var elapsed :Number = curStamp - _lastStamp;

        var pixelsToMove :Number = _pixelsPerMs * elapsed;
        trace("pixelsToMove: " + pixelsToMove);
        var newX :Number = _needle.x + (_direction * pixelsToMove);
        while (newX < 0 || newX > _width) {
            _direction *= -1;
            // fold it over
            if (newX < 0) {
                newX = 0 - newX;
            } else {
                newX = (_width * 2) - newX;
            }
        }
        trace("Repositioning to " + newX);
        _needle.x = newX;

        // finally,
        _lastStamp = curStamp;
    }

    protected function handleFadeTick (event :TimerEvent) :void
    {
        this.alpha = Math.max(0, this.alpha - (1/25));
        if (parent == null || alpha == 0) {
            _fadeTimer.stop();
            if (parent != null) {
                parent.removeChild(this);
            }
        }

        event.updateAfterEvent(); // flash blows
    }

    protected var _width :Number;

    protected var _pixelsPerMs :Number;

    protected var _needle :Sprite;

    protected var _lastStamp :Number;

    protected var _direction :int;

    /** A timer to fade this out. */
    protected var _fadeTimer :Timer;
}
}
