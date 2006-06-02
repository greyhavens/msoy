package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import flash.geom.Matrix;
import flash.geom.Point;

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

    public function moveTo (destLoc :MsoyLocation) :void
    {
        // if there's already a move, kill it
        if (_move != null) {
            _move.end();
        }

        // set walking, and maybe change facing direction
        sendMessage("setAction", "walking");
        if (destLoc.x > loc.x) {
            setOrientation(0);
        } else if (destLoc.x < loc.x)  {
            setOrientation(180);
        }

        _move = new SceneMove(this);
        _move.src = this.loc;
        _move.dest = destLoc;
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
    }

    override public function moveCompleted (orient :Number) :void
    {
        super.moveCompleted(orient);
        setOrientation(int(orient));
    }

    protected override function mouseClick (event :MouseEvent) :void
    {
        //setFacing(!_left);
    }

    protected var _move :SceneMove;
}
}
