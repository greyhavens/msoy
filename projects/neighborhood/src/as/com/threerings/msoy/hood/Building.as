package com.threerings.msoy.hood {

import flash.display.MovieClip;
import flash.display.Sprite;
import flash.display.DisplayObject;
import flash.events.Event;
import flash.geom.Point;
import flash.utils.ByteArray;

import com.threerings.util.EmbededClassLoader;


/**
 * Manages the assets of a single building type.
 */
public class Building extends Sprite
{
    /** The number of variations (i.e. the number of frames in houseTile/shapeTile. */
    public var variationCount :int;

    public function Building (house :Class, populate :Class, soy :Class)
    {
        _houseClass = house;
        _populate = new populate();
        _soyClass = soy;

        var houseClip :MovieClip = new house();
        var populateClip :MovieClip = new populate();
        variationCount = houseClip.totalFrames;
        if (variationCount != populateClip.totalFrames) {
            throw new Error("Frame count mismatch between house and populate clips [" +
                variationCount + "/" + populateClip.totalFrames + "]");
        }
    }

    public function newHouse (frame :int) :MovieClip
    {
        var clip :MovieClip =  (new _houseClass()) as MovieClip;
        clip.gotoAndStop(frame);
        return clip;
    }

    public function findRandomPopulatePoint (frame :int) :Point
    {
        this.addChild(_populate);
        _populate.gotoAndStop(frame);
        var x :Number, y :Number;
        var cnt :int = 1000;
        do {
            x = Math.random() * _populate.width;
            y = Math.random() * _populate.height;
            var p :Point = new Point(x, y);
            p = _populate.localToGlobal(p);
            if (_populate.hitTestPoint(p.x, p.y, true)) {
                this.removeChild(_populate);
                return new Point(x, y);
            }
        } while (--cnt > 0);
        throw new Error("Couldn't find a spot to place a soy in 1,000 iterations!");
    }

    public function getPopulatedTile (frame :int, population :int) :MovieClip
    {
        var house :MovieClip = newHouse(frame);
        while (--population >= 0) {
            var p :Point = findRandomPopulatePoint(frame);
            var soy :DisplayObject = new _soyClass();
            house.addChild(soy);
            soy.x = p.x;
            soy.y = p.y;
        }
        return house;
    }

    protected var _houseClass :Class;
    protected var _populate :MovieClip;
    protected var _soyClass :Class;
}
}