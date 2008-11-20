//
// $Id$

package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import mx.controls.Image;
import mx.containers.HBox;

import com.threerings.util.Command;

import com.threerings.msoy.client.Msgs;

[Event(name="starClick", type="StarsEvent")]
[Event(name="starOver", type="StarsEvent")]

public class Stars extends HBox
{
    public static const STAR_CLICK :String = "starClick";
    public static const STAR_OVER :String = "starOver";

    // There's no Flex CSS property for Image source, embed these here
    [Embed(source="../../../../../../pages/images/ui/stars/full/average_lhalf.png")]
    public static const AVERAGE_LEFT :Class;
    [Embed(source="../../../../../../pages/images/ui/stars/full/average_rhalf.png")]
    public static const AVERAGE_RIGHT :Class;

    [Embed(source="../../../../../../pages/images/ui/stars/full/user_lhalf.png")]
    public static const USER_LEFT :Class;
    [Embed(source="../../../../../../pages/images/ui/stars/full/user_rhalf.png")]
    public static const USER_RIGHT :Class;

    public function Stars (rating :Number, left :Class, right :Class)
    {
        _left = left;
        _right = right;

        for (var i :int = 0; i < 10; ++i) {
            _images[i] = new Image();
            Command.bind(_images[i], MouseEvent.CLICK, handleClick, i);
            Command.bind(_images[i], MouseEvent.MOUSE_OVER, handleMouseOver, i);
            addChild(_images[i]);
        }

        setRating(rating);
    }

    public function setRating (rating :Number) :void
    {
        var filledStars :int = rating * 2;
        for (var i :int = 0; i < filledStars; ++i) {
            _images[i].source = ((i%2) == 0) ? _left : _right;
            _images[i].toolTip = Msgs.GENERAL.get("i.star" + int(i/2));
        }
        for (var k :int = filledStars; k < 10; ++k) {
            _images[k].source = ((k%2) == 0) ? EMPTY_LEFT : EMPTY_RIGHT;
        }
    }

    protected function handleClick (index :int) :void
    {
        dispatchEvent(new StarsEvent(STAR_CLICK, int(index/2+1)));
    }

    protected function handleMouseOver (index :int) :void
    {
        dispatchEvent(new StarsEvent(STAR_OVER, int(index/2+1)));
    }

    protected function getRatingFor (event :MouseEvent) :Number
    {
        return 3;
    }

    protected var _images :Array = new Array(10);
    protected var _left :Class;
    protected var _right :Class;

    [Embed(source="../../../../../../pages/images/ui/stars/full/empty_lhalf.png")]
    public static const EMPTY_LEFT :Class;
    [Embed(source="../../../../../../pages/images/ui/stars/full/empty_rhalf.png")]
    public static const EMPTY_RIGHT :Class;
}

}
