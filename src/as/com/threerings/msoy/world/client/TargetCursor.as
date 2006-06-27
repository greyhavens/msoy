package com.threerings.msoy.world.client {

import flash.display.Graphics;
import flash.display.Sprite;

public class TargetCursor extends Sprite
{
    public function TargetCursor ()
    {
        graphics.clear();
        graphics.lineStyle(3, 0xFFFFFF);
        graphics.moveTo(5, 0);
        graphics.lineTo(5, 11);
        graphics.moveTo(0, 5);
        graphics.lineTo(11, 5);

        graphics.lineStyle(1, 0x000000);
        graphics.moveTo(5, 1);
        graphics.lineTo(5, 10);
        graphics.moveTo(1, 5);
        graphics.lineTo(10, 5);
    }
}
}
