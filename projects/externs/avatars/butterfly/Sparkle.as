package {

import flash.display.Sprite;

import flash.events.Event;

import flash.filters.GlowFilter;

import flash.utils.getTimer;

import com.threerings.flash.Siner;

public class Sparkle extends Sprite
{
    public function Sparkle (x :Number, y :Number, yMax :Number)
    {
        _x = x;
        _y = y;
        _yMax = yMax;

        this.x = _x;
        this.y = _y;

        _color = uint(COLORS[int(Math.random() * COLORS.length)]);

        _glow = new GlowFilter(_color, 1, 0, 0)
        _glow.strength = 5;

        addEventListener(Event.ENTER_FRAME, enterFrame);
        _stamp = getTimer();
        _glowSiner = new Siner(16, 2.2);
        _glowSiner.reset();
        _rotSiner = new Siner(180 * Math.random(), Math.random(),
            180 * Math.random(), Math.random() * 2);
        _rotSiner.randomize();
        _pointySiner = new Siner(6, 1);

        enterFrame();
    }

    public function adjustY (dy :Number) :void
    {
        y += dy;
        _y += dy;
        _yMax += dy;
    }

    protected function enterFrame (event :Event = null) :void
    {
        var elapsed :Number = (getTimer() - _stamp) / 1000;
        var blur :Number = 16 + _glowSiner.value;
        var point :Number = 8 + _pointySiner.value;
        if (_falling) {
            // we keep newY in an unmolested variable, assigning
            // things that are CLOSE to whole pixel values into
            // the display object location causes them to be rounded!
            var newY :Number = Math.min(_yMax, _y + (-30 * elapsed) + (10 * (elapsed * elapsed)));
            y = newY;
            if (newY >= _yMax) {
                _falling = false;
                _stamp = getTimer();

            } else if (elapsed < 1) {
                point *= elapsed;
                blur *= elapsed;
            }
            this.rotation = _rotSiner.value;

        } else {
            if (elapsed < 1) {
                point *= (1 - elapsed);
                blur *= (1 - elapsed);
            } else {
                // remove ourselves, end it
                this.parent.removeChild(this);
                removeEventListener(Event.ENTER_FRAME, enterFrame);
                return;
            }
        }

        with (graphics) {
            clear();
            beginFill(_color);
            moveTo(0, -point);
            curveTo(1, -1, point, 0);
            curveTo(1, 1, 0, point);
            curveTo(-1, 1, -point, 0);
            curveTo(-1, -1, 0, -point);
            endFill();
        }
        _glow.blurX = blur;
        _glow.blurY = blur;
        this.filters = [ _glow ];
    }

    protected var _x :Number;
    protected var _y :Number;
    protected var _yMax :Number;
    protected var _color :uint;

    protected var _glowSiner :Siner;
    protected var _rotSiner :Siner;
    protected var _pointySiner :Siner;
    protected var _stamp :Number;

    protected var _falling :Boolean = true;

    protected var _glow :GlowFilter;

    protected static const COLORS :Array = [ 
        0xFF00EE, //pink
        0xFF0400, //red
        0xFFFB00, //yellow
        0x00FFF2, // cyan
        //0xFF8C00, //orange
        0x002BFF, //blue
        0x04FF00 //green
    ];
}
}
