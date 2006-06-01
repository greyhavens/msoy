package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import flash.geom.Matrix;
import flash.geom.Point;

import mx.effects.Move;
import mx.effects.effectClasses.MoveInstance;
import mx.effects.easing.Linear;

import mx.events.EffectEvent;

import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.world.data.MsoyLocation;

public class Avatar extends ScreenMedia
{
    public function Avatar (desc :MediaData, loc :MsoyLocation)
    {
        super(desc);
        sendMessage("setAction", "standing");
        setOrientation(loc.orient);

//        var matrix :Matrix = this.transform.matrix; // create a copy
//        matrix.b = .5;
//        //matrix.c = .5;
//        this.transform.matrix = matrix;
    }

    public function moveTo (loc :MsoyLocation) :void
    {
        _loc = loc;
        var xx :Number = x;
        var yy :Number = y;

        // if there's already a move, kill it
        if (_move != null) {
            _move.end();
        }

        // set walking, and maybe change facing direction
        sendMessage("setAction", "walking");
        if (xx < loc.x) {
            setOrientation(0);
        } else if (xx > loc.x)  {
            setOrientation(180);
        }

        _move = new Move(this);
        _move.xFrom = xx;
        _move.yFrom = yy;
        _move.xTo = loc.x;
        _move.yTo = loc.y;
        _move.easingFunction = Linear.easeNone;
        var dist :Number = Point.distance(
            new Point(loc.x, loc.y), new Point(xx, yy));
        _move.duration = dist * 10; // the magic
        _move.addEventListener(EffectEvent.EFFECT_END, moveStopped);

        _move.play();
    }

    protected function setOrientation (orient :int) :void
    {
        var left :Boolean = (orient >= 90 && orient < 270)
        sendMessage("setFacing", left ? "left" : "right");
    }

    protected function moveStopped (event :EffectEvent) :void
    {
        _move = null;
        sendMessage("setAction", "standing");
        setOrientation(_loc.orient);
    }

    protected override function mouseClick (event :MouseEvent) :void
    {
        //setFacing(!_left);
    }

    /** The location to which we're currently heading. */
    protected var _loc :MsoyLocation;

    protected var _move :Move;
}
}
