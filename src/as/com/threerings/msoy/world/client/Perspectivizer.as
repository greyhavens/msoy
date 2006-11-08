//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.events.Event;

import flash.geom.Matrix;
import flash.geom.Rectangle;

public class Perspectivizer extends Bitmap
{
    public function Perspectivizer (
        source :DisplayObject,
        perspInfo :Array, mediaScaleX :Number, mediaScaleY :Number)
    {
        super();
        _source = source;

        updatePerspInfo(perspInfo, mediaScaleX, mediaScaleY);
        //addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    public function getSource () :DisplayObject
    {
        return _source;
    }

    public function updatePerspInfo (
        perspInfo :Array, mediaScaleX :Number, mediaScaleY :Number) :void
    {
        _info = perspInfo;
        _mediaScaleX = mediaScaleX;
        _mediaScaleY = mediaScaleY;

        var ww :int = 1 + Math.round(Math.abs(
            Number(_info[0]) - Number(_info[3])));
        var hh :int = 1 + Math.round(Math.max(
            Number(_info[1]) + Number(_info[2]),
            Number(_info[4]) + Number(_info[5])));

        if (ww < 1 || hh < 1) {
            return;

        } else if (ww > 2880 || hh > 2880) {
            Log.getLog(this).warning("Bitmap data too large: " +
                "(" + ww + ", " + hh + ")");
            return;
        }

        if (_destPixels == null || (ww != _destPixels.width) ||
                (hh != _destPixels.height)) {
            if (_destPixels != null) {
                _destPixels.dispose();
            }
            _destPixels = new BitmapData(ww, hh, true, 0);
            bitmapData = _destPixels;
        }
        updateDisplayedImage();
    }

    protected function updateDisplayedImage () :void
    {
        if (_destPixels == null) {
            return;
        }

        var r :Rectangle = _source.getBounds(_source);

        var ww :int = int(Math.round(_mediaScaleX * (r.width + r.x)));
        var hh :int = int(Math.round(_mediaScaleY * (r.height + r.y)));

        //trace("source ww/hh : " + ww + ", " + hh);

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
            //trace("perspective source's dims: (" + ww + ", " + hh + ")");

        } else {
            // clear the pixels
            _sourcePixels.fillRect(new Rectangle(0, 0, ww, hh), 0x00000000);
        }

        // copy the source into _sourcePixels
        //_sourcePixels.draw(_source, null, null, null, r);
        _sourcePixels.draw(_source,
            new Matrix(_mediaScaleX, 0, 0, _mediaScaleY), null, null, r);

        var x0 :Number = _info[0];
        var y0 :Number = _info[1]
        var height0 :Number = _info[2];
        var xN :Number = _info[3];
        var yN :Number = _info[4];
        var heightN :Number = _info[5];
        var yy :int;

        // sample pixels out of _sourcePixels into the bitmap data
        for (var xx :int = 0; xx < _destPixels.width; xx++) {
            var percX :Number = (xx / _destPixels.width);
            var iPerc :Number = 1 - percX;
            var sx :int = int(Math.round(percX * ww));
            var dx :int = int(Math.round(percX * xN + iPerc * x0));
            var heightHere :Number = (percX * heightN) + (iPerc * height0);
            var firstY :Number = (percX * yN) + (iPerc * y0);
            // clear any unused pixels above the strip
            for (yy = 0; yy < firstY; yy++) {
                _destPixels.setPixel32(dx, yy, 0x00000000);
            }
            // fill in a vertical strip in the destination
            for (yy = 0; yy < heightHere; yy++) {
                var dy :int = yy + firstY;
                var sy :int = int(Math.round(yy / heightHere * hh)); 

                _destPixels.setPixel32(dx, dy, _sourcePixels.getPixel32(sx, sy));
            }
            // clear any unused pixels below the strip
            for (yy = firstY + heightHere; yy < _destPixels.height; yy++) {
                _destPixels.setPixel32(dx, yy, 0x00000000);
            }
        }

/* Simple scaling
        for (var yy :int = 0; yy < _destPixels.height; yy++) {
            var sy :int = int(Math.round((yy / HEIGHT) * hh));
            for (var xx :int = 0; xx < _destPixels.width; xx++) {
                var sx :int = int(Math.round((xx / WIDTH) * ww));
                _destPixels.setPixel32(xx, yy, _sourcePixels.getPixel32(sx, sy));
            }
        }
*/
    }

    protected function outputStats () :void
    {
        var r :Rectangle = _source.getBounds(_source);
        trace("Source bounds: " + r);

        trace("source dims: " + _source.width + ", " + _source.height);
    }

    protected function enterFrame (event :Event) :void
    {
        updateDisplayedImage();
    }

    /** The source display object. */
    protected var _source :DisplayObject;

    /** The current pixels of _source at its native size. */
    protected var _sourcePixels :BitmapData;

    protected var _destPixels :BitmapData;

    protected var _info :Array;

    protected var _mediaScaleX :Number;
    protected var _mediaScaleY :Number;

//    protected var _room :AbstractRoomView;

    protected const WIDTH :int = 300;
    protected const HEIGHT :int = 300;
}
}
