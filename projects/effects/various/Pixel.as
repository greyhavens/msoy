//
// $Id$

package {

import flash.display.Bitmap;
import flash.display.BitmapData;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.flash.FrameSprite;

[SWF(width="1", height="1")]
public class Pixel extends FrameSprite
{
    public function Pixel ()
    {
        // draw a goddamn blank pixel so that we even know how big we should be...
//        graphics.beginFill(0x000000, 0);
//        graphics.drawRect(0, 0, 1, 1);
//        graphics.endFill();

        _data = new BitmapData(1, 1, true, 0x00000000);
        _bmp = new Bitmap(_data);
        addChild(_bmp);
    }

    override protected function handleFrame (... ignored) :void
    {
        /*
        trace("My bounds are " + localToGlobal(new Point(0, 0)) + " to " +
            localToGlobal(new Point(1, 1)) + " and my scales are " + scaleX + ", " + scaleY);
        */

        var r :Rectangle = transform.pixelBounds;
        var w :int = r.width;
        var h :int = r.height;

        if (w > 0 && h > 0 && (w != _data.width || h != _data.height)) {
            trace("Noticed new size: " + w + ", " + h);
            removeChild(_bmp);
            _bmp = null;
            _data = new BitmapData(w, h, true, 0x00000000);

            // draw in the bitmap?
            for (var ii :int = 0; ii < 1200; ii++) {
                _data.setPixel32(Math.random() * w, Math.random() * h,
                    uint(Math.random() * 0xFFFFFF) | 0xFF000000);
            }

            // and then set up the backwards scaling...
            trace("Calc'd new scales as " + (1/w) + " and " + (1/h));
            _bmp = new Bitmap(_data);
            _bmp.scaleX = 1/w;
            _bmp.scaleY = 1/h;
            addChild(_bmp);
        }

//        trace("My stage bounds are: " + getBounds(stage));
//        trace("My pixel bounds are: " + transform.pixelBounds);
    }

    protected var _bmp :Bitmap;

    protected var _data :BitmapData;
}
}
