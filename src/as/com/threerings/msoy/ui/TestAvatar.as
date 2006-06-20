package com.threerings.msoy.ui {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Sprite;

import flash.events.TimerEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.events.Event;

import flash.utils.Timer;

import com.threerings.util.Util;

// TODO:
// This is a test to see if I could fuck around and make a translucent
// avatar that sampled its background.
// I'll probably continue to use this class to test things with avatars.
//
public class TestAvatar extends Sprite
{
    public function TestAvatar ()
    {
        super();
        addEventListener(Event.ENTER_FRAME, enteringFrame);
        width = 150;
        height = 300;

/*
        graphics.clear();
        graphics.moveTo(0, 0);
        graphics.beginFill(0xFF0000);
        graphics.lineTo(150, 0);
        graphics.lineTo(150, 300);
        graphics.lineTo(0, 0);
        graphics.endFill();
        */

        var t :Timer = new Timer(50);
        t.addEventListener(TimerEvent.TIMER, function (evt :TimerEvent) :void {
            width = 150;
            height = 300;
        });
        t.start();
    }

    protected function enteringFrame (event :Event) :void
    {
        if (stage == null) {
            return;
        }
        _counter++;
        if (_counter % 2 == 0) {
            return;
        }

        var p0 :Point = new Point(0, 0);
        var p :Point = localToGlobal(p0);

        var w :int = width;
        var h :int = height;
        if (w <= 0) {
            w = 150;
        }
        if (h <= 0) {
            h = 150;
        }

        while (numChildren > 0) {
            removeChildAt(0);
        }
        graphics.clear();

        var src :BitmapData = new BitmapData(w, h, false, 0xFFFFFF);
        var dest :BitmapData = new BitmapData(w, h, false, 0xFFFFFF);
        var r :Rectangle = new Rectangle(p.x, p.y, w, h);
        //src.draw(stage, null, null, null, r);
        src.draw(stage);

/*
        var xp :int;
        var yp :int;
        var allZero :Boolean = true;
        for (var yy :int = 0; yy < src.height; yy++) {
            for (var xx :int = 0; xx < src.width; xx++) {
                var report :Boolean = (Math.random() < .0001);
                p = Point.polar(
                    Math.random() * 16, Math.random() * Math.PI * 2);
                xp = Math.max(0, Math.min(src.width, xx + p.x));
                yp = Math.max(0, Math.min(src.height, yy + p.y));
                var pixel :uint = src.getPixel(xp, yp);
                if (pixel != 0) {
                    allZero = false;
                }
                if (report) {
                    trace("Setting (" + xx + ", " + yy + ") to " + 
                        Util.toHex(pixel) + " from (" + xp + ", " + yp + ")");
                }
                dest.setPixel(xx, yy, pixel);
            }
        }
        trace("allZero: " + allZero);
*/


        var pixel :uint;
        for (var yy :int = 0; yy < src.height; yy++) {
            for (var xx :int = 0; xx < src.width; xx++) {
                pixel = src.getPixel(xx, yy);
                pixel = (fuxorColor((pixel >> 16) & 0xFF) << 16) |
                        (fuxorColor((pixel >> 8) & 0xFF) << 8) |
                        (fuxorColor((pixel) & 0xFF));
                dest.setPixel(xx, yy, pixel);
            }
        }

/*
        var reds :Array = new Array();
        var greens :Array = new Array();
        var blues :Array = new Array();

        for (var ii :int = 0; ii < 256; ii++) {
            var val :uint = fuxorColor(ii);
            blues.push(val);
            greens.push(val << 8);
            reds.push(val << 16);
        }

        r.x = 0;
        r.y = 0;

        src.paletteMap(src, r, p0, reds, greens, blues);
        */

        _bit = dest;

        graphics.beginBitmapFill(dest);
        graphics.drawRect(0, 0, w, h);
        graphics.endFill();

        graphics.beginFill(0xFFFF00);
        graphics.drawCircle(15, 15, 10);
        graphics.endFill();
    }

    protected function fuxorColor (val :uint) :uint
    {
        return Math.min(255, Math.max(0,
            val + int(Math.random() * 32) - 16));
    }

    protected var _bit :BitmapData;

    protected var _counter :int = 0;
}
}
