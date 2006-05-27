package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import flash.geom.Point;

import mx.effects.Move;
import mx.effects.easing.Linear;

import mx.events.EffectEvent;

import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.world.data.MsoyLocation;

public class Avatar extends ScreenMedia
{
    public function Avatar (desc :MediaData)
    {
        super(desc);
        sendMessage("setAction", "standing");
        setFacing(true);
    }

    public function moveTo (loc :MsoyLocation) :void
    {
        var xx :Number = x;
        var yy :Number = y;

        // if there's already a move, kill it
        if (_move != null) {
            _move.end();
        }

        // set walking, and maybe change facing direction
        sendMessage("setAction", "walking");
        if (this.x < loc.x) {
            setFacing(false);
        } else if (this.x > loc.x)  {
            setFacing(true);
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

    protected function setFacing (left :Boolean) :void
    {
        if (_left != left) {
            sendMessage("setFacing", left ? "left" : "right");
            _left = left;
        }
    }

    protected function moveStopped (event :EffectEvent) :void
    {
        sendMessage("setAction", "standing");
        _move = null;
    }

    protected override function mouseClick (event :MouseEvent) :void
    {
        setFacing(!_left);
    }

    protected var _left :Boolean;

    protected var _move :Move;
}
}
