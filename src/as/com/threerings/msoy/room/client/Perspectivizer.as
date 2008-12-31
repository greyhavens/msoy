//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.events.Event;

import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.Log;

public class Perspectivizer extends Bitmap
{
    public function Perspectivizer (
        source :DisplayObject, perspInfo :PerspInfo = null,
        mediaScaleX :Number = 1, mediaScaleY :Number = 1)
    {
        super();
        _source = source;

        if (perspInfo != null) {
            updatePerspInfo(perspInfo, mediaScaleX, mediaScaleY);
        }
//        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    public function getSource () :DisplayObject
    {
        return _source;
    }

    public function getHotSpot () :Point
    {
        return (_info != null) ? _info.hotSpot : new Point(0, 0);
    }

    public function updatePerspInfo (
        perspInfo :PerspInfo, mediaScaleX :Number, mediaScaleY :Number) :void
    {
        _info = perspInfo;
        _mediaScaleX = mediaScaleX;
        _mediaScaleY = mediaScaleY;

        var ww :int = 1 + Math.round(Math.abs(_info.p0.x - _info.pN.x));
        var hh :int = 1 + Math.round(Math.max(
            _info.p0.y + _info.height0, _info.pN.y + _info.heightN));

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

    override public function toString () :String
    {
        return "Perspectivizer:" + _source;
    }

    protected function updateDisplayedImage () :void
    {
        if (_destPixels == null) {
            return;
        }

        var r :Rectangle = _source.getBounds(_source);

        var ww :int = int(Math.round(Math.abs(_mediaScaleX) * (r.width + r.x)));
        var hh :int = int(Math.round(Math.abs(_mediaScaleY) * (r.height + r.y)));

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

        // set up the transformation matrix
        var m :Matrix = new Matrix(1, 0, 0, 1,
            (_mediaScaleX < 0) ? -r.width : 0,
            (_mediaScaleY < 0) ? -r.height : 0);
        m.scale(_mediaScaleX, _mediaScaleY);

        // copy the source into _sourcePixels
        _sourcePixels.draw(_source, m); //, null, null, r);

        var x0 :Number = _info.p0.x;
        var y0 :Number = _info.p0.y;
        var height0 :Number = _info.height0;
        var xN :Number = _info.pN.x;
        var yN :Number = _info.pN.y;
        var heightN :Number = _info.heightN;
        var yy :int;

        // sample pixels out of _sourcePixels into the bitmap data
        // TODO: this is wrong, because I'm progressing from
        // the 0 to the N scale smoothly. What we should do is
        // just have a ref to the RoomView (or some layer-outter)
        // and check the source X pixel column for each projected X pixel col.
        // TODO
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

    protected var _info :PerspInfo;

    protected var _mediaScaleX :Number;
    protected var _mediaScaleY :Number;

//    protected var _room :RoomView;

    protected const WIDTH :int = 300;
    protected const HEIGHT :int = 300;
}
}
