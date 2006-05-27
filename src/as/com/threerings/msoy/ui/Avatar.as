package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import mx.effects.Move;

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
        // set walking, and maybe change facing direction
        sendMessage("setAction", "walking");
        if (this.x < loc.x) {
            setFacing(false);
        } else if (this.x > loc.x)  {
            setFacing(true);
        }

        var move :Move = new Move(this);
        move.xTo = loc.x;
        move.yTo = loc.y;
        move.duration = 850;
        move.addEventListener(EffectEvent.EFFECT_END, moveStopped);

        move.play();
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
    }

    protected override function mouseClick (event :MouseEvent) :void
    {
        setFacing(!_left);
    }

    protected var _left :Boolean;
}
}
