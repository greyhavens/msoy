package {

import flash.display.Bitmap;
import flash.display.BitmapData;
//import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import com.threerings.flash.FrameSprite;

[SWF(width="500", height="500")]
public class DiscoMoire extends FrameSprite
{
    public static const WIDTH :int = 500;
    public static const HEIGHT :int = 500;

    public function DiscoMoire ()
    {
        var dim :Number = Math.max(WIDTH, HEIGHT);

        dim += JIGGLE*2;

        _bursts.push(makeBurst(dim, dim, 3));
        _radians.push(Math.random() * 2 * Math.PI);
        _increments.push(5 * Math.PI/180);
        _bursts.push(makeBurst(dim, dim, 2));
        _radians.push(Math.random() * 2 * Math.PI);
        _increments.push(7 * Math.PI/180);
        _bursts.push(makeBurst(dim, dim, 1));
        _radians.push(Math.random() * 2 * Math.PI);
        _increments.push(3 * Math.PI/180);

        // add all the bursts
        for each (var burst :DisplayObject in _bursts) {
            addChild(burst);
        }

        var mask :Shape = new Shape();
        with (mask.graphics) {
            beginFill(0xFFFFFF);
            drawRect(0, 0, WIDTH, HEIGHT);
            endFill();
        }
        this.mask = mask;
        addChild(mask); // Fuck you very much, flash.
    }

    override protected function handleFrame (... ignored) :void
    {
        for (var ii :int = _bursts.length - 1; ii >= 0; ii--) {
            var radians :Number = Number(_radians[ii]);
            radians += Number(_increments[ii]);
            if (radians > Math.PI * 2) {
                radians -= (Math.PI * 2);
            }
            _radians[ii] = radians;

            var burst :DisplayObject = DisplayObject(_bursts[ii]);
            burst.x = (WIDTH - burst.width)/2 + Math.sin(radians) * JIGGLE;
            burst.y = (HEIGHT - burst.height)/2 + Math.cos(radians) * JIGGLE;
        }
    }

    protected function makeBurst (w :Number, h :Number, degreeIncrement :Number) :DisplayObject
    {
        var s :Sprite = new Sprite();
        s.graphics.lineStyle(1);

        var w2 :Number = w/2;
        var h2 :Number = h/2;

        var radius :Number = .5 * Math.sqrt(w * w + h * h);
        for (var degrees :Number = 0; degrees < 360; degrees += degreeIncrement) {
            s.graphics.moveTo(w2, h2);
            var radians :Number = degrees * Math.PI / 180;

            var x :Number = w2 + Math.sin(radians) * radius;
            var y :Number = h2 + Math.cos(radians) * radius;

            s.graphics.lineTo(x, y);
        }

        // create a bmp, fill it transparently
        var bmp :BitmapData = new BitmapData(w, h, true, 0x00FFFFFF);
        // draw in the lines we made
        bmp.draw(s);
        // then inverse the bitmap..
        for (var xx :int = 0; xx < w; xx++) {
            for (var yy :int = 0; yy < h; yy++) {
                var isTrans :Boolean = ((bmp.getPixel32(xx, yy) & 0xFF000000) == 0);
                bmp.setPixel32(xx, yy, isTrans ? 0xFF000000 : 0x00FFFFFF);
            }
        }

        return new Bitmap(bmp);
    }

    protected var _bursts :Array = [];
    protected var _radians :Array = [];
    protected var _increments :Array = []

    protected static const JIGGLE :int = 40;
}
}
