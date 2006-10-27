//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.events.Event;

import flash.geom.Rectangle;

public class Perspectivizer extends Bitmap
{
    public function Perspectivizer (source :DisplayObject)
    {
        super();

        // TODO
        bitmapData = new BitmapData(WIDTH, HEIGHT, true, 0);

        _source = source;

        addEventListener(Event.ENTER_FRAME, enterFrame);
        updateDisplayedImage();
    }

    protected function updateDisplayedImage () :void
    {
        var r :Rectangle = _source.getBounds(_source);
        //trace("Rect is " + r);

        var ww :int = int(r.width + r.x); //int(_source.width);// + 100;
        var hh :int = int(r.height + r.y); //int(_source.height);// + 200;

        // first, see if our internal bitmapdata needs updating
        if (_sourcePixels == null || (ww != _sourcePixels.width) ||
                (hh != _sourcePixels.height)) {
            if (_sourcePixels != null) {
                _sourcePixels.dispose();
                _sourcePixels = null;
            }
            if (ww < 1 || hh < 1) {
                return;
            }
            _sourcePixels = new BitmapData(ww, hh, true, 0);
            trace("perspective source's dims: (" + ww + ", " + hh + ")");

        } else {
            // clear the pixels
            _sourcePixels.fillRect(new Rectangle(0, 0, ww, hh), 0x00000000);
        }

        // copy the source into _sourcePixels
        _sourcePixels.draw(_source);

        // sample pixels out of _sourcePixels
        // TODO: 
        for (var yy :int = 0; yy < HEIGHT; yy++) {
            var sy :int = int(Math.round((yy / HEIGHT) * hh));
            for (var xx :int = 0; xx < WIDTH; xx++) {
                var sx :int = int(Math.round((xx / WIDTH) * ww));
                bitmapData.setPixel32(xx, yy, _sourcePixels.getPixel32(sx, sy));
            }
        }
    }

    protected function enterFrame (event :Event) :void
    {
        updateDisplayedImage();
    }

    /** The source display object. */
    protected var _source :DisplayObject;

    /** The current pixels of _source at its native size. */
    protected var _sourcePixels :BitmapData;

    protected const WIDTH :int = 300;
    protected const HEIGHT :int = 300;
}
}
