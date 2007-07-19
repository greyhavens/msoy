//
// $Id$

package com.threerings.msoy.hood {

import flash.display.MovieClip;
import flash.display.Sprite;
import flash.display.DisplayObject;
import flash.events.Event;
import flash.geom.Point;
import flash.utils.ByteArray;


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
        _populateClass = populate;
        _soyClass = soy;

        var houseClip :MovieClip = new house();
        var populateClip :MovieClip = new populate();
        variationCount = houseClip.totalFrames;
        if (variationCount != populateClip.totalFrames) {
            throw new Error("Frame count mismatch between house and populate clips [" +
                variationCount + "/" + populateClip.totalFrames + "]");
        }
    }

    public function getPopulatedTile (frame :int, friendPop :int, otherPop :int) :MovieClip
    {
        var house :MovieClip = newHouse(frame);
        var populate :MovieClip = new _populateClass();
        this.addChild(populate);
        house.gotoAndStop(frame);
        populate.gotoAndStop(frame);
        for (var ii :int = 0; ii < friendPop + otherPop; ii ++) {
            var p :Point = findRandomPopulatePoint(populate);
            if (p != null) {
                var soy :MovieClip = new _soyClass();
                // friends are represented by the first frame, others by the second
                soy.gotoAndStop(ii < friendPop ? 1 : 2);
                house.addChild(soy);
                soy.x = p.x;
                soy.y = p.y;
            }
        }
        this.removeChild(populate);
        return house;
    }

    protected function newHouse (frame :int) :MovieClip
    {
        var clip :MovieClip =  (new _houseClass()) as MovieClip;
        clip.gotoAndStop(frame);
        return clip;
    }

    protected function findRandomPopulatePoint (populate :MovieClip) :Point
    {
        var x :Number, y :Number;
        var cnt :int = 10000;
        do {
            x = -populate.width + Math.random() * populate.width * 2;
            y = -populate.height + Math.random() * populate.height * 2;
            var p :Point = new Point(x, y);
            p = populate.localToGlobal(p);
            if (populate.hitTestPoint(p.x, p.y, true)) {
                return p;
            }
        } while (--cnt > 0);
        throw new Error("Couldn't find a spot to place a soy in 1,000 iterations!");
    }

    protected var _houseClass :Class;
    protected var _populateClass :Class;
    protected var _soyClass :Class;
}
}
